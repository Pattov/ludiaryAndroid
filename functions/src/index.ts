import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ region: "europe-west1" });

/* ---------------- Helpers ---------------- */

function requireAuth(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Not authenticated");
  return uid;
}

function nowMillis(): number {
  return Date.now();
}

function asNonEmptyString(v: unknown, name = "param"): string {
  const s = String(v ?? "").trim();
  if (!s) throw new HttpsError("invalid-argument", `${name} required`);
  return s;
}

function normalizeCode(codeRaw: unknown): string {
  return String(codeRaw ?? "").trim().toUpperCase();
}

const FRIEND_STATUS = {
  PENDING_INCOMING: "PENDING_INCOMING",
  PENDING_OUTGOING: "PENDING_OUTGOING",
  ACCEPTED: "ACCEPTED",
} as const;

const INVITE_STATUS = {
  PENDING: "PENDING",
  ACCEPTED: "ACCEPTED",
  CANCELLED: "CANCELLED",
  REJECTED: "REJECTED",
} as const;

/* ---------------- Push + Language ---------------- */

/**
 * Devuelve el idioma del usuario destino.
 * Guarda language en users/{uid}.preferences.language (ej: "es", "en")
 */
async function getUserLanguage(uid: string): Promise<string> {
  const snap = await db.collection("users").doc(uid).get();
  const pref = (snap.get("preferences") as any) ?? {};
  const lang = (pref.language as string | undefined) ?? snap.get("preferences.language");
  return (lang || "es").toLowerCase();
}

/**
 * Consentimiento: users/{uid}.preferences.pushEnabled = true
 * (si no existe, yo recomiendo tratarlo como false hasta que lo acepten)
 */
async function isPushEnabled(uid: string): Promise<boolean> {
  const snap = await db.collection("users").doc(uid).get();
  const pref = (snap.get("preferences") as any) ?? {};
  const enabled = pref.pushEnabled ?? snap.get("preferences.pushEnabled");
  return enabled === true;
}

/**
 * Obtiene tokens del usuario en: users/{uid}/fcmTokens
 */
async function getUserFcmTokens(uid: string): Promise<string[]> {
  const qs = await db.collection("users").doc(uid).collection("fcmTokens").get();
  const tokens: string[] = [];
  qs.forEach((d) => {
    const t = d.get("token");
    if (typeof t === "string" && t.length > 0) tokens.push(t);
  });
  return tokens;
}

/**
 * Mensajes localizados.
 * Si quieres, puedes meter aquí más idiomas o más tipos.
 */
function buildLocalizedMessage(
  lang: string,
  payload: { type: string; fromName?: string; groupName?: string }
): { title: string; body: string } {
  const l = lang.startsWith("en") ? "en" : "es";

  if (payload.type === "friend_invite") {
    if (l === "en") {
      return { title: "Friend request", body: `${payload.fromName ?? "Someone"} sent you a friend request.` };
    }
    return { title: "Solicitud de amistad", body: `${payload.fromName ?? "Alguien"} te ha enviado una solicitud.` };
  }

  if (payload.type === "group_invite") {
    if (l === "en") {
      return { title: "Group invitation", body: `You were invited to: ${payload.groupName ?? "a group"}` };
    }
    return { title: "Invitación a grupo", body: `Te han invitado al grupo: ${payload.groupName ?? "un grupo"}` };
  }

  // default
  if (l === "en") return { title: "Notification", body: "You have a new notification." };
  return { title: "Notificación", body: "Tienes una nueva notificación." };
}

/**
 * Envía push al usuario si:
 * - pushEnabled == true
 * - hay tokens FCM
 *
 * Además limpia tokens inválidos automáticamente.
 */
