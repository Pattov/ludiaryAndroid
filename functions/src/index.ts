import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

// 🌍 REGIÓN ÚNICA (debe coincidir con Android)
setGlobalOptions({ region: "europe-west1" });

/* ---------------------------------------------------
   Helpers
--------------------------------------------------- */
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

// Importante: usa los mismos nombres que tu enum FriendStatus.name()
const FRIEND_STATUS = {
  INCOMING: "INCOMING",
  OUTGOING: "OUTGOING",
  ACCEPTED: "ACCEPTED",
} as const;

const INVITE_STATUS = {
  PENDING: "PENDING",
  ACCEPTED: "ACCEPTED",
  CANCELLED: "CANCELLED",
  REJECTED: "REJECTED",
} as const;

/* ===================================================
   FRIENDS
=================================================== */

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

    const friendUid = (idxSnap.get("uid") as string | undefined) ?? "";
    if (!friendUid) throw new HttpsError("internal", "Invalid friend_code_index entry");
    if (friendUid === myUid) throw new HttpsError("failed-precondition", "Cannot invite yourself");

    const myUserRef = db.collection("users").doc(myUid);
    const friendUserRef = db.collection("users").doc(friendUid);

    const [myUserSnap, friendUserSnap] = await Promise.all([
      tx.get(myUserRef),
      tx.get(friendUserRef),
    ]);

    if (!friendUserSnap.exists) throw new HttpsError("not-found", "User not found");

    const myDisplayName = (myUserSnap.get("displayName") as string | undefined) ?? null;
    const friendDisplayName = (friendUserSnap.get("displayName") as string | undefined) ?? null;

    const myFriendDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
    const theirFriendDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

    const [myRelSnap, theirRelSnap] = await Promise.all([tx.get(myFriendDoc), tx.get(theirFriendDoc)]);

    const myStatus = (myRelSnap.get("status") as string | undefined) ?? "";
    const theirStatus = (theirRelSnap.get("status") as string | undefined) ?? "";

    // Idempotencia: si ya está aceptado, devolvemos info
    if (myStatus === FRIEND_STATUS.ACCEPTED && theirStatus === FRIEND_STATUS.ACCEPTED) {
      return { friendUid, friendCode: code, displayName: friendDisplayName };
    }

    // En el doc del otro guardamos mi friendCode si existe
    const myCode = (myUserSnap.get("friendCode") as string | undefined) ?? null;

    tx.set(
      myFriendDoc,
      {
        friendUid,
        friendCode: code,
        displayName: friendDisplayName,
        status: FRIEND_STATUS.OUTGOING,
        createdAt: myRelSnap.exists ? (myRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: nowMillis(),
      },
      { merge: true }
    );

    tx.set(
      theirFriendDoc,
      {
        friendUid: myUid,
        friendCode: myCode,
        displayName: myDisplayName,
        status: FRIEND_STATUS.INCOMING,
        createdAt: theirRelSnap.exists ? (theirRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: nowMillis(),
      },
      { merge: true }
    );

    return { friendUid, friendCode: code, displayName: friendDisplayName };
  });

  return result;
});

// friendsAccept
export const friendsAccept = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid, "friendUid");
  const now = nowMillis();

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
  const theirDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

  await db.runTransaction(async (tx) => {
    const [mySnap, theirSnap] = await Promise.all([tx.get(myDoc), tx.get(theirDoc)]);

    if (!mySnap.exists) throw new HttpsError("not-found", "Friend relation not found");

    const myStatus = (mySnap.get("status") as string | undefined) ?? "";
    if (myStatus === FRIEND_STATUS.ACCEPTED) return;

    if (myStatus !== FRIEND_STATUS.INCOMING && myStatus !== FRIEND_STATUS.OUTGOING) {
      throw new HttpsError("failed-precondition", "Invalid status for accept");
    }

    tx.set(
      myDoc,
      { status: FRIEND_STATUS.ACCEPTED, updatedAt: now, createdAt: mySnap.get("createdAt") ?? now },
      { merge: true }
    );

    tx.set(
      theirDoc,
      { status: FRIEND_STATUS.ACCEPTED, updatedAt: now, createdAt: theirSnap.get("createdAt") ?? now },
      { merge: true }
    );
  });

  return { ok: true };
});

