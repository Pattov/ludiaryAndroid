package com.ludiary.android.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ludiary.android.R

class AuthActivity: AppCompatActivity() {

    //Punto de entrada de la pantalla de autenticación
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Muestra el layout de activity_auth.xml
        setContentView(R.layout.activity_auth)

        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.authContainer, LoginFragment())
                .commit()
        }
    }
}