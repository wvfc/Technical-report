package com.soutech.relatoriotecnico.ui.maquina

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityMaquinasCadastradasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class MaquinasCadastradasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaquinasCadastradasBinding
    private val client = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    private val listaMaquinas = mutableListOf<MaquinaDto>()
    private val mapaClientes = mutableMapOf<Int, String>()  // id -> nome

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaquinasCadastradasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Máquinas cadastradas"

        sessionManager = SessionManager(this)

        binding.rvMaquinas.layoutManager = LinearLayoutManager(this)

        binding.btnBuscar.setOnClickListener {
            carregarMaquinas()
        }

        // Carrega tudo na primeira abertura
        carregarClientesEMaquinas()
    }

    private fun carregarClientesEMaquinas() {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch {
            // 1) Carrega clientes
            withContext(Dispatchers.IO) {
                try {
                    val url = "${ApiConfig.BASE_URL}/api/clients"
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-Auth-Token", token)
                        .build()
                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val arr = JSONArray(bodyStr)
                        mapaClientes.clear()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.getInt("id")
                            val name = obj.optString("name", "Sem nome")
                            mapaClientes[id] = name
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2) Carrega máquinas
            carregarMaquinas()
        }
    }

    private fun carregarMaquinas() {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_LONG).show()
            return
        }

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sessão expirada. Faça login novamente.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val termoBusca = binding.etBusca.text.toString().trim()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val urlBase = "${ApiConfig.BASE_URL}/api/machines"
                    val url = if (termoBusca.isEmpty()) {
                        urlBase
                    } else {
                        "$urlBase?search=${java.net.URLEncoder.encode(termoBusca, "UTF-8")}"
                    }

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
                    listaMaquinas.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val maquina = parseMaquina(obj)
                        listaMaquinas.add(maquina)
                    }

                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!result.first) {
                Toast.makeText(
                    this@MaquinasCadastradasActivity,
                    result.second,
                    Toast.LENGTH_LONG
                ).show()
            }

            // Mostra mensagem se estiver vazio
            if (listaMaquinas.isEmpty()) {
                binding.rvMaquinas.visibility = View.GONE
                binding.tvVazio.visibility = View.VISIBLE
            } else {
                binding.rvMaquinas.visibility = View.VISIBLE
                binding.tvVazio.visibility = View.GONE
                binding.rvMaquinas.adapter = MaquinaAdapter(listaMaquinas.toList())
            }
        }
    }

    private fun parseMaquina(obj: JSONObject): MaquinaDto {
        val id = obj.getInt("id")
        val clientId = obj.getInt("client_id")
        val brand = obj.optString("brand", "")
        val model = obj.optString("model", "")
        val serial = obj.optString("serial_number", "")
        val clientName = mapaClientes[clientId]

        return MaquinaDto(
            id = id,
            clientId = clientId,
            brand = brand,
            model = model,
            serialNumber = serial,
            clientName = clientName
        )
    }
}
