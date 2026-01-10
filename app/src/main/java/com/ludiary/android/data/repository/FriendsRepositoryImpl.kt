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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FriendsRepositoryImpl(
    private val local: LocalFriendsDataSource,
    private val remote: FirestoreFriendsRepository,
    private val auth: FirebaseAuth
) : FriendsRepository {

    // --- bridge Firestore -> Room ---
    private var remoteSyncJob: Job? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---------- OBSERVE (desde Room) ----------

    override fun observeFriends(query: String): Flow<List<FriendEntity>> =
        local.observeFriends(query)

    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeIncomingRequests(query)

    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeOutgoingRequests(query)

    /**
     * Grupos es local y NO afecta a invitaciones.
     * Si todavía no tienes Room/DAO de grupos, devolvemos vacío para no romper.
     * Cuando lo implementes, sustitúyelo por local.observeGroups(query).
     */
    override fun observeGroups(query: String): Flow<List<FriendEntity>> = emptyFlow()

    // ---------- ACTIONS ----------

    override suspend fun sendInviteByCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        val me = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No autenticado"))

        val codeNorm = code.trim().uppercase()
        Log.d("LUDIARY_FRIENDS_DEBUG", "sendInviteByCode() code=$codeNorm from=${me.uid}")

        if (codeNorm.length !in 10..12) {
            return@withContext Result.failure(IllegalArgumentException("Código inválido"))
        }

        val existing = local.getByFriendCode(codeNorm)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("Ya existe una solicitud con ese código"))
        }

        val now = System.currentTimeMillis()

        // ✅ local-first
        local.upsert(
            FriendEntity(
                friendCode = codeNorm,
                friendUid = null,
                displayName = null,
                nickname = null,
                status = FriendStatus.PENDING_OUTGOING_LOCAL,
                requestedByUid = me.uid,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )

        Log.d("LUDIARY_FRIENDS_DEBUG", "Local saved PENDING_OUTGOING_LOCAL + PENDING")

        Result.success(Unit)
    }

    override suspend fun acceptRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val me = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No autenticado"))

        val entity = local.getById(friendId)
            ?: return@withContext Result.failure(IllegalStateException("Solicitud no encontrada en local"))

        if (entity.status != FriendStatus.PENDING_INCOMING) {
            return@withContext Result.failure(IllegalStateException("No es una solicitud entrante"))
        }

        val friendUid = entity.friendUid
            ?: return@withContext Result.failure(IllegalStateException("friendUid es null; falta sincronización remota->local"))

        val now = System.currentTimeMillis()

        Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest id=$friendId friendUid=$friendUid")

        runCatching {
            // ✅ Firestore: ambos pasan a ACCEPTED
            remote.upsert(
                uid = me.uid,
                friendUid = friendUid,
                data = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = friendUid,
                    email = null,
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
                    email = null,
                    displayName = me.displayName,
                    nickname = null,
                    status = FriendStatus.ACCEPTED.name,
                    createdAt = entity.createdAt,
                    updatedAt = now
                )
            )

            // ✅ Room: ACCEPTED + CLEAN
            local.updateStatusAndUid(
                id = friendId,
                status = FriendStatus.ACCEPTED,
                friendUid = friendUid,
                syncStatus = SyncStatus.CLEAN
            )

            Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest OK -> local ACCEPTED")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = {
                Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest FAIL err=${it.message}")
                Result.failure(it)
            }
        )
    }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val me = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No autenticado"))

        val entity = local.getById(friendId)
            ?: return@withContext Result.failure(IllegalStateException("Solicitud no encontrada en local"))

        val friendUid = entity.friendUid
            ?: return@withContext Result.failure(IllegalStateException("friendUid es null; falta sincronización remota->local"))

        val now = System.currentTimeMillis()

        Log.d("LUDIARY_FRIENDS_DEBUG", "rejectRequest id=$friendId friendUid=$friendUid")

        runCatching {
            // ✅ Firestore: se elimina en ambos lados
            remote.delete(uid = me.uid, friendUid = friendUid)
            remote.delete(uid = friendUid, friendUid = me.uid)

            // ✅ Room: eliminar fila local
            local.deleteById(friendId)

            Log.d("LUDIARY_FRIENDS_DEBUG", "rejectRequest OK -> local deleted")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = {
                Log.d("LUDIARY_FRIENDS_DEBUG", "rejectRequest FAIL err=${it.message}")
                Result.failure(it)
            }
        )
    }

    override suspend fun flushOfflineInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        val me = auth.currentUser ?: return@withContext Result.failure(IllegalStateException("No autenticado"))

        val pending = local.observePendingToSync().first()
            .filter { it.status == FriendStatus.PENDING_OUTGOING_LOCAL && it.syncStatus == SyncStatus.PENDING }

        Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites() pending=${pending.size}")

        for (p in pending) {
            val code = p.friendCode ?: continue

            Log.d("LUDIARY_FRIENDS_DEBUG", "Resolving friendCode=$code")
            val target = runCatching { remote.findUserByFriendCode(code) }.getOrNull()

            if (target == null) {
                Log.d("LUDIARY_FRIENDS_DEBUG", "friendCode $code not found -> delete local id=${p.id}")
                local.deleteById(p.id)
                continue
            }

            if (target.uid == me.uid) {
                Log.d("LUDIARY_FRIENDS_DEBUG", "self invite -> delete local id=${p.id}")
                local.deleteById(p.id)
                continue
            }

            val now = System.currentTimeMillis()

            runCatching {
                // A -> PENDING_OUTGOING
                remote.upsert(
                    uid = me.uid,
                    friendUid = target.uid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = target.uid,
                        email = null,
                        displayName = target.displayName,
                        nickname = null,
                        status = FriendStatus.PENDING_OUTGOING.name,
                        createdAt = p.createdAt,
                        updatedAt = now
                    )
                )

                // B -> PENDING_INCOMING
                remote.upsert(
                    uid = target.uid,
                    friendUid = me.uid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = me.uid,
                        email = null,
                        displayName = me.displayName,
                        nickname = null,
                        status = FriendStatus.PENDING_INCOMING.name,
                        createdAt = p.createdAt,
                        updatedAt = now
                    )
                )

                // local: enlazado con uid y limpio
                local.updateStatusAndUid(
                    id = p.id,
                    status = FriendStatus.PENDING_OUTGOING,
                    friendUid = target.uid,
                    syncStatus = SyncStatus.CLEAN
                )

                Log.d("LUDIARY_FRIENDS_DEBUG", "Synced invite id=${p.id} -> PENDING_OUTGOING + CLEAN")
            }.onFailure {
                Log.d("LUDIARY_FRIENDS_DEBUG", "Sync failed id=${p.id} err=${it.message}")
                // se queda pendiente para reintentar
            }
        }

        Result.success(Unit)
    }

    // ---------- FIRESTORE -> ROOM (receptor) ----------
    // No está en la interfaz; se llama desde el VM con cast seguro.

    fun startRemoteSync() {
        val me = auth.currentUser ?: run {
            Log.d("LUDIARY_FRIENDS_DEBUG", "startRemoteSync: no auth user")
            return
        }

        if (remoteSyncJob?.isActive == true) {
            Log.d("LUDIARY_FRIENDS_DEBUG", "startRemoteSync: already running")
            return
        }

        Log.d("LUDIARY_FRIENDS_DEBUG", "startRemoteSync uid=${me.uid}")

        remoteSyncJob = repoScope.launch {
            remote.observeAll(me.uid).collectLatest { remoteList ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE size=${remoteList.size}")

                for (rf in remoteList) {
                    val status = runCatching { FriendStatus.valueOf(rf.status) }.getOrNull()
                    if (status == null) {
                        Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE unknown status='${rf.status}', skip")
                        continue
                    }

                    val existing = local.getByFriendUid(rf.friendUid)

                    local.upsert(
                        FriendEntity(
                            id = existing?.id ?: 0L,
                            friendUid = rf.friendUid,
                            friendCode = existing?.friendCode,
                            displayName = rf.displayName,
                            nickname = rf.nickname,
                            status = status,
                            createdAt = rf.createdAt ?: existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = rf.updatedAt ?: System.currentTimeMillis(),
                            syncStatus = SyncStatus.CLEAN
                        )
                    )
                }
            }
        }
    }
}