package com.ludiary.android.data.repository.notification

/**
 * Contrato para gestionar tokens de Firebase Cloud Messaging (FCM) asociados a un usuario.
 */
interface FcmTokensRepository {

    /**
     * Inserta o actualiza (upsert) el token FCM del dispositivo para el usuario indicado.
     * @param uid UID del usuario propietario del token.
     * @param token Token FCM del dispositivo (normalmente `trim()` y no vacío).
     */
    suspend fun upsertToken(uid: String, token: String)

    /**
     * Elimina el token FCM del dispositivo para el usuario indicado.
     * @param uid UID del usuario propietario del token.
     * @param token Token FCM a eliminar (normalmente el token actual del dispositivo).
     */
    suspend fun deleteToken(uid: String, token: String)
}
