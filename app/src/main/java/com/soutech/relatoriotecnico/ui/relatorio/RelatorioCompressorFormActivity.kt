package com.soutech.relatoriotecnico.ui.relatorio

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.ImagemRelatorioEntity
import com.soutech.relatoriotecnico.data.RelatorioEntity
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioCompressorFormBinding
import com.soutech.relatoriotecnico.util.PdfUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RelatorioCompressorFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioCompressorFormBinding
    private var clientes: List<ClienteEntity> = emptyList()
    private val imagensUris: MutableList<Uri> = mutableListOf()
    private val sdfDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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

        configurarSpinnersTipoManutencao()
        configurarSpinnersChecklist()
        carregarClientes()

        binding.edDataEntrada.setOnClickListener { escolherDataHora(binding.edDataEntrada) }
        binding.edDataSaida.setOnClickListener { escolherDataHora(binding.edDataSaida) }

        binding.btnSelecionarImagens.setOnClickListener {
            pickerImagens.launch(arrayOf("image/*"))
        }

        binding.btnSalvarRelatorio.setOnClickListener {
            salvarRelatorio()
        }

        binding.btnVoltar.setOnClickListener { finish() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // --------- Tipo de manutenção ---------

    private fun configurarSpinnersTipoManutencao() {
        val tipos = listOf("Preventiva", "Preditiva", "Corretiva", "Inspeção")
        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoManutencao.adapter = adapterTipo
    }

    // --------- Spinners OK / Reparado / Necessita reparo ---------

    private fun configurarSpinnersChecklist() {
        val statusOpcoes = listOf(
            "OK",
            "Reparado / limpo nesta visita",
            "Necessita reparo / limpeza"
        )

        val adapterStatus = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOpcoes)
        adapterStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Inspeções gerais 1–29
        binding.spItem1Status.adapter = adapterStatus
        binding.spItem2Status.adapter = adapterStatus
        binding.spItem3Status.adapter = adapterStatus
        binding.spItem4Status.adapter = adapterStatus
        binding.spItem5Status.adapter = adapterStatus
        binding.spItem6Status.adapter = adapterStatus
        binding.spItem7Status.adapter = adapterStatus
        binding.spItem8Status.adapter = adapterStatus
        binding.spItem9Status.adapter = adapterStatus
        binding.spItem10Status.adapter = adapterStatus
        binding.spItem11Status.adapter = adapterStatus
        binding.spItem12Status.adapter = adapterStatus
        binding.spItem13Status.adapter = adapterStatus
        binding.spItem14Status.adapter = adapterStatus
        binding.spItem15Status.adapter = adapterStatus
        binding.spItem16Status.adapter = adapterStatus
        binding.spItem17Status.adapter = adapterStatus
        binding.spItem18Status.adapter = adapterStatus
        binding.spItem19Status.adapter = adapterStatus
        binding.spItem20Status.adapter = adapterStatus
        binding.spItem21Status.adapter = adapterStatus
        binding.spItem22Status.adapter = adapterStatus
        binding.spItem23Status.adapter = adapterStatus
        binding.spItem24Status.adapter = adapterStatus
        binding.spItem25Status.adapter = adapterStatus
        binding.spItem26Status.adapter = adapterStatus
        binding.spItem27Status.adapter = adapterStatus
        binding.spItem28Status.adapter = adapterStatus
        binding.spItem29Status.adapter = adapterStatus

        // Inspeções elétricas 30–39
        binding.spItem30Status.adapter = adapterStatus
        binding.spItem31Status.adapter = adapterStatus
        binding.spItem32Status.adapter = adapterStatus
        binding.spItem33Status.adapter = adapterStatus
        binding.spItem34Status.adapter = adapterStatus
        binding.spItem35Status.adapter = adapterStatus
        binding.spItem36Status.adapter = adapterStatus
        binding.spItem37Status.adapter = adapterStatus
        binding.spItem38Status.adapter = adapterStatus
        binding.spItem39Status.adapter = adapterStatus

        // Secador 40–42
        binding.spItem40Status.adapter = adapterStatus
        binding.spItem41Status.adapter = adapterStatus
        binding.spItem42Status.adapter = adapterStatus
    }

    // --------- Clientes / datas ---------

    private fun carregarClientes() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = db.clienteDao().listarTodos()
            val nomes = clientes.map { it.nomeFantasia }

            val adapterClientes = ArrayAdapter(
                this@RelatorioCompressorFormActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            adapterClientes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spCliente.adapter = adapterClientes

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@RelatorioCompressorFormActivity,
                    "Cadastre um cliente antes de criar relatórios.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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

    // --------- Salvar / PDF ---------

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
        val tipo = binding.spTipoManutencao.selectedItem.toString()
        val observacoes = binding.edObservacoes.text.toString().trim()

        if (dataEntradaStr.isEmpty() || dataSaidaStr.isEmpty() || modelo.isEmpty()) {
            Toast.makeText(this, "Preencha data, modelo e cliente.", Toast.LENGTH_SHORT).show()
            return
        }

        val dataEntrada = sdfDataHora.parse(dataEntradaStr)?.time ?: Date().time
        val dataSaida = sdfDataHora.parse(dataSaidaStr)?.time ?: Date().time

        val checklistResumo = montarResumoChecklist()

        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val relatorio = RelatorioEntity(
                id = 0,
                clienteId = cliente.id,
                dataEntrada = dataEntrada,
                dataSaida = dataSaida,
                modeloMaquina = modelo,
                tipoManutencao = "$tipo (Compressor)",
                ocorrencia = checklistResumo,
                solucaoProposta = observacoes,
                pecasTexto = null,
                pdfPath = null
            )

            val relId = db.relatorioDao().inserir(relatorio)

            val imagens = imagensUris.mapIndexed { idx, uri ->
                ImagemRelatorioEntity(
                    relatorioId = relId,
                    uri = uri.toString(),
                    ordem = idx
                )
            }
            if (imagens.isNotEmpty()) {
                db.imagemDao().inserirLista(imagens)
            }

            val clienteDb = db.clienteDao().buscarPorId(cliente.id)
            val imagensSalvas = db.imagemDao().listarPorRelatorio(relId)

            val pdfFile = PdfUtils.gerarPdfRelatorioCompressor(
                context = this@RelatorioCompressorFormActivity,
                relatorio = relatorio.copy(id = relId),
                cliente = clienteDb ?: cliente,
                imagens = imagensSalvas
            )

            db.relatorioDao().atualizar(
                relatorio.copy(
                    id = relId,
                    pdfPath = pdfFile.absolutePath
                )
            )

            Toast.makeText(
                this@RelatorioCompressorFormActivity,
                "Relatório de compressor salvo e PDF gerado.",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    // --------- Checklist ---------

    data class ChecklistItem(
        val numero: Int,
        val titulo: String,
        val statusSpinner: Spinner,
        val campos: List<EditText>
    )

    private fun montarResumoChecklist(): String {
    val sb = StringBuilder()

    fun linhaSimples(
        numero: Int,
        titulo: String,
        sp: Spinner,
        ed: EditText?
    ) {
        val status = sp.selectedItem?.toString() ?: "-"
        val valor = ed?.text?.toString()?.trim().orEmpty()
        sb.append(numero)
            .append(" - ")
            .append(titulo)
            .append(" | Status: ")
            .append(status)
        if (valor.isNotEmpty()) {
            sb.append(" | Valor/Obs: ").append(valor)
        }
        sb.append("\n")
    }

    fun linhaDupla(
        numero: Int,
        titulo: String,
        sp: Spinner,
        ed1: EditText,
        rotulo1: String,
        ed2: EditText,
        rotulo2: String
    ) {
        val status = sp.selectedItem?.toString() ?: "-"
        val v1 = ed1.text.toString().trim()
        val v2 = ed2.text.toString().trim()
        sb.append(numero)
            .append(" - ")
            .append(titulo)
            .append(" | Status: ")
            .append(status)
        if (v1.isNotEmpty()) sb.append(" | ").append(rotulo1).append(": ").append(v1)
        if (v2.isNotEmpty()) sb.append(" | ").append(rotulo2).append(": ").append(v2)
        sb.append("\n")
    }

    fun linhaTripla(
        numero: Int,
        titulo: String,
        sp: Spinner,
        ed1: EditText,
        rotulo1: String,
        ed2: EditText,
        rotulo2: String,
        ed3: EditText,
        rotulo3: String
    ) {
        val status = sp.selectedItem?.toString() ?: "-"
        val v1 = ed1.text.toString().trim()
        val v2 = ed2.text.toString().trim()
        val v3 = ed3.text.toString().trim()
        sb.append(numero)
            .append(" - ")
            .append(titulo)
            .append(" | Status: ")
            .append(status)
        if (v1.isNotEmpty()) sb.append(" | ").append(rotulo1).append(": ").append(v1)
        if (v2.isNotEmpty()) sb.append(" | ").append(rotulo2).append(": ").append(v2)
        if (v3.isNotEmpty()) sb.append(" | ").append(rotulo3).append(": ").append(v3)
        sb.append("\n")
    }

    fun linhaQuadrupla(
        numero: Int,
        titulo: String,
        sp: Spinner,
        ed1: EditText, r1: String,
        ed2: EditText, r2: String,
        ed3: EditText, r3: String,
        ed4: EditText, r4: String
    ) {
        val status = sp.selectedItem?.toString() ?: "-"
        val v1 = ed1.text.toString().trim()
        val v2 = ed2.text.toString().trim()
        val v3 = ed3.text.toString().trim()
        val v4 = ed4.text.toString().trim()
        sb.append(numero)
            .append(" - ")
            .append(titulo)
            .append(" | Status: ")
            .append(status)
        if (v1.isNotEmpty()) sb.append(" | ").append(r1).append(": ").append(v1)
        if (v2.isNotEmpty()) sb.append(" | ").append(r2).append(": ").append(v2)
        if (v3.isNotEmpty()) sb.append(" | ").append(r3).append(": ").append(v3)
        if (v4.isNotEmpty()) sb.append(" | ").append(r4).append(": ").append(v4)
        sb.append("\n")
    }

    // ===== Inspeções gerais =====
    linhaSimples(1,
        "Total de horas de funcionamento (em carga)",
        binding.spItem1Status, binding.edItem1Valor
    )

    linhaSimples(2,
        "Pressão de descarga do pacote (carga/alívio)",
        binding.spItem2Status, binding.edItem2Valor
    )

    linhaSimples(3,
        "Temp. de descarga do pacote a plena carga (°F / °C)",
        binding.spItem3Status, binding.edItem3Valor
    )

    linhaSimples(4,
        "Temp. de descarga da unidade compressora a plena carga (°F / °C)",
        binding.spItem4Status, binding.edItem4Valor
    )

    linhaSimples(5,
        "Temp. injeção do óleo a plena carga (°F / °C)",
        binding.spItem5Status, binding.edItem5Valor
    )

    linhaSimples(6,
        "Pressão do cárter em alívio (PSIG / BarG)",
        binding.spItem6Status, binding.edItem6Valor
    )

    linhaSimples(7,
        "Vácuo na admissão em alívio (PSIG / BarG)",
        binding.spItem7Status, binding.edItem7Valor
    )

    linhaSimples(8,
        "Condição do filtro de admissão",
        binding.spItem8Status, binding.edItem8Valor
    )

    linhaDupla(9,
        "Última substituição do filtro de admissão",
        binding.spItem9Status,
        binding.edItem9Data, "Data",
        binding.edItem9Horas, "Horas"
    )

    linhaSimples(10,
        "Verificar nível do óleo refrigerante",
        binding.spItem10Status, binding.edItem10Obs
    )

    linhaSimples(11,
        "Inspecionar vazamentos de óleo",
        binding.spItem11Status, binding.edItem11Obs
    )

    linhaSimples(12,
        "Substituição do filtro de óleo em (2.000h ou 1 ano)",
        binding.spItem12Status, binding.edItem12Obs
    )

    linhaSimples(13,
        "Unidade de pressão do separador a plena carga (PSIG / BarG)",
        binding.spItem13Status, binding.edItem13Valor
    )

    linhaDupla(14,
        "Data da última substituição do elemento separador",
        binding.spItem14Status,
        binding.edItem14Data, "Data",
        binding.edItem14Horas, "Horas"
    )

    linhaSimples(15,
        "Data da última limpeza do orifício e tela do pescador",
        binding.spItem15Status, binding.edItem15Obs
    )

    linhaSimples(16,
        "Inspecionar e limpar o respiro da caixa de engrenagens",
        binding.spItem16Status, binding.edItem16Obs
    )

    linhaSimples(17,
        "Temperatura ambiente da instalação (°F / °C)",
        binding.spItem17Status, binding.edItem17Valor
    )

    linhaTripla(18,
        "Temp. da válvula de controle termostático (°F / °C) – Abertura",
        binding.spItem18Status,
        binding.edItem18A, "A",
        binding.edItem18B, "B",
        binding.edItem18C, "C"
    )

    linhaSimples(19,
        "Alinhamento da correia verificado e em boas condições",
        binding.spItem19Status, binding.edItem19Obs
    )

    linhaSimples(20,
        "Sistema da tensão da correia verificado",
        binding.spItem20Status, binding.edItem20Obs
    )

    linhaSimples(21,
        "Inspecionar por vazamentos de ar",
        binding.spItem21Status, binding.edItem21Obs
    )

    linhaSimples(22,
        "Inspecionar os núcleos dos trocadores de calor",
        binding.spItem22Status, binding.edItem22Obs
    )

    linhaSimples(23,
        "Inspecionar e limpar o dreno de condensado",
        binding.spItem23Status, binding.edItem23Obs
    )

    linhaSimples(24,
        "Inspecionar o motor principal e o ventilador",
        binding.spItem24Status, binding.edItem24Obs
    )

    linhaDupla(25,
        "Última lubrificação do motor principal",
        binding.spItem25Status,
        binding.edItem25Data, "Data",
        binding.edItem25Horas, "Horas"
    )

    linhaDupla(26,
        "Última lubrificação do motor do ventilador",
        binding.spItem26Status,
        binding.edItem26Data, "Data",
        binding.edItem26Horas, "Horas"
    )

    linhaSimples(27,
        "Válvula de segurança instalada e operacional",
        binding.spItem27Status, binding.edItem27Obs
    )

    linhaSimples(28,
        "Tipo do óleo refrigerante",
        binding.spItem28Status, binding.edItem28Tipo
    )

    linhaDupla(29,
        "Última troca do óleo refrigerante",
        binding.spItem29Status,
        binding.edItem29Data, "Data",
        binding.edItem29Horas, "Horas"
    )

    // ===== Inspeções elétricas =====
    linhaTripla(30,
        "Tensão (plena carga)",
        binding.spItem30Status,
        binding.edItem30L1, "L1",
        binding.edItem30L2, "L2",
        binding.edItem30L3, "L3"
    )

    linhaTripla(31,
        "Tensão (sem carga)",
        binding.spItem31Status,
        binding.edItem31L1, "L1",
        binding.edItem31L2, "L2",
        binding.edItem31L3, "L3"
    )

    linhaTripla(32,
        "Corrente do motor (plena carga)",
        binding.spItem32Status,
        binding.edItem32T1, "T1",
        binding.edItem32T2, "T2",
        binding.edItem32T3, "T3"
    )

    linhaTripla(33,
        "Corrente do motor (sem carga)",
        binding.spItem33Status,
        binding.edItem33T1, "T1",
        binding.edItem33T2, "T2",
        binding.edItem33T3, "T3"
    )

    linhaTripla(34,
        "Queda de tensão através da chave de partida",
        binding.spItem34Status,
        binding.edItem34L1, "L1",
        binding.edItem34L2, "L2",
        binding.edItem34L3, "L3"
    )

    linhaTripla(35,
        "Corrente total do pacote (plena carga)",
        binding.spItem35Status,
        binding.edItem35L1, "L1",
        binding.edItem35L2, "L2",
        binding.edItem35L3, "L3"
    )

    linhaQuadrupla(36,
        "Dados da placa de identificação do motor",
        binding.spItem36Status,
        binding.edItem36HpKw, "HP/kW",
        binding.edItem36Rpm, "RPM",
        binding.edItem36V, "V",
        binding.edItem36A, "A"
    )

    linhaSimples(37,
        "Inspecionar os contatores",
        binding.spItem37Status, binding.edItem37Obs
    )

    linhaSimples(38,
        "Verificar as conexões elétricas",
        binding.spItem38Status, binding.edItem38Obs
    )

    linhaSimples(39,
        "Temperatura operacional HAT (°C) (corte de alta temperatura)",
        binding.spItem39Status, binding.edItem39Valor
    )

    // ===== Secador de refrigeração =====
    linhaSimples(40,
        "Ponto de orvalho (°C)",
        binding.spItem40Status, binding.edItem40PontoOrvalho
    )

    linhaSimples(41,
        "Pré-filtro",
        binding.spItem41Status, binding.edItem41Ref
    )

    linhaSimples(42,
        "Pós-filtro",
        binding.spItem42Status, binding.edItem42Ref
    )

    return sb.toString()
 }

        
}