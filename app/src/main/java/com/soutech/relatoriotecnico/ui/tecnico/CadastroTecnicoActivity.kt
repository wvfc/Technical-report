package com.soutech.relatoriotecnico.ui.tecnico

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityCadastroTecnicoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CadastroTecnicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroTecnicoBinding
    private val client = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroTecnicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastrar Técnico"

        sessionManager = SessionManager(this)

        binding.btnSalvarTecnico.setOnClickListener {
            salvarTecnico()
        }
    }

    private fun salvarTecnico() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val nome = binding.etNomeTecnico.text.toString().trim()
        val funcao = binding.etFuncaoTecnico.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "Informe o nome do técnico.", Toast.LENGTH_LONG).show()
            return
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSalvarTecnico.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = JSONObject().apply {
                        put("name", nome)
                        put("role", if (funcao.isEmpty()) JSONObject.NULL else funcao)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/technicians")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code} - $bodyStr")
                    }

                    Pair(true, "Técnico cadastrado com sucesso.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (result.first) {
                Toast.makeText(this@CadastroTecnicoActivity, result.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@CadastroTecnicoActivity, result.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarTecnico.isEnabled = true
            }
        }
    }
}
