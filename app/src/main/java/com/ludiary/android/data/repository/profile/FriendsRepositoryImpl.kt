package com.ludiary.android.data.repository.profile

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
 * @param function Repositorio de Cloud Functions para operaciones transaccionales/seguras.
 * @param auth FirebaseAuth
 */
class FriendsRepositoryImpl(
    private val local: LocalFriendsDataSource,
    private val remote: FirestoreFriendsRepository,
    private val function: FunctionsSocialRepository,
    private val auth: FirebaseAuth
) : FriendsRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastUid: String? = null
    private var remoteSyncJob: Job? = null

    /**
     * Observa la lista de amigos aceptados aplicando filtrado por texto.
     * @param query Texto de búsqueda (puede ser vacío).
     * @return Flujo de amigos (Room emite cambios automáticamente).
     */
    override fun observeFriends(query: String): Flow<List<FriendEntity>> = local.observeFriends(query)

    /**
     * Observa la lista de “grupos” (si en tu tabla FriendEntity se reutiliza para grupos).
     * @param query Texto de búsqueda (puede ser vacío).
     * @return Flujo con los elementos filtrados.
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
                    friendUid = null,
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
     * Intenta enviar todas las invitaciones creadas offline (pendientes) mediante Cloud Functions.
     * @return `Result.success(Unit)` incluso si alguna falla, pero el fallo queda logueado.
     */
    override suspend fun flushOfflineInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: return@runCatching

            val pending = local.getPendingInvites()

            for (entity in pending) {
                val localId = entity.id
                val code = entity.friendCode?.trim()?.uppercase().orEmpty()
                if (code.isBlank()) continue

                val result = function.sendFriendInviteByCode(
                    code = code,
                    clientCreatedAt = entity.createdAt
                )

                val friendUid = result.friendUid
                if (!friendUid.isNullOrBlank()) {
                    local.updateStatusAndUid(
                        id = localId,
                        status = FriendStatus.PENDING_OUTGOING,
                        friendUid = friendUid,
                        syncStatus = SyncStatus.CLEAN
                    )
                } else {
                    // Si backend no devuelve un UID válido, eliminamos el registro local para evitar “basura”.
                    local.deleteById(localId)
                }
            }
        }.onFailure { e ->
            Log.w(
                "LUDIARY_FRIENDS_SYNC",
                "flushOfflineInvites failed: ${e::class.simpleName} - ${e.message}"
            )
        }
    }

    /**
     * Acepta una solicitud entrante de amistad.
     * @param friendId ID local (Room) de la solicitud.
     * @return `Result.success(Unit)` si se acepta correctamente; `failure` si falta sesión/datos o falla backend.
     */
    override suspend fun acceptRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.friends_error_no_session)
            val entity = local.getById(friendId) ?: error(R.string.friends_error_friend_not_synced)

            val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_code_not_found)

            function.acceptFriend(friendUid)

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
     * Rechaza una solicitud entrante de amistad.
     * @param friendId ID local (Room) de la solicitud.
     */
    override suspend fun rejectRequest(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.friends_error_no_session)
            val entity = local.getById(friendId) ?: return@runCatching

            val friendUid = entity.friendUid
            if (!friendUid.isNullOrBlank()) {
                function.rejectFriend(friendUid)
            }

            local.deleteById(friendId)
            Unit
        }
    }

    /**
     * Elimina una amistad existente.
     * @param friendId ID local (Room) de la amistad.
     */
    override suspend fun removeFriend(friendId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.friends_error_no_session)

            val entity = local.getById(friendId) ?: error(R.string.friends_error_friend_code_not_found)
            val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_not_synced)

            function.removeFriend(friendUid)
            local.deleteById(friendId)

            Unit
        }
    }

    /**
     * Actualiza el apodo (nickname) asignado a un amigo.
     * @param friendId ID local del amigo.
     * @param nickname Nuevo apodo.
     */
    override suspend fun updateNickname(friendId: Long, nickname: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.friends_error_no_session)
            val entity = local.getById(friendId) ?: error(R.string.friends_error_friend_not_synced)
            val friendUid = entity.friendUid ?: error(R.string.friends_error_friend_code_not_found)

            function.updateFriendNickname(friendUid, nickname)

            val now = System.currentTimeMillis()
            local.updateNickname(friendId, nickname, now)
        }
    }
}