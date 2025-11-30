package com.soutech.relatoriotecnico.core


import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_AUTHORIZED = "is_authorized"
    }

    fun saveSession(email: String, token: String) {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_IS_AUTHORIZED, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun isAuthorized(): Boolean = prefs.getBoolean(KEY_IS_AUTHORIZED, false)
}
