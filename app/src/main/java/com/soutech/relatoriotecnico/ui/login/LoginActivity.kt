package com.soutech.relatoriotecnico.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.R
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val client = OkHttpClient()

    companion object {
    // sem /admin aqui!
        private const val BASE_URL = "http://192.168.4.29:8000"
   }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Se já está autorizado, tenta verificar online. Se offline, entra direto.
        if (sessionManager.isAuthorized()) {
            if (NetworkUtils.isOnline(this)) {
                lifecycleScope.launch {
                    val blocked = !checkRemoteStatus()
                    if (blocked) {
                        sessionManager.clearSession()
                        mostrarTelaLogin("Seu acesso foi bloqueado pelo administrador.")
                    } else {
                        abrirMain()
                    }
                }
            } else {
                abrirMain()
            }
        } else {
            mostrarTelaLogin()
        }
    }

    private fun mostrarTelaLogin(mensagem: String? = null) {
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        if (!mensagem.isNullOrEmpty()) {
            tvStatus.text = mensagem
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvStatus.text = "Informe e-mail e senha."
                return@setOnClickListener
            }

            if (!NetworkUtils.isOnline(this)) {
                tvStatus.text = "Sem conexão com a internet para efetuar o primeiro login."
                return@setOnClickListener
            }

            tvStatus.text = "Autenticando..."
            realizarLogin(email, password, tvStatus)
        }
    }

    private fun realizarLogin(email: String, password: String, tvStatus: TextView) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = JSONObject()
                    json.put("email", email)
                    json.put("password", password)

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("$BASE_URL/api/login")
                        .post(body)
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code}")
                    }

                    val obj = JSONObject(bodyStr)
                    val status = obj.optString("status", "")
                    val token = obj.optString("token", null)

                    when (status) {
                        "pending_approval" ->
                            Pair(false, "Cadastro realizado. Aguarde o administrador liberar seu acesso.")
                        "blocked" ->
                            Pair(false, "Seu acesso está bloqueado. Contate o administrador.")
                        "ok" -> {
                            if (!token.isNullOrEmpty()) {
                                sessionManager.saveSession(email, token)
                                Pair(true, "ok")
                            } else {
                                Pair(false, "Resposta inválida do servidor: token vazio.")
                            }
                        }
                        else -> Pair(false, "Status desconhecido: $status")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro de comunicação: ${e.message}")
                }
            }

            if (result.first) {
                tvStatus.text = ""
                abrirMain()
            } else {
                tvStatus.text = result.second
            }
        }
    }

    private suspend fun checkRemoteStatus(): Boolean {
        val token = sessionManager.getToken() ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/api/check_token?token=$token"
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string() ?: ""

                if (!response.isSuccessful) return@withContext false

                val obj = JSONObject(bodyStr)
                val status = obj.optString("status", "blocked")
                status == "ok"
            } catch (e: Exception) {
                // Em erro de comunicação, por segurança, considera como liberado
                true
            }
        }
    }

    private fun abrirMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