async function sendPushToUid(args: {
  toUid: string;
  type: string;
  data: Record<string, string>;
  fromName?: string;
  groupName?: string;
}) {
  const { toUid } = args;

  const enabled = await isPushEnabled(toUid);
  if (!enabled) return; // ✅ consentimiento

  const tokens = await getUserFcmTokens(toUid);
  if (tokens.length === 0) return;

  const lang = await getUserLanguage(toUid);
  const { title, body } = buildLocalizedMessage(lang, {
    type: args.type,
    fromName: args.fromName,
    groupName: args.groupName,
  });

  // OJO:
  // - "notification" hace que Android lo muestre en bandeja (si permiso concedido + canal).
  // - "data" te sirve para navegar a pantalla concreta al tocar.
  const multicast = {
    tokens,
    notification: { title, body },
    data: {
      ...args.data,
      type: args.type,
    },
    android: {
      // Debe coincidir con tu canal en Android (NotificationChannel)
      notification: {
        channelId: "social",
      },
    },
  };

  const res = await admin.messaging().sendEachForMulticast(multicast);

  // Limpieza de tokens inválidos
  const toDelete: Promise<FirebaseFirestore.WriteResult>[] = [];
  res.responses.forEach((r, i) => {
    if (!r.success) {
      const code = (r.error as any)?.code || "";
      const isInvalid =
        code.includes("registration-token-not-registered") ||
        code.includes("invalid-registration-token");
      if (isInvalid) {
        const badToken = tokens[i];
        // buscamos docs que tengan ese token y borramos
        // (si tu docId es el token, puedes borrar directo)
        toDelete.push(
          db
            .collection("users")
            .doc(toUid)
            .collection("fcmTokens")
            .doc(badToken)
            .delete()
        );
      }
    }
  });

  if (toDelete.length) await Promise.allSettled(toDelete);
}

/* ================= FRIENDS ================= */

// friendsSendInviteByCode
export const friendsSendInviteByCode = onCall(async (request) => {
  const myUid = requireAuth(request);
  const code = normalizeCode(request.data?.code);
  const clientCreatedAt = Number(request.data?.clientCreatedAt ?? nowMillis());
  if (!code) throw new HttpsError("invalid-argument", "code required");

  const idxRef = db.collection("friend_code_index").doc(code);

  const result = await db.runTransaction(async (tx) => {
    const idxSnap = await tx.get(idxRef);
    if (!idxSnap.exists) throw new HttpsError("not-found", "Friend code not found");

    const friendUid = idxSnap.get("uid") as string | undefined;
    if (!friendUid) throw new HttpsError("internal", "Invalid friend_code_index entry");
    if (friendUid === myUid) throw new HttpsError("failed-precondition", "Cannot invite yourself");

    const myUserRef = db.collection("users").doc(myUid);
    const friendUserRef = db.collection("users").doc(friendUid);

    const [myUserSnap, friendUserSnap] = await Promise.all([tx.get(myUserRef), tx.get(friendUserRef)]);
    if (!friendUserSnap.exists) throw new HttpsError("not-found", "User not found");

    const myDisplayName = (myUserSnap.get("displayName") as string | undefined) ?? null;
    const friendDisplayName = (friendUserSnap.get("displayName") as string | undefined) ?? null;

    const myFriendDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
    const theirFriendDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

    const [myRelSnap, theirRelSnap] = await Promise.all([tx.get(myFriendDoc), tx.get(theirFriendDoc)]);

    const myStatus = myRelSnap.get("status") as string | undefined;
    const theirStatus = theirRelSnap.get("status") as string | undefined;

    // Idempotencia: si ya aceptados, no tocamos nada
    if (myStatus === FRIEND_STATUS.ACCEPTED && theirStatus === FRIEND_STATUS.ACCEPTED) {
      return { friendUid, friendCode: code, displayName: friendDisplayName, myDisplayName };
    }

    // yo: OUTGOING
    tx.set(
      myFriendDoc,
      {
        friendUid,
        friendCode: code,
        displayName: friendDisplayName,
        status: FRIEND_STATUS.PENDING_OUTGOING,
        createdAt: myRelSnap.exists ? (myRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: nowMillis(),
      },
      { merge: true }
    );

    // él/ella: INCOMING
    const myCode = (myUserSnap.get("friendCode") as string | undefined) ?? null;

    tx.set(
      theirFriendDoc,
      {
        friendUid: myUid,
        friendCode: myCode,
        displayName: myDisplayName,
        status: FRIEND_STATUS.PENDING_INCOMING,
        createdAt: theirRelSnap.exists ? (theirRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: nowMillis(),
      },
      { merge: true }
    );

    return { friendUid, friendCode: code, displayName: friendDisplayName, myDisplayName };
  });

  // ✅ PUSH al receptor (si lo tiene permitido)
  await sendPushToUid({
    toUid: result.friendUid!,
    type: "friend_invite",
    fromName: (result as any).myDisplayName ?? "Alguien",
    data: {
      fromUid: myUid,
    },
  });

  // respuesta al cliente
  return {
    friendUid: result.friendUid,
    friendCode: result.friendCode,
    displayName: result.displayName,
  };
});

// friendsAccept
export const friendsAccept = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid, "friendUid");
  const now = nowMillis();

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
  const theirDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

  await db.runTransaction(async (tx) => {
    const [mySnap] = await Promise.all([
      tx.get(myDoc),
      tx.get(theirDoc)
    ]);
    if (!mySnap.exists) throw new HttpsError("not-found", "Friend relation not found");

    tx.set(myDoc, { status: FRIEND_STATUS.ACCEPTED, updatedAt: now }, { merge: true });
    tx.set(theirDoc, { status: FRIEND_STATUS.ACCEPTED, updatedAt: now }, { merge: true });
  });

  return { ok: true };
});

