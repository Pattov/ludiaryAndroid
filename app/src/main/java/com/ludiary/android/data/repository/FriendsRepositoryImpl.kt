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

            remote.observeAll(me.uid).collect { remoteList ->

                for (rf in remoteList) {
                    val status = runCatching { FriendStatus.valueOf(rf.status) }.getOrNull()
                    if (status == null) {
                        continue
                    }

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

            Unit
        }
    }

    override suspend fun flushOfflineInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: return@runCatching

            val pending = local.getPendingInvites()

            for (entity in pending) {
                val localId = entity.id
                val senderName = remote.getUserDisplayName(me.uid) ?: "Usuario"
                val now = System.currentTimeMillis()

                val code = entity.friendCode?.trim()?.uppercase().orEmpty()
                if (code.isBlank()) continue

                val myCode = remote.findFriendCodeByUid(me.uid)
                val targetCode = code // ya lo tienes normalizado

                val target = remote.findUserByFriendCode(code)
                if (target == null) {
                    continue
                }

                // ✅ No permitir enviarse a uno mismo
                if (target.uid == me.uid) {
                    local.deleteById(localId)
                    continue
                }

                // ✅ Caso cruce de solicitudes:
                // Si el target YA me había enviado solicitud (yo lo tengo como PENDING_INCOMING),
                // enviarle yo solicitud debe equivaler a ACEPTAR automáticamente.
                val existingIncoming = local.getByFriendUid(target.uid)
                if (existingIncoming != null && existingIncoming.status == FriendStatus.PENDING_INCOMING) {

                    val nowAccept = System.currentTimeMillis()
                    val myCodeResolved = remote.findFriendCodeByUid(me.uid)
                    val senderNameResolved = remote.getUserDisplayName(me.uid) ?: "Usuario"

                    // Firestore: ambos ACCEPTED
                    remote.upsert(
                        uid = me.uid,
                        friendUid = target.uid,
                        data = FirestoreFriendsRepository.RemoteFriend(
                            friendUid = target.uid,
                            friendCode = code, // el código del target (el que tú has escrito)
                            displayName = target.displayName,
                            nickname = existingIncoming.nickname,
                            status = FriendStatus.ACCEPTED.name,
                            createdAt = existingIncoming.createdAt,
                            updatedAt = nowAccept
                        )
                    )

                    remote.upsert(
                        uid = target.uid,
                        friendUid = me.uid,
                        data = FirestoreFriendsRepository.RemoteFriend(
                            friendUid = me.uid,
                            friendCode = myCodeResolved,
                            displayName = senderNameResolved,
                            nickname = null,
                            status = FriendStatus.ACCEPTED.name,
                            createdAt = existingIncoming.createdAt,
                            updatedAt = nowAccept
                        )
                    )

                    // Local: marco ACCEPTED la fila existente (la incoming)
                    local.updateStatusAndUid(
                        id = existingIncoming.id,
                        status = FriendStatus.ACCEPTED,
                        friendUid = target.uid,
                        syncStatus = SyncStatus.CLEAN
                    )

                    // Local: borro el pendiente local que acabamos de procesar (para no dejar basura)
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
            }

        }.onFailure { e ->
            Log.w(
                "LUDIARY_FRIENDS_SYNC",
                "flushOfflineInvites failed: ${e::class.simpleName} - ${e.message}"
            )
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

            Unit
        }
    }

    override fun stopRemoteSync() {
        remoteSyncJob?.cancel()
        remoteSyncJob = null
    }

    override suspend fun updateNickname(friendId: Long, nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error("No hay sesión")
                val entity = local.getById(friendId) ?: error("No existe en local")
                val friendUid = entity.friendUid ?: error("No tiene friendUid")

                val now = System.currentTimeMillis()

                remote.upsert(
                    uid = me.uid,
                    friendUid = friendUid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = friendUid,
                        friendCode = entity.friendCode,
                        displayName = entity.displayName,
                        nickname = nickname,
                        status = entity.status.name,
                        createdAt = entity.createdAt,
                        updatedAt = now
                    )
                )

                local.updateNickname(friendId, nickname, now)
            }
        }

    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: return@runCatching

            val friendUid = entity.friendUid

            // MVP: borrar en remoto si tenemos uid
            if (!friendUid.isNullOrBlank()) {
                remote.delete(me.uid, friendUid)
                remote.delete(friendUid, me.uid)
            }

            // MVP: borrar en local
            local.deleteById(friendId)

            Unit
        }
    }

    override suspend fun removeFriend(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error("No hay sesión")
            val entity = local.getById(friendId) ?: return@runCatching Unit
            val friendUid = entity.friendUid ?: error("No tiene friendUid")

            remote.delete(me.uid, friendUid)
            remote.delete(friendUid, me.uid)

            local.deleteById(friendId)

            Unit
        }
    }

}