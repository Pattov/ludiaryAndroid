package com.ludiary.android.data.repository.sessions

import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.data.local.entity.SessionPlayerEntity

data class RemoteAppliedSession(
    val id: String,
    val isDeleted: Boolean,
    val updatedAtMillis: Long?,
    val sessionEntity: SessionEntity,
    val playerEntities: List<SessionPlayerEntity>
)