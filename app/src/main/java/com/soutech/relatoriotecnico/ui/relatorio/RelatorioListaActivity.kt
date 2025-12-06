package com.soutech.relatoriotecnico.ui.relatorio

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioListaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class ClienteFiltroDto(
    val id: Int?,
    val name: String
)

class RelatorioListaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioListaBinding
    private val client = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    private val listaClientes = mutableListOf<ClienteFiltroDto>()
    private val mapaClientes = mutableMapOf<Int, String>()
    private val listaRelatorios = mutableListOf<RelatorioDto>()
    private val mapaMaquinasSerial = mutableMapOf<Int, String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Histórico de Relatórios"

        sessionManager = SessionManager(this)

        binding.rvRelatorios.layoutManager = LinearLayoutManager(this)

        binding.btnAplicarFiltro.setOnClickListener {
            carregarRelatorios()
        }

        carregarClientesEMaquinasEReports()
    }

    private fun carregarClientesEMaquinasEReports() {
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
            // 1) Carregar clientes
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
                        listaClientes.clear()
                        mapaClientes.clear()

                        // opção "Todos"
                        listaClientes.add(ClienteFiltroDto(null, "Todos os clientes"))

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val id = obj.getInt("id")
                            val name = obj.optString("name", "Sem nome")
                            listaClientes.add(ClienteFiltroDto(id, name))
                            mapaClientes[id] = name
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2) popular spinner
            val nomes = listaClientes.map { it.name }
            val adapter = ArrayAdapter(
                this@RelatorioListaActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClienteFiltro.adapter = adapter

            // 3) Carregar relatórios
            carregarRelatorios()
        }
    }

    private fun carregarRelatorios() {
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

        val dataDe = binding.etDataDe.text.toString().trim()
        val dataAte = binding.etDataAte.text.toString().trim()
        val numeroSerie = binding.etNumeroSerieFiltro.text.toString().trim()

        val posCliente = binding.spinnerClienteFiltro.selectedItemPosition
        val clienteIdSelecionado = listaClientes.getOrNull(posCliente)?.id

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val urlBase = "${ApiConfig.BASE_URL}/api/reports"
                    val params = mutableListOf<String>()

                    if (dataDe.isNotEmpty()) params.add("date_from=$dataDe")
                    if (dataAte.isNotEmpty()) params.add("date_to=$dataAte")
                    if (clienteIdSelecionado != null) params.add("client_id=$clienteIdSelecionado")
                    if (numeroSerie.isNotEmpty()) params.add("serial_number=$numeroSerie")

                    val query = if (params.isNotEmpty()) {
                        "?" + params.joinToString("&")
                    } else {
                        ""
                    }

                    val url = urlBase + query

                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = client.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro: ${response.code} - $bodyStr")
                    }

                    val arr = JSONArray(bodyStr)
                    listaRelatorios.clear()

                    // também puxar máquinas uma vez (opcional) se quiser o nº de série consistente
                    // mas como o backend já aceita filtro por serial, aqui vamos depender de pdf_url/serial em content no futuro
                    // por enquanto, deixamos número de série vazio, a não ser que venha no próprio JSON

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val rel = parseRelatorio(obj)
                        listaRelatorios.add(rel)
                    }

                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!result.first) {
                Toast.makeText(this@RelatorioListaActivity, result.second, Toast.LENGTH_LONG).show()
            }

            binding.rvRelatorios.adapter = RelatorioAdapter(this@RelatorioListaActivity, listaRelatorios.toList())
        }
    }

    private fun parseRelatorio(obj: JSONObject): RelatorioDto {
        val id = obj.getInt("id")
        val type = obj.optString("type", "")
        val clientId = obj.getInt("client_id")
        val machineId = if (obj.isNull("machine_id")) null else obj.getInt("machine_id")
        val title = obj.optString("title", "")
        val dateIso = obj.optString("date", "")
        val pdfUrl = if (obj.isNull("pdf_url")) null else obj.getString("pdf_url")

       val clientName = if (obj.isNull("client_name")) null else obj.getString("client_name")
       val serialNumber = if (obj.isNull("serial_number")) null else obj.getString("serial_number")

       return RelatorioDto(
            id = id,
            type = type,
            clientId = clientId,
            clientName = clientName,
            machineId = machineId,
            serialNumber = serialNumber,
            title = title,
            dateIso = dateIso,
            pdfUrl = pdfUrl
      )
   }

}
