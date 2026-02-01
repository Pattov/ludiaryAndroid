import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";

admin.initializeApp();
const db = admin.firestore();

setGlobalOptions({ region: "europe-west1" });

/* ---------------------------------------------------
   Helpers
--------------------------------------------------- */
function requireAuth(request: CallableRequest): string {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Not authenticated");
  return uid;
}

/* ===================================================
   FRIENDS
=================================================== */

// friendsSendInviteByCode
export const friendsSendInviteByCode = onCall(async (request) => {
  requireAuth(request);

  const code = String(request.data?.code ?? "").trim().toUpperCase();
  if (!code) throw new HttpsError("invalid-argument", "code required");

  // TODO: buscar usuario por código
  return {
    friendUid: null,
    friendCode: code,
    displayName: null,
  };
});

// friendsAccept
export const friendsAccept = onCall(async (request) => {
  requireAuth(request);

  const friendUid = String(request.data?.friendUid ?? "");
  if (!friendUid) throw new HttpsError("invalid-argument", "friendUid required");

  // TODO: marcar amistad como aceptada
  return { ok: true };
});

// friendsReject
export const friendsReject = onCall(async (request) => {
  requireAuth(request);

  const friendUid = String(request.data?.friendUid ?? "");
  if (!friendUid) throw new HttpsError("invalid-argument", "friendUid required");

  // TODO: rechazar invitación
  return { ok: true };
});

// friendsRemove
export const friendsRemove = onCall(async (request) => {
  requireAuth(request);

  const friendUid = String(request.data?.friendUid ?? "");
  if (!friendUid) throw new HttpsError("invalid-argument", "friendUid required");

  // TODO: eliminar relación de amistad
  return { ok: true };
});

// friendsUpdateNickname
export const friendsUpdateNickname = onCall(async (request) => {
  requireAuth(request);

  const friendUid = String(request.data?.friendUid ?? "");
  if (!friendUid) throw new HttpsError("invalid-argument", "friendUid required");

  // Lo dejamos preparado (sin TS errors) hasta implementar la lógica real:
  const nickname = request.data?.nickname ?? null;
  void nickname;

  // TODO: actualizar nickname
  return { ok: true };
});

/* ===================================================
   GROUPS
=================================================== */

// groupsCreate
export const groupsCreate = onCall(async (request) => {
  const uid = requireAuth(request);

  const name = String(request.data?.name ?? "").trim();
  if (!name) throw new HttpsError("invalid-argument", "name required");

  const groupRef = db.collection("groups").doc();
  const now = Date.now();

  await groupRef.set({
    name,
    ownerUid: uid,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return {
    groupId: groupRef.id,
    name,
    now,
    membersCount: 1,
  };
});

// groupsInvite
export const groupsInvite = onCall(async (request) => {
  const uid = requireAuth(request);

  const { groupId, groupNameSnapshot, toUid } = request.data ?? {};

  if (!groupId || !toUid) {
    throw new HttpsError("invalid-argument", "groupId and toUid required");
  }

  const inviteId = `${groupId}_${toUid}`;

  // TODO: crear invitación
  return {
    inviteId,
    groupId,
    groupNameSnapshot,
    fromUid: uid,
    toUid,
    status: "PENDING",
    createdAt: Date.now(),
    respondedAt: null,
  };
});

// groupsAcceptInvite
export const groupsAcceptInvite = onCall(async (request) => {
  requireAuth(request);

  const inviteId = String(request.data?.inviteId ?? "");
  if (!inviteId) throw new HttpsError("invalid-argument", "inviteId required");

  // TODO: aceptar invitación
  return { ok: true };
});

// groupsCancelInvite
export const groupsCancelInvite = onCall(async (request) => {
  requireAuth(request);

  const inviteId = String(request.data?.inviteId ?? "");
  if (!inviteId) throw new HttpsError("invalid-argument", "inviteId required");

  // TODO: cancelar invitación
  return { ok: true };
});

// groupsRejectInvite
export const groupsRejectInvite = onCall(async (request) => {
  requireAuth(request);

  const inviteId = String(request.data?.inviteId ?? "");
  if (!inviteId) throw new HttpsError("invalid-argument", "inviteId required");

  // TODO: rechazar invitación
  return { ok: true };
});

// groupsLeave
export const groupsLeave = onCall(async (request) => {
  requireAuth(request);

  const groupId = String(request.data?.groupId ?? "");
  if (!groupId) throw new HttpsError("invalid-argument", "groupId required");

  // TODO: salir del grupo
  return { ok: true };
});

/* ===================================================
   NOTIFICATIONS
=================================================== */

// notificationsMarkAsRead
export const notificationsMarkAsRead = onCall(async (request) => {
  const uid = requireAuth(request);

  const notifId = String(request.data?.notifId ?? "");
  if (!notifId) throw new HttpsError("invalid-argument", "notifId required");

  const notifRef = db.doc(`users/${uid}/notifications/${notifId}`);
  const statsRef = db.doc(`users/${uid}/notificationStats/stats`);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(notifRef);
    if (!snap.exists) return;

    const isRead = snap.get("isRead") === true;
    if (isRead) return;

    tx.update(notifRef, {
      isRead: true,
      readAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    const statsSnap = await tx.get(statsRef);
    const current = (statsSnap.get("unreadCount") as number) ?? 0;
    tx.set(statsRef, { unreadCount: Math.max(0, current - 1) }, { merge: true });
  });

  return { ok: true };
});
