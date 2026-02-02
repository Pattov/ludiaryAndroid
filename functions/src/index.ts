import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ region: "europe-west1" });

function requireAuth(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Not authenticated");
  return uid;
}

function nowMillis(): number {
  return Date.now();
}

function asNonEmptyString(v: unknown, field = "param"): string {
  const s = String(v ?? "").trim();
  if (!s) throw new HttpsError("invalid-argument", `${field} required`);
  return s;
}

function normalizeCode(codeRaw: unknown): string {
  return String(codeRaw ?? "").trim().toUpperCase();
}

const FRIEND_STATUS = {
  INCOMING: "PENDING_INCOMING",
  OUTGOING: "PENDING_OUTGOING",
  ACCEPTED: "ACCEPTED",
} as const;

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
    const myCode = (myUserSnap.get("friendCode") as string | undefined) ?? null;

    const myFriendDoc = db.collection("users").doc(myUid).collection("friends").doc(friendUid);
    const theirFriendDoc = db.collection("users").doc(friendUid).collection("friends").doc(myUid);

    const [myRelSnap, theirRelSnap] = await Promise.all([
      tx.get(myFriendDoc),
      tx.get(theirFriendDoc),
    ]);

    const myStatus = (myRelSnap.get("status") as string | undefined) ?? "";
    const theirStatus = (theirRelSnap.get("status") as string | undefined) ?? "";

    // Si ya está aceptado en ambos lados, devolvemos info (idempotente)
    if (myStatus === FRIEND_STATUS.ACCEPTED && theirStatus === FRIEND_STATUS.ACCEPTED) {
      return { friendUid, friendCode: code, displayName: friendDisplayName };
    }

    const now = nowMillis();

    // Creamos/actualizamos invitación (OUTGOING/INCOMING)
    tx.set(
      myFriendDoc,
      {
        friendUid,
        friendCode: code,
        displayName: friendDisplayName,
        status: FRIEND_STATUS.OUTGOING,
        createdAt: myRelSnap.exists ? (myRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: now,
      },
      { merge: true }
    );

    tx.set(
      theirFriendDoc,
      {
        friendUid: myUid,
        friendCode: myCode,          // puede ser null
        displayName: myDisplayName,  // puede ser null
        status: FRIEND_STATUS.INCOMING,
        createdAt: theirRelSnap.exists ? (theirRelSnap.get("createdAt") ?? clientCreatedAt) : clientCreatedAt,
        updatedAt: now,
      },
      { merge: true }
    );

    return { friendUid, friendCode: code, displayName: friendDisplayName };
  });

  return result;
});

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

    // Estricto: solo acepto si me llegó (INCOMING)
    if (myStatus !== FRIEND_STATUS.INCOMING) {
      if (myStatus === FRIEND_STATUS.ACCEPTED) return; // idempotente
      throw new HttpsError("failed-precondition", "Invite is not incoming");
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

export const friendsRemove = onCall(async (request) => {
  const myUid = requireAuth(request);
  const friendUid = asNonEmptyString(request.data?.friendUid);

  const batch = db.batch();

  batch.delete(db.doc(`users/${myUid}/friends/${friendUid}`));
  batch.delete(db.doc(`users/${friendUid}/friends/${myUid}`));

  await batch.commit();

  return { ok: true };
});
