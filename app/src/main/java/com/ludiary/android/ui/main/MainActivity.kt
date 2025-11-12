package com.ludiary.android.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ludiary.android.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtiene el NavHostFragment declarado en el layout principal
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostMain) as NavHostFragment

        // Recupera el NavController para manejar la navegación
        val navController = navHost.navController

        // Asocia el BottomNavigationView con el NavController.
        // Esto sincroniza los ítems del menú.
        findViewById<BottomNavigationView>(R.id.bnvMain).setupWithNavController(navController)
    }
}