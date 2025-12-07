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
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroTecnicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Técnico"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sessionManager = SessionManager(this)

        binding.btnSalvarTecnico.setOnClickListener {
            salvarTecnico()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun salvarTecnico() {
        val nome = binding.etNomeTecnico.text.toString().trim()
        val funcao = binding.etFuncaoTecnico.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "Informe o nome do técnico.", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSalvarTecnico.isEnabled = false

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val bodyJson = JSONObject().apply {
                        put("name", nome)
                        put("role", if (funcao.isEmpty()) JSONObject.NULL else funcao)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = bodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/technicians")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val respText = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(
                            false,
                            "Erro ao salvar técnico: ${response.code} - $respText"
                        )
                    }

                    Pair(true, "Técnico salvo com sucesso.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (resultado.first) {
                Toast.makeText(this@CadastroTecnicoActivity, resultado.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@CadastroTecnicoActivity, resultado.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarTecnico.isEnabled = true
            }
        }
    }
}
