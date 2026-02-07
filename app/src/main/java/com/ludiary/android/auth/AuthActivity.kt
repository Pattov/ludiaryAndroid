package com.ludiary.android.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.ui.main.MainActivity

/**
 * Actividad principal del modulo de autenticación.
 *
 * Sirve como punto de entrada al flujo de Ludiary.
 * Carga el layout activity_auth.xml y define con el identificador `@id/authContainer``.
 */
class AuthActivity: AppCompatActivity() {

    /**
     * Inicializa la actividad y muestra el fragmento de login por defecto.
     *
     * @param savedInstanceState El estado de la instancia, si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay sesión, no se enseña login
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Establece el layout principal de la actividad de autenticación.
        setContentView(R.layout.activity_auth)

        // Carga el fragmento de login solo la primera vez que se crea la actividad.
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.authContainer, LoginFragment())
                .commit()
        }
    }
}