// friendsReject (en tu app: reject = borrar solicitud)
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

  const nickname =
    nicknameRaw === null || nicknameRaw === undefined ? null : String(nicknameRaw).trim();

  const myDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);

  await myDoc.set(
    {
      nickname: nickname && nickname.length > 0 ? nickname : null,
      updatedAt: nowMillis(),
    },
    { merge: true }
  );

  return { ok: true };
});

/* ===================================================
   GROUPS
=================================================== */

// groupsCreate
export const groupsCreate = onCall(async (request) => {
  const myUid = requireAuth(request);
  const name = asNonEmptyString(request.data?.name, "name");
  const now = nowMillis();

  const groupRef = db.collection("groups").doc(); // id auto
  const memberRef = groupRef.collection("members").doc(myUid);
  const idxRef = db.collection("users").doc(myUid).collection("groups").doc(groupRef.id);

  const batch = db.batch();
  batch.set(groupRef, {
    name,
    createdAt: now,
    updatedAt: now,
    membersCount: 1,
  });
  batch.set(memberRef, { uid: myUid, joinedAt: now });
  batch.set(idxRef, {
    groupId: groupRef.id,
    nameSnapshot: name,
    joinedAt: now,
    updatedAt: now,
  });
  await batch.commit();

  return { groupId: groupRef.id, name, now, membersCount: 1 };
});

// groupsInvite
export const groupsInvite = onCall(async (request) => {
  const fromUid = requireAuth(request);
  const groupId = asNonEmptyString(request.data?.groupId, "groupId");
  const toUid = asNonEmptyString(request.data?.toUid, "toUid");
  const groupNameSnapshot = String(request.data?.groupNameSnapshot ?? "Grupo");
  const clientCreatedAt = Number(request.data?.clientCreatedAt ?? nowMillis());

  if (toUid === fromUid) throw new HttpsError("failed-precondition", "Cannot invite yourself");

  const groupRef = db.collection("groups").doc(groupId);
  const memberRef = groupRef.collection("members").doc(fromUid);

  const inviteId = `${groupId}_${toUid}`;
  const inviteRef = db.collection("group_invites").doc(inviteId);

  await db.runTransaction(async (tx) => {
    const [groupSnap, memberSnap, inviteSnap] = await Promise.all([
      tx.get(groupRef),
      tx.get(memberRef),
      tx.get(inviteRef),
    ]);

    if (!groupSnap.exists) throw new HttpsError("not-found", "Group not found");
    if (!memberSnap.exists) throw new HttpsError("permission-denied", "Not a member of the group");

    // Idempotente: si ya existe PENDING, no tocamos nada
    if (inviteSnap.exists) {
      const st = (inviteSnap.get("status") as string | undefined) ?? "";
      if (st === INVITE_STATUS.PENDING) return;
    }

    tx.set(
      inviteRef,
      {
        groupId,
        groupNameSnapshot,
        fromUid,
        toUid,
        status: INVITE_STATUS.PENDING,
        createdAt: clientCreatedAt,
        respondedAt: null,
      },
      { merge: true }
    );
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

// groupsAcceptInvite
export const groupsAcceptInvite = onCall(async (request) => {
  const myUid = requireAuth(request);
  const inviteId = asNonEmptyString(request.data?.inviteId, "inviteId");
  const now = nowMillis();

  const inviteRef = db.collection("group_invites").doc(inviteId);

  await db.runTransaction(async (tx) => {
    const inviteSnap = await tx.get(inviteRef);
    if (!inviteSnap.exists) throw new HttpsError("not-found", "Invite not found");

    const toUid = (inviteSnap.get("toUid") as string | undefined) ?? "";
    if (toUid !== myUid) throw new HttpsError("permission-denied", "Not authorized");

    const status = (inviteSnap.get("status") as string | undefined) ?? "";
    if (status !== INVITE_STATUS.PENDING) {
      if (status === INVITE_STATUS.ACCEPTED) return; // idempotente
      throw new HttpsError("failed-precondition", "Invite is not pending");
    }

    const groupId = (inviteSnap.get("groupId") as string | undefined) ?? "";
    const groupNameSnapshot =
      (inviteSnap.get("groupNameSnapshot") as string | undefined) ?? "Grupo";

    const groupRef = db.collection("groups").doc(groupId);
    const memberRef = groupRef.collection("members").doc(myUid);
    const idxRef = db.collection("users").doc(myUid).collection("groups").doc(groupId);

    const groupSnap = await tx.get(groupRef);
    if (!groupSnap.exists) throw new HttpsError("not-found", "Group not found");

    tx.update(inviteRef, { status: INVITE_STATUS.ACCEPTED, respondedAt: now });

    tx.set(memberRef, { uid: myUid, joinedAt: now }, { merge: true });

    tx.set(
      idxRef,
      { groupId, nameSnapshot: groupNameSnapshot, joinedAt: now, updatedAt: now },
      { merge: true }
    );

    tx.set(
      groupRef,
      { updatedAt: now, membersCount: admin.firestore.FieldValue.increment(1) },
      { merge: true }
    );
  });

  return { ok: true };
});

// groupsCancelInvite
export const groupsCancelInvite = onCall(async (request) => {
  const myUid = requireAuth(request);
  const inviteId = asNonEmptyString(request.data?.inviteId, "inviteId");
  const now = nowMillis();

  const inviteRef = db.collection("group_invites").doc(inviteId);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(inviteRef);
    if (!snap.exists) return;

    const fromUid = (snap.get("fromUid") as string | undefined) ?? "";
    const toUid = (snap.get("toUid") as string | undefined) ?? "";

    if (myUid !== fromUid && myUid !== toUid) {
      throw new HttpsError("permission-denied", "Not authorized");
    }

    tx.set(inviteRef, { status: INVITE_STATUS.CANCELLED, respondedAt: now }, { merge: true });
  });

  return { ok: true };
});

// groupsRejectInvite (en tu app: reject = delete)
export const groupsRejectInvite = onCall(async (request) => {
  const myUid = requireAuth(request);
  const inviteId = asNonEmptyString(request.data?.inviteId, "inviteId");

  const inviteRef = db.collection("group_invites").doc(inviteId);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(inviteRef);
    if (!snap.exists) return;

    const toUid = (snap.get("toUid") as string | undefined) ?? "";
    if (toUid !== myUid) throw new HttpsError("permission-denied", "Not authorized");

    tx.delete(inviteRef);
  });

  return { ok: true };
});

