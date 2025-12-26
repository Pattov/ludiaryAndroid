package com.ludiary.android.data.repository

/**
 * Clase de datos que representa el resultado de la sincronización de sesiones.
 * @property adopted Número de sesiones adoptadas.
 * @property pushed Número de sesiones subidas.
 * @property pulledPersonal Número de sesiones descargadas para el usuario personal.
 * @property pulledGroups Número de sesiones descargadas para grupos.
 */
data class SessionsSyncResult(
    val adopted: Int,
    val pushed: Int,
    val pulledPersonal: Int,
    val pulledGroups: Int
)

/**
 * Interfaz que define el repositorio de sesiones.
 */
interface SessionsRepository {

    /**
     * Obtiene todas las sesiones de un usuario personal.
     * @param uid Identificador único del usuario.
     */
    suspend fun sync(uid: String): SessionsSyncResult

    /**
     * Obtiene una partida por su identificador.
     * @param sessionId Identificador único de la partida.
     */
    suspend fun deleteSession(sessionId: String)
}