package com.ludiary.android.util

import androidx.annotation.IdRes
import com.google.android.material.bottomnavigation.BottomNavigationView

fun BottomNavigationView.setBadgeCount(@IdRes itemId: Int, count: Int) {
    val badge = getOrCreateBadge(itemId)
    if (count <= 0) {
        removeBadge(itemId)
        return
    }
    badge.isVisible = true
    badge.number = count
    badge.maxCharacterCount = 3 // 999+
}