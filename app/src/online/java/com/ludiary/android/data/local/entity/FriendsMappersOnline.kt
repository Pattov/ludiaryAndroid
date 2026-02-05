package com.ludiary.android.data.local.entity

import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.data.model.SyncStatus
import com.ludiary.android.data.repository.profile.FirestoreFriendsRepository

fun FirestoreFriendsRepository.RemoteFriend.toEntity(
    fallbackNow: Long = System.currentTimeMillis()
): FriendEntity {
    val parsedStatus = runCatching { FriendStatus.valueOf(status) }
        .getOrElse { FriendStatus.ACCEPTED }

    return FriendEntity(
        id = 0L,
        friendUid = friendUid,
        friendCode = friendCode,
        displayName = displayName,
        nickname = nickname,
        status = parsedStatus,
        createdAt = createdAt ?: fallbackNow,
        updatedAt = updatedAt ?: fallbackNow,
        syncStatus = SyncStatus.CLEAN
    )
}