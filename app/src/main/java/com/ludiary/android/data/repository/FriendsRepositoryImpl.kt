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

    override fun observeGroups(query: String): Flow<List<FriendEntity>> {
        // TODO: cuando tengas GroupEntity/GroupDao, lo cambiamos.
        return flowOf(emptyList())
    }

    override suspend fun upsert(friend: FriendEntity) {
        local.upsert(friend)
    }

    override suspend fun setSyncStatus(id: Long, status: SyncStatus) {
        local.setSyncStatus(id, status)
    }

    override suspend fun acceptRequest(friendId: Long): Result<Unit> {
        return Result.failure(NotImplementedError("acceptRequest pendiente"))
    }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> {
        return Result.failure(NotImplementedError("rejectRequest pendiente"))
    }


    override suspend fun sendInviteByCode(codeRaw: String): Result<Unit> {
        val me = auth.currentUser ?: return Result.failure(IllegalStateException("No autenticado"))

        val code = codeRaw.trim()
        if (code.isBlank()) return Result.failure(IllegalArgumentException("Código vacío"))

        val isValid = code.length in 10..12 && code.all { it.isLetterOrDigit() }
        if (!isValid) return Result.failure(IllegalArgumentException("Código inválido"))

        // Evitar duplicados locales por código (pendiente local)
        val existing = local.getByFriendCode(code)
        if (existing != null) {
            return Result.failure(IllegalStateException("Ya tienes una solicitud con ese código"))
        }

        val now = System.currentTimeMillis()

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
                // Código no válido / no existe -> borrar local (y mensaje neutro lo gestiona UI si quieres)
                local.deleteById(p.id)
                continue
            }

            val now = System.currentTimeMillis()

            // Crear en remoto ambos lados
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

                // Actualizar local: ya tenemos uid y pasa de LOCAL -> remoto
                local.upsert(
                    p.copy(
                        friendUid = target.uid,
                        status = FriendStatus.PENDING_OUTGOING,
                        syncStatus = SyncStatus.CLEAN,
                        updatedAt = now
                    )
                )
            }
        }

        return Result.success(Unit)
    }
}