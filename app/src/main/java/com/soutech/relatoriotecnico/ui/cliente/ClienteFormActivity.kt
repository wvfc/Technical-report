package com.soutech.relatoriotecnico.ui.cliente

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityClienteFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ClienteFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteFormBinding
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Cliente"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sessionManager = SessionManager(this)

        binding.btnSalvarCliente.setOnClickListener {
            salvarCliente()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun salvarCliente() {
        val razaoSocial = binding.edRazaoSocial.text.toString().trim()
        val nomeFantasia = binding.etNomeCliente.text.toString().trim()
        val cnpj = binding.etCnpj.text.toString().trim()
        val endereco = binding.etEndereco.text.toString().trim()
        val email = binding.edEmail.text.toString().trim()
        val telefone = binding.edTelefone.text.toString().trim()
        val whatsapp = binding.edWhatsapp.text.toString().trim()

        // Nome fantasia obrigatório (pode ajustar se quiser exigir razão social também)
        if (nomeFantasia.isEmpty()) {
            Toast.makeText(this, "Informe o nome fantasia do cliente.", Toast.LENGTH_SHORT).show()
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

        // Monta um "endereço completo" com todos os dados adicionais,
        // aproveitando o campo address do backend sem precisar alterar o modelo.
        val enderecoCompleto = buildString {
            if (endereco.isNotEmpty()) append("Endereço: $endereco\n")
            if (razaoSocial.isNotEmpty()) append("Razão Social: $razaoSocial\n")
            if (email.isNotEmpty()) append("E-mail: $email\n")
            if (telefone.isNotEmpty()) append("Telefone: $telefone\n")
            if (whatsapp.isNotEmpty()) append("WhatsApp: $whatsapp\n")
        }.trim()

        binding.btnSalvarCliente.isEnabled = false

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val bodyJson = JSONObject()

                    // nomes EXATOS esperados pelo backend FastAPI
                    bodyJson.put("name", nomeFantasia)

                    // Evita Overload resolution ambiguity usando if separado
                    if (cnpj.isEmpty()) {
                        bodyJson.put("cnpj", JSONObject.NULL)
                    } else {
                        bodyJson.put("cnpj", cnpj)
                    }

                    if (enderecoCompleto.isEmpty()) {
                        bodyJson.put("address", JSONObject.NULL)
                    } else {
                        bodyJson.put("address", enderecoCompleto)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = bodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/clients")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val respText = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(
                            false,
                            "Erro ao salvar cliente: ${response.code} - $respText"
                        )
                    }

                    Pair(true, "Cliente salvo com sucesso.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (resultado.first) {
                Toast.makeText(this@ClienteFormActivity, resultado.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@ClienteFormActivity, resultado.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarCliente.isEnabled = true
            }
        }
    }
}
