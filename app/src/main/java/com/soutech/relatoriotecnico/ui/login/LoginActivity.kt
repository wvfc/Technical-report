package com.soutech.relatoriotecnico.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.soutech.relatoriotecnico.R
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.ui.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // Se já configurado, vai direto para o aplicativo
        if (sessionManager.isConfigurado()) {
            abrirMain()
            return
        }

        mostrarTelaCadastro()
    }

    private fun mostrarTelaCadastro() {
        setContentView(R.layout.activity_login)

        val etNome = findViewById<EditText>(R.id.etEmail)
        val etEmail = findViewById<EditText>(R.id.etPassword)
        val btnEntrar = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Personaliza as dicas dos campos para o cadastro inicial
        etNome.hint = "Seu nome"
        etEmail.hint = "Seu e-mail (opcional)"
        btnEntrar.text = "Começar"
        tvStatus.text = "Bem-vindo! Informe seu nome para começar."

        btnEntrar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (nome.isEmpty()) {
                tvStatus.text = "Informe seu nome para continuar."
                return@setOnClickListener
            }

            sessionManager.salvarPerfil(nome, email)
            abrirMain()
        }
    }

    private fun abrirMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
