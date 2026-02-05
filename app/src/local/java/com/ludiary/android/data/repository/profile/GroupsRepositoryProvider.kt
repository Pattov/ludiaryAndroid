package com.ludiary.android.data.repository.profile

import android.content.Context
import com.ludiary.android.data.local.LocalGroupsDataSource

object GroupsRepositoryProvider {
    fun provide(
        context: Context,
        local: LocalGroupsDataSource
    ): GroupsRepository = GroupsRepositoryLocal(local)
}
