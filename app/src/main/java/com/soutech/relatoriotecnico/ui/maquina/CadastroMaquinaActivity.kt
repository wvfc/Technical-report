package com.soutech.relatoriotecnico.ui.maquina

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityCadastroMaquinaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// DTO simples para o spinner de clientes
data class ClienteRemotoMaquina(
    val id: Int,
    val nomeFantasia: String
)

class CadastroMaquinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroMaquinaBinding
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    private var clientes: List<ClienteRemotoMaquina> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroMaquinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Máquina"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sessionManager = SessionManager(this)

        // Carregar clientes para o spinner
        carregarClientes()

        binding.btnSalvarMaquina.setOnClickListener {
            salvarMaquina()
        }
        // NÃO usamos btnVoltar aqui, já que o layout não tem esse botão.
        // O usuário pode voltar pelo botão do Android ou pelo ícone da ActionBar.
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ==================== CARREGAR CLIENTES (API) ====================

    private fun carregarClientes() {
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

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/clients")
                        .get()
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro ao carregar clientes: ${response.code}")
                    }

                    val arr = JSONArray(bodyStr)
                    val lista = mutableListOf<ClienteRemotoMaquina>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getInt("id")
                        val nome = obj.optString("name", "Sem nome")
                        lista.add(ClienteRemotoMaquina(id, nome))
                    }
                    clientes = lista
                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!resultado.first) {
                Toast.makeText(this@CadastroMaquinaActivity, resultado.second, Toast.LENGTH_LONG).show()
            }

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@CadastroMaquinaActivity,
                    "Cadastre um cliente antes de cadastrar máquinas.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val nomes = clientes.map { it.nomeFantasia }
                val adapter = ArrayAdapter(
                    this@CadastroMaquinaActivity,
                    android.R.layout.simple_spinner_item,
                    nomes
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCliente.adapter = adapter
            }
        }
    }

    // ==================== SALVAR MÁQUINA (API) ====================

    private fun salvarMaquina() {
        if (clientes.isEmpty()) {
            Toast.makeText(this, "Cadastre pelo menos um cliente primeiro.", Toast.LENGTH_SHORT).show()
            return
        }

        val idxCliente = binding.spinnerCliente.selectedItemPosition
        if (idxCliente < 0) {
            Toast.makeText(this, "Selecione um cliente.", Toast.LENGTH_SHORT).show()
            return
        }
        val cliente = clientes[idxCliente]

        val marca = binding.etMarca.text.toString().trim()
        val modelo = binding.etModelo.text.toString().trim()
        val modeloIhm = binding.etModeloIhm.text.toString().trim()
        val numeroSerie = binding.etNumeroSerie.text.toString().trim()
        val fotoPlaqueta = binding.etFotoPlaqueta.text.toString().trim()
        val fotoCompressor = binding.etFotoCompressor.text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || numeroSerie.isEmpty()) {
            Toast.makeText(
                this,
                "Informe pelo menos marca, modelo e número de série.",
                Toast.LENGTH_SHORT
            ).show()
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

        binding.btnSalvarMaquina.isEnabled = false

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    val bodyJson = JSONObject().apply {
                        // Nomes EXATOS usados no backend (MachineCreate)
                        put("client_id", cliente.id)
                        put("brand", marca)
                        put("model", modelo)
                        put("ihm_model", if (modeloIhm.isEmpty()) JSONObject.NULL else modeloIhm)
                        put("serial_number", numeroSerie)
                        put("plate_photo_url", if (fotoPlaqueta.isEmpty()) JSONObject.NULL else fotoPlaqueta)
                        put("compressor_photo_url", if (fotoCompressor.isEmpty()) JSONObject.NULL else fotoCompressor)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = bodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/machines")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val respText = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(
                            false,
                            "Erro ao salvar máquina: ${response.code} - $respText"
                        )
                    }

                    Pair(true, "Máquina salva com sucesso.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (resultado.first) {
                Toast.makeText(this@CadastroMaquinaActivity, resultado.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@CadastroMaquinaActivity, resultado.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarMaquina.isEnabled = true
            }
        }
    }
}
