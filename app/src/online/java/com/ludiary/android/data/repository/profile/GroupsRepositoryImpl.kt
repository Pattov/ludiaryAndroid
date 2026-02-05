import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.local.entity.GroupMemberEntity
import com.ludiary.android.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementación de [GroupsRepositoryImpl] (módulo social: grupos e invitaciones).
 * @param local Fuente de datos local (Room).
 * @param remote Fuente de datos remota (Firestore).
 * @param function Repositorio de Cloud Functions (acciones seguras).
 * @param auth FirebaseAuth para obtener el usuario actual.
 */
class GroupsRepositoryImpl(
    private val local: LocalGroupsDataSource,
    private val remote: FirestoreGroupsRepository,
    private val function: FunctionsSocialRepository,
    private val auth: FirebaseAuth
) : GroupsRepository {

    private var remoteSyncJob: Job? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val groupDocJobs = mutableMapOf<String, Job>()

    /**
     * Observa la lista de grupos del usuario, aplicando filtrado por texto.
     * @param query Texto de búsqueda. Puede ser vacío para devolver todos los grupos.
     */
    override fun observeGroups(query: String) = local.observeGroups(query)

    /**
     * Observa la lista de miembros de un grupo.
     * @param groupId Identificador del grupo.
     */
    override fun observeMembers(groupId: String) = local.observeMembers(groupId)

    /**
     * Observa invitaciones entrantes pendientes (pendientes de aceptar).
     */
    override fun observeIncomingPendingInvites() =
        local.observePendingInvites(auth.currentUser?.uid)

    /**
     * Observa invitaciones salientes pendientes creadas por el usuario (status pending).
     */
    override fun observeOutgoingPendingInvites() =
        local.observeOutgoingPendingInvites(auth.currentUser?.uid)

    // ------------------------- SYNC (FIRESTORE → ROOM) -------------------------

    /**
     * Inicia la sincronización remota (realtime) con Firestore.
     */
    override fun startRemoteSync() {
        val me = auth.currentUser ?: return

        // Evitar listeners duplicados
        remoteSyncJob?.cancel()

        remoteSyncJob = repoScope.launch {

            // 1) Índice users/{uid}/groups
            launch {
                remote.observeUserGroupsIndex(me.uid).collect { remoteGroups ->

                    // Upsert básico (nombre y fechas)
                    local.upsertGroups(remoteGroups.map { it.toEntity() })

                    // Gestionar jobs por groupId: arrancar/parar observers del doc de grupo
                    val groupIds = remoteGroups.map { it.groupId }.toSet()

                    // Parar jobs de grupos que ya no están
                    (groupDocJobs.keys - groupIds).forEach { gid ->
                        groupDocJobs.remove(gid)?.cancel()
                    }

                    // Arrancar jobs nuevos
                    (groupIds - groupDocJobs.keys).forEach { gid ->
                        groupDocJobs[gid] = repoScope.launch {
                            remote.observeGroupDoc(gid).collect { (count, updatedAt) ->

                                val meta = remoteGroups.firstOrNull { it.groupId == gid }

                                // Upsert “completo” del grupo con membersCount actualizado
                                local.upsertGroup(
                                    GroupEntity(
                                        groupId = gid,
                                        nameSnapshot = meta?.nameSnapshot ?: "Grupo",
                                        createdAt = meta?.joinedAt ?: System.currentTimeMillis(),
                                        updatedAt = updatedAt,
                                        membersCount = count
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2) Invitaciones RECIBIDAS (pending)
            launch {
                remote.observeIncomingPendingInvites(me.uid).collect { remoteInvites ->
                    val items = remoteInvites.map { it.toEntity() }

                    local.upsertInvites(items)

                    // Limpieza incoming: si ya no están en remoto, se eliminan en local
                    val remoteIds = items.map { it.inviteId }
                    if (remoteIds.isEmpty()) {
                        local.deleteAllIncomingPendingInvites(me.uid)
                    } else {
                        local.deleteMissingIncomingInvites(me.uid, remoteIds)
                    }
                }
            }

            // 3) Invitaciones ENVIADAS por mí (pending)
            launch {
                remote.observeOutgoingPendingInvites(me.uid).collect { remoteInvites ->
                    val items = remoteInvites.map { it.toEntity() }

                    local.upsertInvites(items)

                    // Limpieza outgoing: si ya no están en remoto, se eliminan en local
                    val remoteIds = items.map { it.inviteId }
                    if (remoteIds.isEmpty()) {
                        local.deleteAllOutgoingPendingInvites(me.uid)
                    } else {
                        local.deleteMissingOutgoingInvites(me.uid, remoteIds)
                    }
                }
            }
        }
    }

    /**
     * Detiene listeners realtime y jobs por grupo.
     */
    override fun stopRemoteSync() {
        remoteSyncJob?.cancel()
        remoteSyncJob = null
        groupDocJobs.values.forEach { it.cancel() }
        groupDocJobs.clear()
    }

    /**
     * Crea un grupo mediante Cloud Functions y actualiza inmediatamente la caché local.
     * @param name Nombre del grupo.
     * @return `Result.success(Unit)` si se crea correctamente; `failure` si no hay sesión o falla backend/local.
     */
    override suspend fun createGroup(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.groups_error_no_session)

            val created = function.createGroup(name)

            // Cache local inmediata
            local.upsertGroup(
                GroupEntity(
                    groupId = created.groupId,
                    nameSnapshot = created.name,
                    createdAt = created.now,
                    updatedAt = created.now,
                    membersCount = created.membersCount
                )
            )

            val meUid = auth.currentUser?.uid ?: return@runCatching
            local.upsertMember(groupMemberEntity(created.groupId, meUid, created.now))
        }
    }

    /**
     * Invita a un usuario a unirse a un grupo.
     * @param groupId ID del grupo.
     * @param groupNameSnapshot Nombre del grupo (snapshot) para UI consistente.
     * @param toUid UID del usuario invitado.
     */
    override suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.groups_error_no_session)

            val now = System.currentTimeMillis()

            // Se genera un inviteId determinista para evitar duplicados locales (ej: groupId_toUid).
            val inviteId = "${groupId}_${toUid}"

            // Cache local inmediata para mostrar en UI.
            local.upsertInvite(
                GroupInviteEntity(
                    inviteId = inviteId,
                    groupId = groupId,
                    groupNameSnapshot = groupNameSnapshot,
                    fromUid = me.uid,
                    toUid = toUid,
                    status = "PENDING",
                    createdAt = now,
                    respondedAt = null
                )
            )

            try {
                function.inviteToGroup(
                    groupId = groupId,
                    groupNameSnapshot = groupNameSnapshot,
                    toUid = toUid,
                    clientCreatedAt = now
                )
            } catch (e: Exception) {
                Log.w(
                    "LUDIARY_GROUPS_DEBUG",
                    "inviteToGroup pending/offline inviteId=$inviteId",
                    e
                )
                throw e
            }

            Unit
        }
    }

    /**
     * Reintenta enviar invitaciones salientes pendientes.
     * @return `Result.success(Unit)` si el proceso no lanza excepción; `failure` si falla alguna llamada.
     */
    override suspend fun flushPendingInvites(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: return@runCatching
            val pending = local.pendingOutgoingInvitesAll(me.uid)

            pending.forEach { inv ->
                function.inviteToGroup(
                    groupId = inv.groupId,
                    groupNameSnapshot = inv.groupNameSnapshot,
                    toUid = inv.toUid,
                    clientCreatedAt = inv.createdAt
                )
            }
        }
    }

    /**
     * Acepta una invitación de grupo (entrante).
     * @param inviteId ID de invitación.
     */
    override suspend fun acceptInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.groups_error_no_session)
            function.acceptGroupInvite(inviteId)
        }
    }

    /**
     * Cancela una invitación enviada (normalmente por el emisor).
     * @param inviteId ID de invitación.
     */
    override suspend fun cancelInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.groups_error_no_session)
            function.cancelGroupInvite(inviteId)
        }
    }

    /**
     * Rechaza una invitación recibida (entrante) y limpia el registro local.
     * @param inviteId ID de invitación.
     */
    override suspend fun rejectInvite(inviteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.currentUser ?: error(R.string.groups_error_no_session)
            function.rejectGroupInvite(inviteId)
            local.deleteInvite(inviteId)
        }
    }

    /**
     * Devuelve invitaciones salientes pendientes asociadas a un grupo.
     * @param groupId ID del grupo.
     * @return Lista de invitaciones pendientes para ese grupo, filtradas por el UID del usuario actual.
     */
    override suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> =
        withContext(Dispatchers.IO) {
            val me = auth.currentUser ?: return@withContext emptyList()
            local.pendingOutgoingInvitesForGroup(groupId, me.uid)
        }

    /**
     * Abandona un grupo.
     * @param groupId ID del grupo.
     */
    override suspend fun leaveGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.groups_error_no_session)

            try {
                function.leaveGroup(groupId)
            } catch (e: Exception) {
                Log.w("LUDIARY_GROUPS_DEBUG", "leaveGroup cloud failed groupId=$groupId", e)
                throw e
            }

            local.leaveGroupLocal(groupId, me.uid)
        }
    }
}