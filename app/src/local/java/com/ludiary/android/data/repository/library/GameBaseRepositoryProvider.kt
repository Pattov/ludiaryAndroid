package com.ludiary.android.data.repository.library

object GameBaseRepositoryProvider {
    fun provide(): GameBaseRepository =
        GameBaseRepositoryLocal()
}