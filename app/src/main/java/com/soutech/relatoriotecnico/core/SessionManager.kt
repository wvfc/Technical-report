package com.soutech.relatoriotecnico.core

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOME = "nome"
        private const val KEY_EMAIL = "email"
        private const val KEY_CONFIGURADO = "configurado"
    }

    fun salvarPerfil(nome: String, email: String) {
        prefs.edit()
            .putString(KEY_NOME, nome)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_CONFIGURADO, true)
            .apply()
    }

    fun limparPerfil() {
        prefs.edit().clear().apply()
    }

    fun getNome(): String? = prefs.getString(KEY_NOME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isConfigurado(): Boolean = prefs.getBoolean(KEY_CONFIGURADO, false)
}
