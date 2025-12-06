package com.soutech.relatoriotecnico.ui.relatorio

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.R
import com.soutech.relatoriotecnico.core.ApiConfig
import com.soutech.relatoriotecnico.core.NetworkUtils
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioCompressorFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


// DTOs simples para clientes e técnicos vindos da API
data class ClienteRemoto(
    val id: Int,
    val nomeFantasia: String
)

data class TecnicoRemoto(
    val id: Int,
    val nome: String
)

class RelatorioCompressorFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioCompressorFormBinding
    private val sdfDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // HTTP / sessão
    private val httpClient = OkHttpClient()
    private lateinit var sessionManager: SessionManager

    // Listas vindas do servidor
    private var clientes: List<ClienteRemoto> = emptyList()
    private var tecnicos: List<TecnicoRemoto> = emptyList()
    private var tecnicoSelecionadoId: Int? = null

    // Seleção de imagens (por enquanto só salvamos as URIs localmente; depois podemos mandar pro servidor)
    private val imagensUris: MutableList<Uri> = mutableListOf()

    private val pickerImagens = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            imagensUris.clear()
            imagensUris.addAll(uris)
            Toast.makeText(
                this,
                "${uris.size} imagem(ns) selecionada(s).",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioCompressorFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Relatório – Compressor"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sessionManager = SessionManager(this)

        configurarSpinnersFixos()
        carregarClientes()
        carregarTecnicos()

        binding.edDataEntrada.setOnClickListener { escolherDataHora(binding.edDataEntrada) }
        binding.edDataSaida.setOnClickListener { escolherDataHora(binding.edDataSaida) }

        binding.btnSelecionarImagens.setOnClickListener {
            pickerImagens.launch(arrayOf("image/*"))
        }

        binding.btnSalvarRelatorio.setOnClickListener {
            salvarRelatorio()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // =========================================================================
    //  SPINNERS: tipo de manutenção, status dos itens (fixos) e técnico (via API)
    // =========================================================================

    private fun configurarSpinnersFixos() {
        // Tipo de manutenção
        val tipos = listOf("Preventiva", "Preditiva", "Corretiva", "Inspeção")
        val adapterTipo = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            tipos
        )
        adapterTipo.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        binding.spTipoManutencao.adapter = adapterTipo

        // Status dos itens do checklist
        val statusOpcoes = listOf(
            "Ok",
            "Reparado / limpo nesta visita",
            "Necessita reparo / limpeza"
        )

        val adapterStatus = ArrayAdapter(
            this,
            R.layout.spinner_item_dark,
            statusOpcoes
        )
        adapterStatus.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)

        fun aplicaStatus(spinner: Spinner) {
            spinner.adapter = adapterStatus
        }

        // Aplicar em todos os itens 1–42
        aplicaStatus(binding.spItem1Status)
        aplicaStatus(binding.spItem2Status)
        aplicaStatus(binding.spItem3Status)
        aplicaStatus(binding.spItem4Status)
        aplicaStatus(binding.spItem5Status)
        aplicaStatus(binding.spItem6Status)
        aplicaStatus(binding.spItem7Status)
        aplicaStatus(binding.spItem8Status)
        aplicaStatus(binding.spItem9Status)
        aplicaStatus(binding.spItem10Status)
        aplicaStatus(binding.spItem11Status)
        aplicaStatus(binding.spItem12Status)
        aplicaStatus(binding.spItem13Status)
        aplicaStatus(binding.spItem14Status)
        aplicaStatus(binding.spItem15Status)
        aplicaStatus(binding.spItem16Status)
        aplicaStatus(binding.spItem17Status)
        aplicaStatus(binding.spItem18Status)
        aplicaStatus(binding.spItem19Status)
        aplicaStatus(binding.spItem20Status)
        aplicaStatus(binding.spItem21Status)
        aplicaStatus(binding.spItem22Status)
        aplicaStatus(binding.spItem23Status)
        aplicaStatus(binding.spItem24Status)
        aplicaStatus(binding.spItem25Status)
        aplicaStatus(binding.spItem26Status)
        aplicaStatus(binding.spItem27Status)
        aplicaStatus(binding.spItem28Status)
        aplicaStatus(binding.spItem29Status)
        aplicaStatus(binding.spItem30Status)
        aplicaStatus(binding.spItem31Status)
        aplicaStatus(binding.spItem32Status)
        aplicaStatus(binding.spItem33Status)
        aplicaStatus(binding.spItem34Status)
        aplicaStatus(binding.spItem35Status)
        aplicaStatus(binding.spItem36Status)
        aplicaStatus(binding.spItem37Status)
        aplicaStatus(binding.spItem38Status)
        aplicaStatus(binding.spItem39Status)
        aplicaStatus(binding.spItem40Status)
        aplicaStatus(binding.spItem41Status)
        aplicaStatus(binding.spItem42Status)
    }

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
                    val lista = mutableListOf<ClienteRemoto>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getInt("id")
                        val nome = obj.optString("name", "Sem nome")
                        lista.add(ClienteRemoto(id, nome))
                    }
                    clientes = lista
                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!resultado.first) {
                Toast.makeText(this@RelatorioCompressorFormActivity, resultado.second, Toast.LENGTH_LONG).show()
            }

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@RelatorioCompressorFormActivity,
                    "Cadastre um cliente no sistema antes de criar relatórios.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val nomes = clientes.map { it.nomeFantasia }
                val adapter = ArrayAdapter(
                    this@RelatorioCompressorFormActivity,
                    R.layout.spinner_item_dark,
                    nomes
                )
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
                binding.spCliente.adapter = adapter
            }
        }
    }

    private fun carregarTecnicos() {
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
                    val url = "${ApiConfig.BASE_URL}/api/technicians"
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("X-Auth-Token", token)
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro ao carregar técnicos: ${response.code}")
                    }

                    val arr = JSONArray(bodyStr)
                    val lista = mutableListOf<TecnicoRemoto>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getInt("id")
                        val nome = obj.optString("name", "Sem nome")
                        lista.add(TecnicoRemoto(id, nome))
                    }
                    tecnicos = lista
                    Pair(true, "OK")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (!resultado.first) {
                Toast.makeText(this@RelatorioCompressorFormActivity, resultado.second, Toast.LENGTH_LONG).show()
            }

            if (tecnicos.isEmpty()) {
                Toast.makeText(
                    this@RelatorioCompressorFormActivity,
                    "Nenhum técnico cadastrado. Cadastre um técnico primeiro.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val nomes = tecnicos.map { it.nome }
                val adapter = ArrayAdapter(
                    this@RelatorioCompressorFormActivity,
                    R.layout.spinner_item_dark,
                    nomes
                )
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
                binding.spinnerTecnico.adapter = adapter

                binding.spinnerTecnico.setOnItemSelectedListener { _, _, position, _ ->
                    tecnicoSelecionadoId = tecnicos[position].id
                }
            }
        }
    }

    // Helper pra Spinner (listener simplificado)
    // Helper pra Spinner (listener simplificado) – SEM genérico
