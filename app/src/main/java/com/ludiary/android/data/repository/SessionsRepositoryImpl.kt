package com.ludiary.android.data.repository

import com.ludiary.android.data.local.LocalSessionsDataSource
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.sync.SyncPrefs

class SessionsRepositoryImpl(
    private val local: LocalSessionsDataSource,
    private val remote: FirestoreSessionsRepository,
    private val syncPrefs: SyncPrefs,
    private val groupIdProvider: GroupIdProvider
) : SessionsRepository {

    override suspend fun sync(uid: String): SessionsSyncResult {

        val adopted = local.adoptOfflinePersonalSessionsForUser(uid)

        val pushed = pushPending()

        val (pulledPersonal, pulledGroups) = pullIncremental(uid)

        return SessionsSyncResult(
            adopted = adopted,
            pushed = pushed,
            pulledPersonal = pulledPersonal,
            pulledGroups = pulledGroups
        )
    }

    private suspend fun pushPending(): Int {
        val pending = local.getPendingForPush()
        var pushed = 0

        for (s in pending) {
            if (s.ownerUserId.isNullOrBlank()) continue

            if (s.syncStatus == SyncStatus.DELETED || s.isDeleted) {
                remote.softDeleteSession(s.id)
                local.hardDeleteCascade(s.id)
                pushed++
            } else {
                val withPlayers = local.getWithPlayers(s.id) ?: continue
                remote.upsertSession(withPlayers)
                local.markClean(s.id, System.currentTimeMillis())
                pushed++
            }
        }
        return pushed
    }


    private suspend fun pullIncremental(uid: String): Pair<Int, Int> {
        var pulledPersonal = 0
        var pulledGroups = 0

        // PERSONAL
        val lastPersonal = syncPrefs.getLastPullPersonalSessions(uid)
        val personalChanged = remote.fetchPersonalChangedSince(uid, lastPersonal)

        var maxPersonal: Long? = null
        for (r in personalChanged) {
            maxPersonal = maxOf(maxPersonal ?: 0L, r.updatedAtMillis ?: 0L)

            if (r.isDeleted) {
                local.hardDeleteCascade(r.id)
            } else {
                local.applyRemoteReplacePlayers(r.sessionEntity, r.playerEntities)
            }
            pulledPersonal++
        }
        if (maxPersonal != null && maxPersonal > 0L) {
            syncPrefs.setLastPullPersonalSessions(uid, maxPersonal)
        }

        // GROUPS
        val groupIds = groupIdProvider.getGroupIdsForUser(uid)
        for (gid in groupIds) {
            val lastGroup = syncPrefs.getLastPullGroupSessions(uid, gid)
            val groupChanged = remote.fetchGroupChangedSince(gid, lastGroup)

            var maxGroup: Long? = null
            for (r in groupChanged) {
                maxGroup = maxOf(maxGroup ?: 0L, r.updatedAtMillis ?: 0L)

                if (r.isDeleted) {
                    local.hardDeleteCascade(r.id)
                } else {
                    local.applyRemoteReplacePlayers(r.sessionEntity, r.playerEntities)
                }
                pulledGroups++
            }
            if (maxGroup != null && maxGroup > 0L) {
                syncPrefs.setLastPullGroupSessions(uid, gid, maxGroup)
            }
        }

        return pulledPersonal to pulledGroups
    }
}

interface GroupIdProvider {
    suspend fun getGroupIdsForUser(uid: String): List<String>
}