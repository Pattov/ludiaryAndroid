package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendsRepositoryImpl(
    private val local: LocalFriendsDataSource,
    private val remote: FirestoreFriendsRepository,
    private val auth: FirebaseAuth
) : FriendsRepository {

    private var remoteSyncJob: Job? = null

    // ✅ Este era el que te faltaba (por eso el Unresolved reference)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lastUid: String? = null

    override fun observeFriends(query: String): Flow<List<FriendEntity>> = local.observeFriends(query)
    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> = local.observeIncomingRequests(query)
    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> = local.observeOutgoingRequests(query)
    override fun observeGroups(query: String): Flow<List<FriendEntity>> = local.observeGroups(query)

    /**
     * Listener realtime Firestore -> Room
     * Firebase siempre tiene razón: lo remoto pisa lo local en conflictos.
     */
    override fun startRemoteSync() {
        val me = auth.currentUser ?: return

        if (lastUid != null && lastUid != me.uid) {
            repoScope.launch { local.clearAll() }
        }
        lastUid = me.uid

        // ✅ Importante: evitar listeners duplicados al reentrar a la pantalla
        remoteSyncJob?.cancel()

        remoteSyncJob = repoScope.launch {
            Log.d("LUDIARY_FRIENDS_DEBUG", "startRemoteSync uid=${me.uid}")

            remote.observeAll(me.uid).collect { remoteList ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE size=${remoteList.size}")

                for (rf in remoteList) {
                    val status = runCatching { FriendStatus.valueOf(rf.status) }.getOrNull()
                    if (status == null) {
                        Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE skip invalid status=${rf.status}")
                        continue
                    }

                    // ✅ Nunca insertar a pelo: esto evita el UNIQUE constraint failed: friends.friendUid
                    local.upsertRemote(
                        friendUid = rf.friendUid,
                        friendCode = rf.friendCode,
                        displayName = rf.displayName,
                        nickname = rf.nickname,
                        status = status,
                        createdAt = rf.createdAt,
                        updatedAt = rf.updatedAt
                    )
                }
            }
        }
    }

    override suspend fun sendInviteByCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")

            val normalized = code.trim().uppercase()
            Log.d("LUDIARY_FRIENDS_DEBUG", "sendInviteByCode() code=$normalized from=${me.uid}")

            // Local-first: guardamos pendiente local
            local.insert(
                FriendEntity(
                    friendCode = normalized,
                    friendUid = null, // aún no resuelto
                    displayName = null,
                    nickname = null,
                    status = FriendStatus.PENDING_OUTGOING_LOCAL,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING
                )
            )

            Log.d("LUDIARY_FRIENDS_DEBUG", "Local saved PENDING_OUTGOING_LOCAL + PENDING")
            Unit
        }
    }

    override suspend fun flushOfflineInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: return@runCatching

            val pending = local.getPendingInvites()
            Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites() pending=${pending.size}")

            for (entity in pending) {
                val localId = entity.id
                val senderName = remote.getUserDisplayName(me.uid) ?: "Usuario"
                val now = System.currentTimeMillis()

                val code = entity.friendCode?.trim()?.uppercase().orEmpty()
                if (code.isBlank()) continue

                Log.d("LUDIARY_FRIENDS_DEBUG", "Resolving friendCode=$code localId=$localId")

                val myCode = remote.findFriendCodeByUid(me.uid)
                val targetCode = code // ya lo tienes normalizado

                val target = remote.findUserByFriendCode(code)
                if (target == null) {
                    Log.d("LUDIARY_FRIENDS_DEBUG", "resolve MISS -> keep local pending localId=$localId")
                    continue
                }

                // ✅ No permitir enviarse a uno mismo
                if (target.uid == me.uid) {
                    Log.d("LUDIARY_FRIENDS_DEBUG", "Self-invite blocked -> delete local id=$localId")
                    local.deleteById(localId)
                    continue
                }

                // ✅ Atamos primero friendUid en local para que cuando llegue Firestore no intente insertar otra fila
                local.updateStatusAndUid(
                    id = localId,
                    status = FriendStatus.PENDING_OUTGOING,
                    friendUid = target.uid,
                    syncStatus = SyncStatus.PENDING
                )

                // Firestore: escribimos en ambos lados
                remote.upsert(
                    uid = me.uid,
                    friendUid = target.uid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = target.uid,
                        friendCode = targetCode,
                        displayName = target.displayName,
                        nickname = null,
                        status = FriendStatus.PENDING_OUTGOING.name,
                        createdAt = entity.createdAt,
                        updatedAt = now
                    )
                )

                remote.upsert(
                    uid = target.uid,
                    friendUid = me.uid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = me.uid,
                        friendCode = myCode,
                        displayName = senderName,
                        nickname = null,
                        status = FriendStatus.PENDING_INCOMING.name,
                        createdAt = entity.createdAt,
                        updatedAt = now
                    )
                )

                // Local: ya está escrito remoto -> limpio
                local.updateSyncStatus(localId, SyncStatus.CLEAN)
                Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites OK localId=$localId -> remote written")
            }

        }.onFailure {
            Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites FAIL err=${it.message}")
        }
    }

    override suspend fun getMyFriendCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            remote.getMyFriendCode(me.uid) ?: error("No tienes friendCode")
        }
    }


    override suspend fun acceptRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: error("No existe en local")

            val friendUid = entity.friendUid ?: error("No tiene friendUid (no sincronizado aún)")
            Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest id=$friendId friendUid=$friendUid")

            val now = System.currentTimeMillis()

            val friendCodeOfOther = entity.friendCode ?: remote.findFriendCodeByUid(friendUid)
            val myCode = remote.findFriendCodeByUid(me.uid)

            // Firestore: ambos ACCEPTED
            remote.upsert(
                uid = me.uid,
                friendUid = friendUid,
                data = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = friendUid,
                    friendCode = friendCodeOfOther,
                    displayName = entity.displayName,
                    nickname = entity.nickname,
                    status = FriendStatus.ACCEPTED.name,
                    createdAt = entity.createdAt,
                    updatedAt = now
                )
            )

            remote.upsert(
                uid = friendUid,
                friendUid = me.uid,
                data = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = me.uid,
                    friendCode = myCode,
                    displayName = null,
                    nickname = null,
                    status = FriendStatus.ACCEPTED.name,
                    createdAt = entity.createdAt,
                    updatedAt = now
                )
            )

            // Local
            local.updateStatusAndUid(
                id = friendId,
                status = FriendStatus.ACCEPTED,
                friendUid = friendUid,
                syncStatus = SyncStatus.CLEAN
            )

            Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest OK -> local ACCEPTED")
            Unit
        }
    }

    override fun stopRemoteSync() {
        remoteSyncJob?.cancel()
        remoteSyncJob = null
    }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: return@runCatching

            val friendUid = entity.friendUid
            Log.d("LUDIARY_FRIENDS_DEBUG", "rejectRequest id=$friendId friendUid=$friendUid status=${entity.status}")

            // MVP: borrar en remoto si tenemos uid
            if (!friendUid.isNullOrBlank()) {
                remote.delete(me.uid, friendUid)
                remote.delete(friendUid, me.uid)
            }

            // MVP: borrar en local
            local.deleteById(friendId)

            Log.d("LUDIARY_FRIENDS_DEBUG", "rejectRequest OK -> deleted local + remote(if possible)")
            Unit
        }
    }
}