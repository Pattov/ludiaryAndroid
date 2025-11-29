package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.model.UserGame
import com.ludiary.android.data.repository.UserGamesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EditUserGameViewModel (
    private val uid: String,
    private val repository: UserGamesRepository
) : ViewModel() {

    private val _events = Channel<EditUserGameEvent>()
    val events = _events.receiveAsFlow()

    fun onSaveClicked(
        gameId: String?,
        title: String,
        rating: Float?,
        language: String,
        edition: String,
        notes: String
    ){
        if (title.isBlank()){
            sendEvent(EditUserGameEvent.ShowError("El título es obligatorio"))
            return
        }

        if (gameId == null) {

            val newGame = UserGame(
                id = "",
                userId = uid,
                gameId = "",
                isCustom = true,
                titleSnapshot = title,
                personalRating = rating,
                language = language.ifBlank { null },
                edition = edition.ifBlank { null },
                notes = notes.ifBlank { null }
            )

            viewModelScope.launch {
                repository.addUserGame(uid, newGame)
                sendEvent(EditUserGameEvent.CloseScreen("Juego guardado"))
            }

        } else {

            val updated = UserGame(
                id = gameId,
                userId = uid,
                gameId = "",
                isCustom = true,
                titleSnapshot = title,
                personalRating = rating,
                language = language.ifBlank { null },
                edition = edition.ifBlank { null },
                notes = notes.ifBlank { null }
            )

            viewModelScope.launch {
                repository.updateUserGame(uid, updated)
                sendEvent(EditUserGameEvent.CloseScreen("Juego actualizado"))
            }
        }
    }

    private fun sendEvent(event: EditUserGameEvent){
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

sealed class EditUserGameEvent {
    data class ShowError(val message: String) : EditUserGameEvent()
    data class CloseScreen(val message: String) : EditUserGameEvent()
}