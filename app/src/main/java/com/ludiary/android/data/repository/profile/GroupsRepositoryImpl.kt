package com.ludiary.android.data.repository.profile

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
 * Implementación de [GroupsRepository].
 * @param local Fuente de datos local (Room).
 * @param remote Fuente de datos remota (Firestore).
 * @param auth FirebaseAuth para obtener el usuario actual.
 */
class GroupsRepositoryImpl(
    private val local: LocalGroupsDataSource,
    private val remote: FirestoreGroupsRepository,
    private val auth: FirebaseAuth
) : GroupsRepository {

    private var remoteSyncJob: Job? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val groupDocJobs = mutableMapOf<String, Job>()

    /**
     * Observa la lista de grupos del usuario, con filtrado por texto.
     * @param query Texto de búsqueda. Puede ser vacío para devolver todos los grupos.
     * @return Flow reactivo con la lista de [GroupEntity].
     */
    override fun observeGroups(query: String) = local.observeGroups(query)

    /**
     * Observa la lista de miembros de un grupo.
     * @param groupId Identificador del grupo.
     * @return Flow reactivo con la lista de [GroupMemberEntity].
     */
    override fun observeMembers(groupId: String) = local.observeMembers(groupId)

    /**
     * Observa invitaciones entrantes pendientes (pendientes de aceptar).
     * @return Flow reactivo con la lista de [GroupInviteEntity] entrantes.
     */
    override fun observeIncomingPendingInvites() = local.observePendingInvites(auth.currentUser?.uid)

    /**
     * Observa invitaciones salientes pendientes creadas por el usuario.
     * @return Flow reactivo con la lista de [GroupInviteEntity] salientes.
     */
    override fun observeOutgoingPendingInvites() = local.observeOutgoingPendingInvites(auth.currentUser?.uid)

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
     * Crea un grupo remoto y hace cache local inmediata.
     * @param name Nombre del grupo.
     * @return Result<Unit>
     *     Éxito si se crea y se guarda en local;
     *     failure si no hay sesión o falla remoto.
     */
    override suspend fun createGroup(name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.groups_error_no_session)

                val created = remote.createGroup(me.uid, name)

                // Cache local inmediata (mappers)
                local.upsertGroup(created.toEntity())
                local.upsertMember(groupMemberEntity(created.groupId, me.uid, created.now))
            }
        }

    /**
     * Envía una invitación a un grupo (offline-first).
     * @param groupId ID del grupo.
     * @param groupNameSnapshot Nombre del grupo (snapshot).
     * @param toUid UID del destinatario.
     * @return Result<Unit>
     *     Éxito si se registra;
     *     failure si no hay sesión o falla el remoto.
     */
    override suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val me = auth.currentUser ?: error(R.string.groups_error_no_session)
            val now = System.currentTimeMillis()
            val inviteId = "${groupId}_${toUid}"

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
                remote.createInvite(
                    inviteId = inviteId,
                    groupId = groupId,
                    groupNameSnapshot = groupNameSnapshot,
                    fromUid = me.uid,
                    toUid = toUid,
                    createdAt = now
                )
            } catch (e: Exception) {
                Log.w("LUDIARY_GROUPS_DEBUG", "inviteToGroup pending/offline inviteId=$inviteId", e)
                throw e
            }
        }
    }

    /**
     * Reintenta sincronizar invitaciones salientes pendientes guardadas en local.
     * @return Result<Unit>
     *     Éxito si procesa la cola sin error fatal.
     *     failure si algo revienta.
     */
    override suspend fun flushPendingInvites(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: return@runCatching
                val pending = local.pendingOutgoingInvitesAll(me.uid)

                pending.forEach { inv ->
                    val exists = try {
                        remote.inviteExists(inv.inviteId)
                    } catch (_: Exception) {
                        false
                    }

                    if (!exists) {
                        remote.createInvite(
                            inviteId = inv.inviteId,
                            groupId = inv.groupId,
                            groupNameSnapshot = inv.groupNameSnapshot,
                            fromUid = inv.fromUid,
                            toUid = inv.toUid,
                            createdAt = inv.createdAt
                        )
                    }
                }
            }
        }

    /**
     * Acepta una invitación entrante.
     * @param inviteId ID de la invitación.
     * @return Result<Unit> Éxito si se acepta; failure si no hay sesión o falla remoto.
     */
    override suspend fun acceptInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.groups_error_no_session)

                val accepted = remote.acceptInvite(me.uid, inviteId)

                // Local
                val now = accepted.respondedAt ?: System.currentTimeMillis()

                local.upsertGroup(
                    GroupEntity(
                        groupId = accepted.groupId,
                        nameSnapshot = accepted.groupNameSnapshot,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                local.upsertMember(groupMemberEntity(accepted.groupId, me.uid, now))
                local.upsertInvite(accepted.toEntity())
            }
        }

    /**
     * Cancela una invitación (saliente o por cualquiera de las partes según reglas remotas).
     * @param inviteId ID de la invitación.
     * @return Result<Unit>
     *     Éxito si se cancela.
     *     Si no existe en remoto, no hace nada.
     */
    override suspend fun cancelInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.groups_error_no_session)

                val cancelled = remote.cancelInvite(me.uid, inviteId) ?: return@runCatching

                local.upsertInvite(cancelled.toEntity())
            }
        }

    /**
     * Rechaza una invitación entrante.
     * @param inviteId ID de la invitación.
     * @return Result<Unit>
     *     Éxito si se rechaza;
     *     failure si no hay sesión o falla remoto.
     */
    override suspend fun rejectInvite(inviteId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.groups_error_no_session)
                remote.rejectInvite(me.uid, inviteId)
                local.deleteInvite(inviteId)
            }
        }

    /**
     * Devuelve invitaciones salientes pendientes asociadas a un grupo.
     * @param groupId ID del grupo.
     * @return Lista de invitaciones pendientes para ese grupo.
     */
    override suspend fun pendingOutgoingInvitesForGroup(groupId: String): List<GroupInviteEntity> =
        withContext(Dispatchers.IO) {
            val me = auth.currentUser ?: return@withContext emptyList()
            local.pendingOutgoingInvitesForGroup(groupId, me.uid)
        }

    /**
     * Abandona un grupo.
     * @param groupId ID del grupo.
     * @return Result<Unit>
     *     Éxito si se abandona;
     *     failure si no hay sesión o falla remoto.
     */
    override suspend fun leaveGroup(groupId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val me = auth.currentUser ?: error(R.string.groups_error_no_session)

                try {
                    remote.leaveGroup(me.uid, groupId)
                } catch (e: Exception) {
                    Log.w("LUDIARY_GROUPS_DEBUG", "leaveGroup remote failed groupId=$groupId", e)
                    throw e
                }

                local.leaveGroupLocal(groupId, me.uid)
            }
        }
}