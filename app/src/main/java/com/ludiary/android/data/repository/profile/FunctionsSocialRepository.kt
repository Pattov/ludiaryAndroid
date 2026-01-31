package com.ludiary.android.data.repository.profile

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Capa de escritura para Cloud Functions
 */
class FunctionsSocialRepository(
    private val functions: FirebaseFunctions
) {

    private companion object {
        const val FUNCTION_FRIENDS_SEND_INVITE_BY_CODE = "friendsSendInviteByCode"
        const val FUNCTION_FRIENDS_ACCEPT = "friendsAccept"
        const val FUNCTION_FRIENDS_REJECT = "friendsReject"
        const val FUNCTION_FRIENDS_REMOVE = "friendsRemove"
        const val FUNCTION_FRIENDS_UPDATE_NICKNAME = "friendsUpdateNickname"

        const val FUNCTION_GROUPS_CREATE = "groupsCreate"
        const val FUNCTION_GROUPS_INVITE = "groupsInvite"
        const val FUNCTION_GROUPS_ACCEPT_INVITE = "groupsAcceptInvite"
        const val FUNCTION_GROUPS_CANCEL_INVITE = "groupsCancelInvite"
        const val FUNCTION_GROUPS_REJECT_INVITE = "groupsRejectInvite"
        const val FUNCTION_GROUPS_LEAVE = "groupsLeave"
    }

    // ------------------------- FRIENDS -------------------------

    data class FriendInviteResult(
        val friendUid: String?,
        val friendCode: String?,
        val displayName: String?
    )

    suspend fun sendFriendInviteByCode(code: String, clientCreatedAt: Long): FriendInviteResult {
        val payload = hashMapOf(
            "code" to code.trim().uppercase(),
            "clientCreatedAt" to clientCreatedAt
        )

        val res = functions
            .getHttpsCallable(FUNCTION_FRIENDS_SEND_INVITE_BY_CODE)
            .call(payload)
            .await()

        val data = res.data as? Map<*, *> ?: emptyMap<String, Any?>()

        return FriendInviteResult(
            friendUid = data["friendUid"] as? String,
            friendCode = data["friendCode"] as? String,
            displayName = data["displayName"] as? String
        )
    }

    suspend fun acceptFriend(friendUid: String) {
        functions.getHttpsCallable(FUNCTION_FRIENDS_ACCEPT)
            .call(hashMapOf("friendUid" to friendUid))
            .await()
    }

    suspend fun rejectFriend(friendUid: String) {
        functions.getHttpsCallable(FUNCTION_FRIENDS_REJECT)
            .call(hashMapOf("friendUid" to friendUid))
            .await()
    }

    suspend fun removeFriend(friendUid: String) {
        functions.getHttpsCallable(FUNCTION_FRIENDS_REMOVE)
            .call(hashMapOf("friendUid" to friendUid))
            .await()
    }

    suspend fun updateFriendNickname(friendUid: String, nickname: String?) {
        functions.getHttpsCallable(FUNCTION_FRIENDS_UPDATE_NICKNAME)
            .call(hashMapOf("friendUid" to friendUid, "nickname" to nickname))
            .await()
    }

    // ------------------------- GROUPS -------------------------

    data class CreateGroupResult(
        val groupId: String,
        val name: String,
        val now: Long,
        val membersCount: Int
    )

    suspend fun createGroup(name: String): CreateGroupResult {
        val res = functions.getHttpsCallable(FUNCTION_GROUPS_CREATE)
            .call(hashMapOf("name" to name.trim()))
            .await()

        val data = res.data as? Map<*, *> ?: error("Invalid response from $FUNCTION_GROUPS_CREATE")

        return CreateGroupResult(
            groupId = data["groupId"] as String,
            name = data["name"] as String,
            now = (data["now"] as Number).toLong(),
            membersCount = (data["membersCount"] as? Number)?.toInt() ?: 1
        )
    }

    data class GroupInviteResult(
        val inviteId: String,
        val groupId: String,
        val groupNameSnapshot: String,
        val fromUid: String,
        val toUid: String,
        val status: String,
        val createdAt: Long,
        val respondedAt: Long?
    )

    suspend fun inviteToGroup(
        groupId: String,
        groupNameSnapshot: String,
        toUid: String,
        clientCreatedAt: Long
    ): GroupInviteResult {
        val payload = hashMapOf(
            "groupId" to groupId,
            "groupNameSnapshot" to groupNameSnapshot,
            "toUid" to toUid,
            "clientCreatedAt" to clientCreatedAt
        )

        val res = functions.getHttpsCallable(FUNCTION_GROUPS_INVITE)
            .call(payload)
            .await()

        val data = res.data as? Map<*, *> ?: error("Invalid response from $FUNCTION_GROUPS_INVITE")

        return GroupInviteResult(
            inviteId = data["inviteId"] as String,
            groupId = data["groupId"] as String,
            groupNameSnapshot = data["groupNameSnapshot"] as String,
            fromUid = data["fromUid"] as String,
            toUid = data["toUid"] as String,
            status = data["status"] as String,
            createdAt = (data["createdAt"] as Number).toLong(),
            respondedAt = (data["respondedAt"] as? Number)?.toLong()
        )
    }

    suspend fun acceptGroupInvite(inviteId: String) {
        functions.getHttpsCallable(FUNCTION_GROUPS_ACCEPT_INVITE)
            .call(hashMapOf("inviteId" to inviteId))
            .await()
    }

    suspend fun cancelGroupInvite(inviteId: String) {
        functions.getHttpsCallable(FUNCTION_GROUPS_CANCEL_INVITE)
            .call(hashMapOf("inviteId" to inviteId))
            .await()
    }

    suspend fun rejectGroupInvite(inviteId: String) {
        functions.getHttpsCallable(FUNCTION_GROUPS_REJECT_INVITE)
            .call(hashMapOf("inviteId" to inviteId))
            .await()
    }

    suspend fun leaveGroup(groupId: String) {
        functions.getHttpsCallable(FUNCTION_GROUPS_LEAVE)
            .call(hashMapOf("groupId" to groupId))
            .await()
    }
}