// friendsReject
export const friendsReject = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid, "friendUid");

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
  const theirDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

  await db.runTransaction(async (tx) => {
    tx.delete(myDoc);
    tx.delete(theirDoc);
  });

  return { ok: true };
});

// friendsRemove
export const friendsRemove = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid, "friendUid");

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
  const theirDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

  await db.runTransaction(async (tx) => {
    tx.delete(myDoc);
    tx.delete(theirDoc);
  });

  return { ok: true };
});

// friendsUpdateNickname
export const friendsUpdateNickname = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid, "friendUid");
  const nicknameRaw = request.data?.nickname;
  const nickname = nicknameRaw === null || nicknameRaw === undefined ? null : String(nicknameRaw).trim();
  const now = nowMillis();

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);

  await myDoc.set({ nickname: nickname && nickname.length > 0 ? nickname : null, updatedAt: now }, { merge: true });
  return { ok: true };
});

/* ================= GROUPS ================= */

// groupsInvite
export const groupsInvite = onCall(async (request) => {
  const fromUid = requireAuth(request);
  const groupId = asNonEmptyString(request.data?.groupId, "groupId");
  const groupNameSnapshot = String(request.data?.groupNameSnapshot ?? "Grupo");
  const toUid = asNonEmptyString(request.data?.toUid, "toUid");
  const clientCreatedAt = Number(request.data?.clientCreatedAt ?? nowMillis());

  if (toUid === fromUid) throw new HttpsError("failed-precondition", "Cannot invite yourself");

  const groupRef = db.collection("groups").doc(groupId);
  const memberRef = groupRef.collection("members").doc(fromUid);

  const inviteId = `${groupId}_${toUid}`;
  const inviteRef = db.collection("group_invites").doc(inviteId);

  await db.runTransaction(async (tx) => {
    const [groupSnap, memberSnap, inviteSnap] = await Promise.all([tx.get(groupRef), tx.get(memberRef), tx.get(inviteRef)]);

    if (!groupSnap.exists) throw new HttpsError("not-found", "Group not found");
    if (!memberSnap.exists) throw new HttpsError("permission-denied", "Not a member of the group");

    if (inviteSnap.exists) {
      const st = (inviteSnap.get("status") as string | undefined) ?? "";
      if (st === INVITE_STATUS.PENDING) return;
    }

    tx.set(inviteRef, {
      groupId,
      groupNameSnapshot,
      fromUid,
      toUid,
      status: INVITE_STATUS.PENDING,
      createdAt: clientCreatedAt,
      respondedAt: null,
    }, { merge: true });
  });

  // ✅ PUSH al receptor (si lo tiene permitido)
  await sendPushToUid({
    toUid,
    type: "group_invite",
    groupName: groupNameSnapshot,
    data: {
      groupId,
      inviteId,
      fromUid,
      toUid,
    },
  });

  return {
    inviteId,
    groupId,
    groupNameSnapshot,
    fromUid,
    toUid,
    status: INVITE_STATUS.PENDING,
    createdAt: clientCreatedAt,
    respondedAt: null,
  };
});