private fun Spinner.setOnItemSelectedListener(
    onItemSelected: (parent: AdapterView<*>, view: View?, position: Int, id: Long) -> Unit
) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: AdapterView<*>,
            view: View?,
            position: Int,
            id: Long
        ) {
            onItemSelected(parent, view, position, id)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
}

    // =========================================================================
    //  UTILITÁRIOS DE DATA/HORA
    // =========================================================================

    private fun escolherDataHora(campo: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        campo.setText(sdfDataHora.format(cal.time))
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // =========================================================================
    //  SALVAR RELATÓRIO (ENVIA PARA O SERVIDOR)
    // =========================================================================

    private fun salvarRelatorio() {
        if (clientes.isEmpty()) {
            Toast.makeText(this, "Cadastre pelo menos um cliente.", Toast.LENGTH_SHORT).show()
            return
        }

        val idxCliente = binding.spCliente.selectedItemPosition
        if (idxCliente < 0) {
            Toast.makeText(this, "Selecione um cliente.", Toast.LENGTH_SHORT).show()
            return
        }
        val cliente = clientes[idxCliente]

        val dataEntradaStr = binding.edDataEntrada.text.toString().trim()
        val dataSaidaStr = binding.edDataSaida.text.toString().trim()
        val modelo = binding.edModeloMaquina.text.toString().trim()
        val tipo = binding.spTipoManutencao.selectedItem?.toString() ?: ""
        val observacoes = binding.edObservacoes.text.toString().trim()

        if (dataEntradaStr.isEmpty() || dataSaidaStr.isEmpty() || modelo.isEmpty()) {
            Toast.makeText(
                this,
                "Preencha data de entrada, saída, modelo e cliente.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Ainda não estamos usando os timestamps no backend, mas podemos mandar dentro do content
        val checklistResumo = montarResumoChecklist()

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

        binding.btnSalvarRelatorio.isEnabled = false

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    // JSON com todos os dados detalhados do relatório
                    val contentJson = JSONObject().apply {
                        put("data_entrada", dataEntradaStr)
                        put("data_saida", dataSaidaStr)
                        put("modelo_maquina", modelo)
                        put("tipo_manutencao", tipo)
                        put("observacoes", observacoes)
                        put("checklist_resumo", checklistResumo)
                        // Se quiser, pode adicionar as URIs das imagens:
                        put("imagens_uris", JSONArray(imagensUris.map { it.toString() }))
                    }

                    val bodyJson = JSONObject().apply {
                        put("type", "compressor")
                        put("client_id", cliente.id)
                        put("machine_id", JSONObject.NULL)      // podemos ligar a uma máquina depois
                        put("technician_id", tecnicoSelecionadoId ?: JSONObject.NULL)
                        put("title", "Relatório compressor - $modelo")
                        put("content", contentJson.toString())
                        // pdf_url deixamos NULL por enquanto; quando gerarmos o PDF no servidor, ele atualiza.
                        put("pdf_url", JSONObject.NULL)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = bodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("${ApiConfig.BASE_URL}/api/reports")
                        .post(body)
                        .addHeader("X-Auth-Token", token)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val bodyStr = response.body?.string() ?: ""

                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Erro ao salvar relatório: ${response.code} - $bodyStr")
                    }

                    Pair(true, "Relatório de compressor salvo no servidor.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(false, "Erro: ${e.message}")
                }
            }

            if (resultado.first) {
                Toast.makeText(this@RelatorioCompressorFormActivity, resultado.second, Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this@RelatorioCompressorFormActivity, resultado.second, Toast.LENGTH_LONG).show()
                binding.btnSalvarRelatorio.isEnabled = true
            }
        }
    }

    // =========================================================================
    //  CHECKLIST 1–42 (mantido do seu código original)
    // =========================================================================

    private data class ChecklistItem(
        val numero: Int,
        val titulo: String,
        val statusSpinner: Spinner,
        val campos: List<EditText>
    )

    private fun montarResumoChecklist(): String {
        val itens = listOf(
            // ===== Inspeções gerais =====
            ChecklistItem(
                1,
                "Total de horas de funcionamento / em carga",
                binding.spItem1Status,
                listOf(binding.edItem1Valor)
            ),
            ChecklistItem(
                2,
                "Pressão de descarga do pacote (carga/alívio)",
                binding.spItem2Status,
                listOf(binding.edItem2Valor)
            ),
            ChecklistItem(
                3,
                "Temp. de descarga do pacote a plena carga (°F / °C)",
                binding.spItem3Status,
                listOf(binding.edItem3Valor)
            ),
            ChecklistItem(
                4,
                "Temp. de descarga da unidade compressora a plena carga (°F / °C)",
                binding.spItem4Status,
                listOf(binding.edItem4Valor)
            ),
            ChecklistItem(
                5,
                "Temp. injeção do óleo a plena carga (°F / °C)",
                binding.spItem5Status,
                listOf(binding.edItem5Valor)
            ),
            ChecklistItem(
                6,
                "Pressão do cárter em alívio (PSIG / BarG)",
                binding.spItem6Status,
                listOf(binding.edItem6Valor)
            ),
            ChecklistItem(
                7,
                "Vácuo na admissão em alívio (PSIG / BarG)",
                binding.spItem7Status,
                listOf(binding.edItem7Valor)
            ),
            ChecklistItem(
                8,
                "Condição do filtro de admissão",
                binding.spItem8Status,
                emptyList()
            ),
            ChecklistItem(
                9,
                "Última substituição do filtro de admissão (Data / Horas)",
                binding.spItem9Status,
                listOf(binding.edItem9Data, binding.edItem9Horas)
            ),
            ChecklistItem(
                10,
                "Verificar nível do óleo refrigerante",
                binding.spItem10Status,
                listOf(binding.edItem10Obs)
            ),
            ChecklistItem(
                11,
                "Inspecionar vazamentos de óleo",
                binding.spItem11Status,
                listOf(binding.edItem11Obs)
            ),
            ChecklistItem(
                12,
                "Substituição do filtro de óleo (2.000 h ou 1 ano)",
                binding.spItem12Status,
                listOf(binding.edItem12Obs)
            ),
            ChecklistItem(
                13,
                "Queda de pressão do separador de óleo da plena carga (PSIG / BarG)",
                binding.spItem13Status,
                listOf(binding.edItem13Valor)
            ),
            ChecklistItem(
                14,
                "Data da última substituição do elemento separador (Data / Horas)",
                binding.spItem14Status,
                listOf(binding.edItem14Data, binding.edItem14Horas)
            ),
            ChecklistItem(
                15,
                "Inspecionar e limpar o orifício e tela do descarregador",
                binding.spItem15Status,
                listOf(binding.edItem15Obs)
            ),
            ChecklistItem(
                16,
                "Inspecionar e limpar o respiro da caixa de engrenagens",
                binding.spItem16Status,
                listOf(binding.edItem16Obs)
            ),
            ChecklistItem(
                17,
                "Temperatura ambiente da instalação (°F / °C)",
                binding.spItem17Status,
                listOf(binding.edItem17Valor)
            ),
            ChecklistItem(
                18,
                "Temp. da válvula de controle termostático (°F / °C) / Abertura",
                binding.spItem18Status,
                emptyList()
            ),
            ChecklistItem(
                19,
                "Alinhamento da correia verificado e em boas condições (A / B / C)",
                binding.spItem19Status,
                emptyList()
            ),
            ChecklistItem(
                20,
                "Sistema da tensão da correia verificado",
                binding.spItem20Status,
                listOf(binding.edItem20Obs)
            ),
            ChecklistItem(
                21,
                "Inspecionar por vazamentos de ar",
                binding.spItem21Status,
                listOf(binding.edItem21Obs)
            ),
            ChecklistItem(
                22,
                "Inspecionar os núcleos de trocadores de calor",
                binding.spItem22Status,
                listOf(binding.edItem22Obs)
            ),
            ChecklistItem(
                23,
                "Inspecionar e limpar o dreno de condensado",
                binding.spItem23Status,
                listOf(binding.edItem23Obs)
            ),
            ChecklistItem(
                24,
                "Inspecionar o motor principal e o ventilador",
                binding.spItem24Status,
                listOf(binding.edItem24Obs)
            ),
            ChecklistItem(
                25,
                "Última lubrificação do motor principal (Data / Horas)",
                binding.spItem25Status,
                listOf(binding.edItem25Data, binding.edItem25Horas)
            ),
            ChecklistItem(
                26,
                "Última lubrificação do motor do ventilador (Data / Horas)",
                binding.spItem26Status,
                listOf(binding.edItem26Data, binding.edItem26Horas)
            ),
            ChecklistItem(
                27,
                "Válvula de segurança instalada e operacional",
                binding.spItem27Status,
                listOf(binding.edItem27Obs)
            ),
            ChecklistItem(
                28,
                "Tipo de óleo refrigerante",
                binding.spItem28Status,
                listOf(binding.edItem28Tipo)
            ),
            ChecklistItem(
                29,
                "Última troca do óleo refrigerante (Data / Horas)",
                binding.spItem29Status,
                listOf(binding.edItem29Data, binding.edItem29Horas)
            ),

            // ===== Inspeções elétricas =====
            ChecklistItem(
                30,
                "Tensão (plena carga) L1 / L2 / L3",
                binding.spItem30Status,
                listOf(binding.edItem30L1, binding.edItem30L2, binding.edItem30L3)
            ),
            ChecklistItem(
                31,
                "Tensão (sem carga) L1 / L2 / L3",
                binding.spItem31Status,
                listOf(binding.edItem31L1, binding.edItem31L2, binding.edItem31L3)
            ),
            ChecklistItem(
                32,
                "Corrente do motor (plena carga) T1 / T2 / T3",
                binding.spItem32Status,
                listOf(binding.edItem32T1, binding.edItem32T2, binding.edItem32T3)
            ),
            ChecklistItem(
                33,
                "Corrente do motor (sem carga) T1 / T2 / T3",
                binding.spItem33Status,
                listOf(binding.edItem33T1, binding.edItem33T2, binding.edItem33T3)
            ),
            ChecklistItem(
                34,
                "Queda de tensão através da chave de partida L1 / L2 / L3",
                binding.spItem34Status,
                listOf(binding.edItem34L1, binding.edItem34L2, binding.edItem34L3)
            ),
            ChecklistItem(
                35,
                "Corrente total do pacote (plena carga) L1 / L2 / L3",
                binding.spItem35Status,
                listOf(binding.edItem35L1, binding.edItem35L2, binding.edItem35L3)
            ),
            ChecklistItem(
                36,
                "Dados da placa de identificação do motor (HP/kW, RPM, V, A)",
                binding.spItem36Status,
                listOf(
                    binding.edItem36HpKw,
                    binding.edItem36Rpm,
                    binding.edItem36V,
                    binding.edItem36A
                )
            ),
            ChecklistItem(
                37,
                "Inspecionar os contatores",
                binding.spItem37Status,
                listOf(binding.edItem37Obs)
            ),
            ChecklistItem(
                38,
                "Verificar as conexões elétricas",
                binding.spItem38Status,
                listOf(binding.edItem38Obs)
            ),
            ChecklistItem(
                39,
                "Temp. operacional HAT (°C) (corte de alta temperatura)",
                binding.spItem39Status,
                listOf(binding.edItem39Valor)
            ),

            // ===== Secador de refrigeração =====
            ChecklistItem(
                40,
                "Ponto de orvalho (°C)",
                binding.spItem40Status,
                listOf(binding.edItem40PontoOrvalho)
            ),
            ChecklistItem(
                41,
                "Pré-filtro (Ref.)",
                binding.spItem41Status,
                listOf(binding.edItem41Ref)
            ),
            ChecklistItem(
                42,
                "Pós-filtro (Ref.)",
                binding.spItem42Status,
                listOf(binding.edItem42Ref)
            )
        )

        val sb = StringBuilder()
        for (item in itens) {
            val status = item.statusSpinner.selectedItem?.toString()
                ?.takeIf { it.isNotBlank() } ?: "-"

            val valores = item.campos
                .map { it.text.toString().trim() }
                .filter { it.isNotEmpty() }

            sb.append("${item.numero}. ${item.titulo} | Status: $status")
            if (valores.isNotEmpty()) {
                sb.append(" | Valores/Obs: ${valores.joinToString(" ; ")}")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
