package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.model.UserGame
import com.ludiary.android.data.repository.UserGamesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de creación y edición de juegos del usuario.
 */
class EditUserGameViewModel (
    private val uid: String,
    private val repository: UserGamesRepository
) : ViewModel() {

    /**
     * Eventos emitidos por el ViewModel.
     */
    private val _events = Channel<EditUserGameEvent>()
    val events = _events.receiveAsFlow()

    /**
     * Carga los datos de un juego.
     * Si el juego no existe, muestra un error.
     * @param gameId Identificador único del juego.
     */
    fun loadGame(gameId: String){
        viewModelScope.launch {
            // Obtener la lista actual de juegos (solo la primera emisión)
            val games = repository.getUserGames(uid).first()
            val game = games.find { it.id == gameId }

            if (game != null){
                sendEvent(EditUserGameEvent.FillForm(game))
            } else {
                sendEvent(EditUserGameEvent.ShowError("Juego no encontrado"))
            }
        }
    }

    /**
     * Acción principal del formulario: guardar o actualizar un juego.
     *
     * @param gameId Identificador único del juego.
     * @param title Título del juego.
     * @param rating Rating del juego.
     * @param language Idioma del juego.
     * @param edition Edición del juego.
     * @param notes Notas del juego.
     */
    fun onSaveClicked(
        gameId: String?,
        title: String,
        rating: Float?,
        language: String,
        edition: String,
        notes: String
    ){
        // Validación básica
        if (title.isBlank()){
            sendEvent(EditUserGameEvent.ShowError("El título es obligatorio"))
            return
        }

        if (gameId == null) {
            //Crear nuevo juego
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

            //Actualizar juego existente
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

    /**
     * Envía un evento hacia la UI de forma segura.
     */
    private fun sendEvent(event: EditUserGameEvent){
        viewModelScope.launch {
            _events.send(event)
        }
    }
}

/**
 * Eventos usados por [EditUserGameViewModel] para comunicarse con la UI.
 *
 * Representan acciones puntuales, no estado continuo.
 */
sealed class EditUserGameEvent {

    /** Muestra un mensaje de error (validación o fallo en Firestore). */
    data class ShowError(val message: String) : EditUserGameEvent()

    /** Cierra la pantalla actual y muestra un mensaje de confirmación. */
    data class CloseScreen(val message: String) : EditUserGameEvent()

    /** Rellena el formulario con los datos del juego cargado. */
    data class FillForm (val game: UserGame) : EditUserGameEvent()
}