package com.soutech.relatoriotecnico.ui.maquina

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

data class ClienteRemotoMaquina(
    val id: Int,
    val nomeFantasia: String
)

class CadastroMaquinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroMaquinaBinding
    private lateinit var sessionManager: SessionManager
    private val httpClient = OkHttpClient()

    private var clientes: List<ClienteRemotoMaquina> = emptyList()
    private var clienteSelecionadoId: Int? = null

    private var plaquetaUri: Uri? = null
    private var compressorUri: Uri? = null

    private val pickerPlaqueta = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            plaquetaUri = uri
            binding.btnFotoPlaqueta.text = "Plaqueta selecionada"
        }
    }

    private val pickerCompressor = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            compressorUri = uri
            binding.btnFotoCompressor.text = "Foto do compressor selecionada"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroMaquinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastrar Máquina"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sessionManager = SessionManager(this)

        carregarClientes()

        binding.btnFotoPlaqueta.setOnClickListener {
            pickerPlaqueta.launch(arrayOf("image/*"))
        }

        binding.btnFotoCompressor.setOnClickListener {
            pickerCompressor.launch(arrayOf("image/*"))
        }

        binding.btnSalvarMaquina.setOnClickListener {
            salvarMaquina()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // -------------------------------------------------------------------------
    // Carregar clientes do servidor para o spinner
    // -------------------------------------------------------------------------

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
                    val url = "${ApiConfig.BASE_URL}/api/clients"
                    val request = Request.Builder()
                        .url(url)
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
                return@launch
            }

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@CadastroMaquinaActivity,
                    "Cadastre um cliente antes de cadastrar máquinas.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val nomes = clientes.map { it.nomeFantasia }

            val adapter = android.widget.ArrayAdapter(
                this@CadastroMaquinaActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.spClienteMaquina.adapter = adapter
            binding.spClienteMaquina.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    clienteSelecionadoId = clientes[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    clienteSelecionadoId = null
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Salvar máquina no servidor
    // -------------------------------------------------------------------------

    private fun salvarMaquina() {
        if (clientes.isEmpty()) {
            Toast.makeText(this, "Cadastre pelo menos um cliente.", Toast.LENGTH_SHORT).show()
            return
        }

        val marca = binding.edMarcaFabricante.text.toString().trim()
        val modelo = binding.edModeloMaquina.text.toString().trim()
        val modeloIhm = binding.edModeloIhm.text.toString().trim()
        val numeroSerie = binding.edNumeroSerie.text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || clienteSelecionadoId == null) {
            Toast.makeText(
                this,
                "Informe cliente, marca e modelo da máquina.",
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
                    val json = JSONObject().apply {
                        put("client_id", clienteSelecionadoId ?: JSONObject.NULL)
                        put("brand", marca)
                        put("model", modelo)
                        put("ihm_model",
                            if (modeloIhm.isNotBlank()) modeloIhm else JSONObject.NULL
                        )
                        put("serial_number",
                            if (numeroSerie.isNotBlank()) numeroSerie else JSONObject.NULL
                        )
                        put(
                            "plaqueta_uri",
                            plaquetaUri?.toString() ?: JSONObject.NULL
                        )
                        put(
                            "compressor_uri",
                            compressorUri?.toString() ?: JSONObject.NULL
                        )
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/machines")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code} - $bodyStr")
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
