package com.ludiary.android.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Clase de utilidad para el manejo de idiomas.
 */
object LocaleManager {

    /**
     * Aplica el idioma seleccionado a un contexto.
     * @param context Contexto en el que aplicar el idioma.
     * @param lang Idioma a aplicar.
     */
    fun applyLanguage(context: Context, lang: String?): Context {
        if (lang == null || lang.isEmpty()) {
            return context
        }

        val locale = Locale.forLanguageTag(lang)

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}