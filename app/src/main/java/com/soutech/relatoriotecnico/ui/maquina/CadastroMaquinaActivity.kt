package com.soutech.relatoriotecnico.ui.maquina

import android.os.Bundle
import android.view.View
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ClienteDto(
    val id: Int,
    val name: String
)

class CadastroMaquinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroMaquinaBinding
    private val client = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    private val listaClientes = mutableListOf<ClienteDto>()
    private var clienteSelecionadoId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroMaquinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastrar Máquina"

        sessionManager = SessionManager(this)

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
        } else {
            carregarClientes()
        }

        binding.btnSalvarMaquina.setOnClickListener {
            salvarMaquina()
        }
    }

    private fun carregarClientes() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnSalvarMaquina.isEnabled = false
        binding.spinnerCliente.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = "${ApiConfig.BASE_URL}/api/clients"
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code}")
                    }

                    val arr = JSONArray(bodyStr)
                    listaClientes.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getInt("id")
                        val name = obj.optString("name", "Sem nome")
                        listaClientes.add(ClienteDto(id, name))
                    }

                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!result.first) {
                Toast.makeText(this@CadastroMaquinaActivity, result.second, Toast.LENGTH_LONG).show()
            } else {
                if (listaClientes.isEmpty()) {
                    Toast.makeText(
                        this@CadastroMaquinaActivity,
                        "Nenhum cliente cadastrado. Cadastre um cliente primeiro.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val nomes = listaClientes.map { it.name }
                    val adapter = ArrayAdapter(
                        this@CadastroMaquinaActivity,
                        android.R.layout.simple_spinner_item,
                        nomes
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerCliente.adapter = adapter

                    binding.spinnerCliente.setOnItemSelectedListener { _, _, position, _ ->
                        clienteSelecionadoId = listaClientes[position].id
                    }
                }
            }

            binding.btnSalvarMaquina.isEnabled = true
            binding.spinnerCliente.isEnabled = true
        }
    }

    // Pequena extensão para Spinner (ajuda a evitar código verboso de listener)
    private fun <T> android.widget.Spinner.setOnItemSelectedListener(
        onItemSelected: (parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) -> Unit
    ) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                onItemSelected(parent, view, position, id)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun salvarMaquina() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val clientId = clienteSelecionadoId
        if (clientId == null) {
            Toast.makeText(this, "Selecione um cliente.", Toast.LENGTH_LONG).show()
            return
        }

        val marca = binding.etMarca.text.toString().trim()
        val modelo = binding.etModelo.text.toString().trim()
        val modeloIhm = binding.etModeloIhm.text.toString().trim()
        val numeroSerie = binding.etNumeroSerie.text.toString().trim()
        val fotoPlaqueta = binding.etFotoPlaqueta.text.toString().trim()
        val fotoCompressor = binding.etFotoCompressor.text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || numeroSerie.isEmpty()) {
            Toast.makeText(this, "Preencha marca, modelo e número de série.", Toast.LENGTH_LONG).show()
            return
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSalvarMaquina.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = JSONObject().apply {
                        put("client_id", clientId)
                        put("brand", marca)
                        put("model", modelo)
                        put("ihm_model", if (modeloIhm.isEmpty()) JSONObject.NULL else modeloIhm)
                        put("serial_number", numeroSerie)
                        put("plate_photo_url", if (fotoPlaqueta.isEmpty()) JSONObject.NULL else fotoPlaqueta)
                        put("compressor_photo_url", if (fotoCompressor.isEmpty()) JSONObject.NULL else fotoCompressor)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/machines")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code} - $bodyStr")
                    }

                    Pair(true, "Máquina cadastrada com sucesso.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (result.first) {
                Toast.makeText(this@CadastroMaquinaActivity, result.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@CadastroMaquinaActivity, result.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarMaquina.isEnabled = true
            }
        }
    }
}
