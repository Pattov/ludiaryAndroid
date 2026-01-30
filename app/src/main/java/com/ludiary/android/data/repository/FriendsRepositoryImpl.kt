package com.ludiary.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
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

/**
 * Implementación del repositorio de amigos.
 * @param local Fuente de datos local
 * @param remote Fuente de datos remota
 * @param auth FirebaseAuth
 */
class FriendsRepositoryImpl(
    private val local: LocalFriendsDataSource,
    private val remote: FirestoreFriendsRepository,
    private val auth: FirebaseAuth
) : FriendsRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastUid: String? = null
    private var remoteSyncJob: Job? = null

    /**
     * Observa la lista de amigos aceptados aplicando filtrado por texto
     * @param query Texto de búsqueda
     */
    override fun observeFriends(query: String): Flow<List<FriendEntity>> = local.observeFriends(query)

    /**
     * Observa la lista de grupos
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    override fun observeGroups(query: String): Flow<List<FriendEntity>> = local.observeGroups(query)

    /**
     * Observa las solicitudes entrantes de amistad, aplicando filtro
     * @param query Texto de búsqueda. Puede ser vacío.
     */
    override fun observeIncomingRequests(query: String): Flow<List<FriendEntity>> = local.observeIncomingRequests(query)

    /**
     * Observa las solicitudes salientes de amistad, aplicando filtro
     * @param query Texto de búsqueda. Puede ser vacío.
     *
     */
    override fun observeOutgoingRequests(query: String): Flow<List<FriendEntity>> = local.observeOutgoingRequests(query)

    /**
     * Listener realtime Firestore para sincronizar amigos hacia Room
     * Firebase siempre tiene razón: lo remoto pisa lo local en conflictos
     */
    override fun startRemoteSync() {
        val me = auth.currentUser ?: return

        if (lastUid != null && lastUid != me.uid) {
            repoScope.launch { local.clearAll() }
        }
        lastUid = me.uid

        // Evitar listeners duplicados si se llama desde onStart/onResume, etc.
        remoteSyncJob?.cancel()

        remoteSyncJob = repoScope.launch {

            remote.observeAll(me.uid).collect { remoteList ->

                for (rf in remoteList) {
                    local.upsertRemote(rf)
                }
            }
        }
    }

    /**
     * Detiene el listener realtime de firestore
     */
    override fun stopRemoteSync() {
        remoteSyncJob?.cancel()
        remoteSyncJob = null
    }

    /**
     * Inserta una invitación en Room usando un código de amistad
     * @param code Código de amigo introducido por el usuario
     * @return Result<Unit>
     *     OK si se guardó en local
     *     error si no hay sesión o falla Room
     */
    override suspend fun sendInviteByCode(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.friends_error_no_session)

            val normalized = code.trim().uppercase()

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

    /**
     * Procesa invitaciones pendientes guardadas en local y las sincroniza con Firestore
     * @return Result<Unit>
     *     OK si procesa la cola sin error fatal
     *     failure si ocurre una excepción
     */
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
                val target = remote.findUserByFriendCode(code) ?: continue

                // No permitir enviarse a uno mismo
                if (target.uid == me.uid) {
                    local.deleteById(localId)
                    continue
                }

                // Cruce de solicitudes: aceptar automáticamente si ya había incoming
                val existingIncoming = local.getByFriendUid(target.uid)
                if (existingIncoming != null && existingIncoming.status == FriendStatus.PENDING_INCOMING) {
                    val nowAccept = System.currentTimeMillis()
                    val myCodeResolved = remote.findFriendCodeByUid(me.uid)
                    val senderNameResolved = remote.getUserDisplayName(me.uid) ?: "Usuario"

                    remote.upsert(
                        uid = me.uid,
                        friendUid = target.uid,
                        data = FirestoreFriendsRepository.RemoteFriend(
                            friendUid = target.uid,
                            friendCode = code,
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

                    local.updateStatusAndUid(
                        id = existingIncoming.id,
                        status = FriendStatus.ACCEPTED,
                        friendUid = target.uid,
                        syncStatus = SyncStatus.CLEAN
                    )

                    local.deleteById(localId)
                    continue
                }

                // Atar friendUid en local antes de escribir remoto para evitar duplicados cuando llegue el listener
                local.updateStatusAndUid(
                    id = localId,
                    status = FriendStatus.PENDING_OUTGOING,
                    friendUid = target.uid,
                    syncStatus = SyncStatus.PENDING
                )

                remote.upsert(
                    uid = me.uid,
                    friendUid = target.uid,
                    data = FirestoreFriendsRepository.RemoteFriend(
                        friendUid = target.uid,
                        friendCode = code,
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

                local.updateSyncStatus(localId, SyncStatus.CLEAN)
            }
        }.onFailure { e ->
            Log.w(
                "LUDIARY_FRIENDS_SYNC",
                "flushOfflineInvites failed: ${e::class.simpleName} - ${e.message}"
            )
        }
    }

    /**
     * Acepta una solicitud entrante y confirma la amistad en Firestore.
     * @param friendId ID local (Room) de la solicitud a aceptar.
     * @return Result<Unit>
     *     OK si se acepta correctamente
     *     failure si no hay sesión o faltan datos
     * @throws IllegalStateException Si no existe sesión o la entidad no tiene `friendUid`.
     */
    override suspend fun acceptRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.friends_error_no_session)
            val entity = local.getById(friendId) ?: error(R.string.friends_error_friend_not_synced)

            val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_code_not_found)

            val now = System.currentTimeMillis()

            val friendCodeOfOther = entity.friendCode ?: remote.findFriendCodeByUid(friendUid)
            val myCode = remote.findFriendCodeByUid(me.uid)

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

            local.updateStatusAndUid(
                id = friendId,
                status = FriendStatus.ACCEPTED,
                friendUid = friendUid,
                syncStatus = SyncStatus.CLEAN
            )

            Unit
        }
    }

    /**
     * Obtiene el código de amigo del usuario actual desde Firestore.
     * @return Result<String>
     *     Código de amigo si existe
     *     failure si no hay sesión o no está generado
     */
    override suspend fun getMyFriendCode(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.friends_error_no_session)
            remote.getMyFriendCode(me.uid) ?: error(R.string.friends_error_friend_code_not_found)
        }
    }

    /**
     * Rechaza una solicitud
     * @param friendId ID local (Room) de la solicitud a rechazar
     * @return Result<Unit>
     *     OK se elimina relación en firebase y se borra en local
     *     failure si no hay sesión o ocurre error
     */
    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.friends_error_no_session)
            val entity = local.getById(friendId) ?: return@runCatching

            val friendUid = entity.friendUid

            if (!friendUid.isNullOrBlank()) {
                remote.delete(me.uid, friendUid)
                remote.delete(friendUid, me.uid)
            }

            local.deleteById(friendId)
            Unit
        }
    }

    /**
     * Elimina un amigo confirmado.
     * @param friendId ID local (Room) del amigo a eliminar.
     * @return Result<Unit>
     *     OK si se elimina
     *     failure si no hay sesión o faltan datos.
     */
    override suspend fun removeFriend(friendId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.friends_error_no_session)
                val entity = local.getById(friendId) ?: return@runCatching
                val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_code_not_found)

                remote.delete(me.uid, friendUid)
                remote.delete(friendUid, me.uid)

                local.deleteById(friendId)
            }
        }

    /**
     * Actualiza el nickname(alias) de un amigo
     * @param friendId ID local (Room) del amigo.
     * @param nickname Nuevo alias.
     * @return Result<Unit>
     *     OK si se actualiza
     *     failure si no hay sesión o faltan datos.
     */
    override suspend fun updateNickname(friendId: Long, nickname: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.friends_error_no_session)
                val entity = local.getById(friendId) ?: error(R.string.friends_error_friend_not_synced)
                val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_code_not_found)

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
}