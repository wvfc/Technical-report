package com.soutech.relatoriotecnico.ui.tecnico

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityTecnicosCadastradosBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class TecnicosCadastradosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTecnicosCadastradosBinding
    private val client = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    private val listaTecnicos = mutableListOf<TecnicoDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicosCadastradosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Técnicos cadastrados"

        sessionManager = SessionManager(this)

        binding.rvTecnicos.layoutManager = LinearLayoutManager(this)

        binding.btnBuscarTecnico.setOnClickListener {
            carregarTecnicos()
        }

        // carrega na abertura da tela
        carregarTecnicos()
    }

    private fun carregarTecnicos() {
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

        val termo = binding.etBuscaTecnico.text.toString().trim()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = "${ApiConfig.BASE_URL}/api/technicians"
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
                    listaTecnicos.clear()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val tecnico = parseTecnico(obj)
                        listaTecnicos.add(tecnico)
                    }

                    // Filtro local por nome
                    if (termo.isNotEmpty()) {
                        val filtrada = listaTecnicos.filter {
                            it.name.contains(termo, ignoreCase = true)
                        }
                        listaTecnicos.clear()
                        listaTecnicos.addAll(filtrada)
                    }

                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!result.first) {
                Toast.makeText(
                    this@TecnicosCadastradosActivity,
                    result.second,
                    Toast.LENGTH_LONG
                ).show()
            }

            // atualiza lista / mensagem de vazio
            if (listaTecnicos.isEmpty()) {
                binding.rvTecnicos.visibility = View.GONE
                binding.tvVazio.visibility = View.VISIBLE
            } else {
                binding.rvTecnicos.visibility = View.VISIBLE
                binding.tvVazio.visibility = View.GONE
                binding.rvTecnicos.adapter = TecnicoAdapter(listaTecnicos.toList())
            }
        }
    }

    private fun parseTecnico(obj: JSONObject): TecnicoDto {
        val id = obj.getInt("id")
        val name = obj.optString("name", "")
        val role = obj.optString("role", null)
        return TecnicoDto(id = id, name = name, role = role)
    }
}
