package com.ludiary.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FriendsRepositoryImpl(
    private val local: LocalFriendsDataSource,
    private val remote: FirestoreFriendsRepository,
    private val auth: FirebaseAuth
) : FriendsRepository {

    override fun observeFriends(query: String): Flow<List<FriendEntity>> =
        local.observeFriends(query)

    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeIncomingRequests(query)

    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> =
        local.observeOutgoingRequests(query)

    override fun observeGroups(query: String): Flow<List<FriendEntity>> =
        flowOf(emptyList()) // MVP: grupos solo local (aún no implementado aquí)

    override suspend fun sendInviteByCode(code: String): Result<Unit> {
        val me = auth.currentUser ?: return Result.failure(IllegalStateException("No autenticado"))

        val code = code.trim().uppercase()
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Código vacío"))

        val isValid = code.length in 10..12 && code.all { it.isLetterOrDigit() }
        if (!isValid) return Result.failure(IllegalArgumentException("Código inválido"))

        // Evitar duplicados locales
        val existing = local.getByFriendCode(code)
        if (existing != null) return Result.failure(IllegalStateException("Ya tienes una solicitud con ese código"))

        val now = System.currentTimeMillis()

        // Local-first: pendiente local (sin friendUid todavía)
        local.upsert(
            FriendEntity(
                friendCode = code,
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

        return Result.success(Unit)
    }

    override suspend fun flushOfflineInvites(): Result<Unit> {
        val me = auth.currentUser ?: return Result.failure(IllegalStateException("No autenticado"))

        val pending = local.getPendingToSync()
            .filter { it.status == FriendStatus.PENDING_OUTGOING_LOCAL && it.syncStatus == SyncStatus.PENDING }

        for (p in pending) {
            val code = p.friendCode ?: continue

            val target = runCatching { remote.findUserByFriendCode(code) }.getOrNull()
            if (target == null) {
                // No se puede resolver -> eliminar para no dejarlo atascado (mensaje neutro en UI)
                local.deleteById(p.id)
                continue
            }

            // Evitar auto-invite si alguien pega su propio código
            if (target.uid == me.uid) {
                local.deleteById(p.id)
                continue
            }

            val now = System.currentTimeMillis()

            // Remoto: ambos lados
            runCatching {
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

                // Local: ya conocemos friendUid y pasa a pendiente “remota”
                local.updateStatusAndUid(
                    id = p.id,
                    status = FriendStatus.PENDING_OUTGOING,
                    friendUid = target.uid,
                    syncStatus = SyncStatus.CLEAN
                )
            }
        }

        return Result.success(Unit)
    }

    override suspend fun acceptRequest(friendId: Long): Result<Unit> {
        val me = auth.currentUser ?: return Result.failure(IllegalStateException("No autenticado"))

        val localReq = local.getById(friendId)
            ?: return Result.failure(IllegalStateException("Solicitud no encontrada"))

        val friendUid = localReq.friendUid
            ?: return Result.failure(IllegalStateException("Solicitud incompleta (sin uid remoto)"))

        val now = System.currentTimeMillis()

        return runCatching {
            // Remoto: ambos pasan a ACCEPTED
            remote.upsert(
                uid = me.uid,
                friendUid = friendUid,
                data = FirestoreFriendsRepository.RemoteFriend(
                    friendUid = friendUid,
                    email = null,
                    displayName = localReq.displayName,
                    nickname = localReq.nickname,
                    status = FriendStatus.ACCEPTED.name,
                    createdAt = localReq.createdAt,
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
                    createdAt = localReq.createdAt,
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
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> {
        val me = auth.currentUser ?: return Result.failure(IllegalStateException("No autenticado"))

        val localReq = local.getById(friendId)
            ?: return Result.failure(IllegalStateException("Solicitud no encontrada"))

        val friendUid = localReq.friendUid
            ?: run {
                // Si era solo local incompleta, borramos y listo
                local.deleteById(friendId)
                return Result.success(Unit)
            }

        return runCatching {
            // Remoto: se elimina relación (no quedan “amigos”)
            remote.delete(me.uid, friendUid)
            remote.delete(friendUid, me.uid)

            // Local
            local.deleteById(friendId)
        }.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
}