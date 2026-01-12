package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.Timestamp
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
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startRemoteSync() {
        val me = auth.currentUser ?: return
        if (remoteSyncJob?.isActive == true) return

        remoteSyncJob = repoScope.launch {
            Log.d("LUDIARY_FRIENDS_DEBUG", "startRemoteSync uid=${me.uid}")

            remote.observeAll(me.uid).collect { remoteList ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE size=${remoteList.size}")

                for (rf in remoteList) {
                    val status = runCatching { FriendStatus.valueOf(rf.status) }.getOrNull() ?: continue
                    val existing = local.getByFriendUid(rf.friendUid)

                    local.upsert(
                        FriendEntity(
                            id = existing?.id ?: 0L,
                            friendUid = rf.friendUid,
                            friendCode = existing?.friendCode, // remoto no lo trae
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

    override fun observeFriends(query: String): Flow<List<FriendEntity>> =
        local.observeByStatus(FriendStatus.ACCEPTED, query)

    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeByStatus(FriendStatus.PENDING_INCOMING, query)

    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeByStatus(FriendStatus.PENDING_OUTGOING, query)

    override fun observeGroups(query: String): Flow<List<FriendEntity>> =
        local.observeGroups(query)

    override suspend fun sendInviteByCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")

            val trimmed = code.trim().uppercase()
            Log.d("LUDIARY_FRIENDS_DEBUG", "sendInviteByCode() code=$trimmed from=${me.uid}")

            // 1) Guardamos local como “pending local” (aún sin friendUid)
            val now = System.currentTimeMillis()
            local.upsert(
                FriendEntity(
                    friendUid = null,
                    friendCode = trimmed,
                    displayName = null,
                    nickname = null,
                    status = FriendStatus.PENDING_OUTGOING_LOCAL,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )

            Log.d("LUDIARY_FRIENDS_DEBUG", "Local saved PENDING_OUTGOING_LOCAL")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun flushOfflineInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: return@runCatching
            val pending = local.getPendingInvites()
            Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites() pending=${pending.size}")

            for (p in pending) {
                val code = p.friendCode?.trim().orEmpty()
                if (code.isBlank()) {
                    Log.d("LUDIARY_FRIENDS_DEBUG", "pending without code -> delete local id=${p.id}")
                    local.deleteById(p.id)
                    continue
                }

                Log.d("LUDIARY_FRIENDS_DEBUG", "Resolving friendCode=$code localId=${p.id}")
                val target = remote.findUserByFriendCode(code)

                // Mensaje neutro: si no existe, borramos local (o podrías dejarlo y reintentar)
                if (target == null) {
                    Log.d("LUDIARY_FRIENDS_DEBUG", "friendCode MISS -> delete local id=${p.id}")
                    local.deleteById(p.id)
                    continue
                }

                // Bloquea self-invite
                if (target.uid == me.uid) {
                    Log.d("LUDIARY_FRIENDS_DEBUG", "Self-invite blocked -> delete local id=${p.id}")
                    local.deleteById(p.id)
                    continue
                }

                val now = System.currentTimeMillis()

                // ✅ CLAVE ANTI-DUPLICADO:
                // antes de escribir remoto, fija friendUid en el row local
                local.updateStatusAndUid(
                    id = p.id,
                    status = FriendStatus.PENDING_OUTGOING,
                    friendUid = target.uid,
                    updatedAt = now,
                    syncStatus = SyncStatus.CLEAN
                )

                Log.d("LUDIARY_FRIENDS_DEBUG", "Local updated -> friendUid=${target.uid} status=PENDING_OUTGOING")

                // Ahora escribe remoto (dos docs: el mío y el del target)
                val rfMine = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = target.uid,
                    displayName = target.displayName,
                    nickname = null,
                    status = FriendStatus.PENDING_OUTGOING.name,
                    createdAt = now,
                    updatedAt = now
                )
                val rfTarget = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = me.uid,
                    displayName = null,
                    nickname = null,
                    status = FriendStatus.PENDING_INCOMING.name,
                    createdAt = now,
                    updatedAt = now
                )

                Log.d("LUDIARY_FRIENDS_DEBUG", "REMOTE write outgoing(me) + incoming(target)")
                remote.upsert(uid = me.uid, friendUid = target.uid, data = rfMine)
                remote.upsert(uid = target.uid, friendUid = me.uid, data = rfTarget)
            }
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "flushOfflineInvites FAIL err=${e.message}", e)
                Result.failure(e)
            }
        )
    }

    override suspend fun acceptRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: error("No existe en local")
            val friendUid = entity.friendUid ?: error("friendUid null (no resuelto)")

            Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest id=$friendId friendUid=$friendUid")

            val now = System.currentTimeMillis()

            // remoto: ambos lados ACCEPTED
            val mine = FirestoreFriendsRepository.RemoteFriend(
                friendUid = friendUid,
                displayName = entity.displayName,
                nickname = entity.nickname,
                status = FriendStatus.ACCEPTED.name,
                createdAt = entity.createdAt,
                updatedAt = now
            )
            val theirs = FirestoreFriendsRepository.RemoteFriend(
                friendUid = me.uid,
                displayName = null,
                nickname = null,
                status = FriendStatus.ACCEPTED.name,
                createdAt = entity.createdAt,
                updatedAt = now
            )

            remote.upsert(uid = me.uid, friendUid = friendUid, data = mine)
            remote.upsert(uid = friendUid, friendUid = me.uid, data = theirs)

            // local: ACCEPTED
            local.updateStatusAndUid(
                id = friendId,
                status = FriendStatus.ACCEPTED,
                friendUid = friendUid,
                updatedAt = now,
                syncStatus = SyncStatus.CLEAN
            )

            Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest OK -> local ACCEPTED")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "acceptRequest FAIL err=${e.message}", e)
                Result.failure(e)
            }
        )
    }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: return@runCatching
            val otherUid = entity.friendUid

            Log.d("LUDIARY_FRIENDS_DEBUG", "reject/cancel id=$friendId otherUid=$otherUid status=${entity.status}")

            // MVP: “borrar al rechazar”
            // - si era incoming: reject
            // - si era outgoing: cancel
            if (!otherUid.isNullOrBlank()) {
                remote.delete(uid = me.uid, friendUid = otherUid)
                remote.delete(uid = otherUid, friendUid = me.uid)
            }

            local.deleteById(friendId)
            Log.d("LUDIARY_FRIENDS_DEBUG", "reject/cancel OK -> deleted local row")
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e ->
                Log.d("LUDIARY_FRIENDS_DEBUG", "reject/cancel FAIL err=${e.message}", e)
                Result.failure(e)
            }
        )
    }
}