// groupsLeave
export const groupsLeave = onCall(async (request) => {
  const myUid = requireAuth(request);
  const groupId = asNonEmptyString(request.data?.groupId, "groupId");
  const now = nowMillis();

  const groupRef = db.collection("groups").doc(groupId);
  const memberRef = groupRef.collection("members").doc(myUid);
  const idxRef = db.collection("users").doc(myUid).collection("groups").doc(groupId);

  await db.runTransaction(async (tx) => {
    const groupSnap = await tx.get(groupRef);
    if (!groupSnap.exists) return;

    tx.delete(memberRef);
    tx.delete(idxRef);

    tx.set(
      groupRef,
      { updatedAt: now, membersCount: admin.firestore.FieldValue.increment(-1) },
      { merge: true }
    );
  });

  // Si se queda vacío, borramos grupo
  const membersSnap = await groupRef.collection("members").limit(1).get();
  if (membersSnap.empty) {
    await groupRef.delete();
  }

  return { ok: true };
});

/* ===================================================
   NOTIFICATIONS
=================================================== */

// notificationsMarkAsRead
export const notificationsMarkAsRead = onCall(async (request) => {
  const uid = requireAuth(request);
  const notifId = asNonEmptyString(request.data?.notifId, "notifId");
  const now = nowMillis();

  const notifRef = db.doc(`users/${uid}/notifications/${notifId}`);
  const statsRef = db.doc(`users/${uid}/notificationStats/stats`);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(notifRef);
    if (!snap.exists) return;

    const isRead = snap.get("isRead") === true;
    if (isRead) return;

    tx.set(
      notifRef,
      { isRead: true, readAt: now }, // millis para que Android getLong funcione
      { merge: true }
    );

    const statsSnap = await tx.get(statsRef);
    const current = (statsSnap.get("unreadCount") as number | undefined) ?? 0;

    tx.set(statsRef, { unreadCount: Math.max(0, current - 1) }, { merge: true });
  });

  return { ok: true };
});