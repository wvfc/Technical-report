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
        val itens = mutableListOf<ChecklistItem>()

        // ===== INSPEÇÕES GERAIS – 1 a 29 =====
        itens += ChecklistItem(
            1,
            "Total de horas de funcionamento (em carga)",
            binding.spItem1Status,
            listOf(binding.edItem1Valor)
        )
        itens += ChecklistItem(
            2,
            "Pressão de descarga do pacote (carga / alívio)",
            binding.spItem2Status,
            listOf(binding.edItem2Valor)
        )
        itens += ChecklistItem(
            3,
            "Temp. de descarga do pacote a plena carga (°F / °C)",
            binding.spItem3Status,
            listOf(binding.edItem3Valor)
        )
        itens += ChecklistItem(
            4,
            "Temp. de descarga unidade compressoa a plena carga (°F / °C)",
            binding.spItem4Status,
            listOf(binding.edItem4Valor)
        )
        itens += ChecklistItem(
            5,
            "Temp. injeção do óleo a plena carga (°F / °C)",
            binding.spItem5Status,
            listOf(binding.edItem5Valor)
        )
        itens += ChecklistItem(
            6,
            "Pressão do cárter em alívio (PSIG / BarG)",
            binding.spItem6Status,
            listOf(binding.edItem6Valor)
        )
        itens += ChecklistItem(
            7,
            "Vácuo na admissão em alívio (PSIG / BarG)",
            binding.spItem7Status,
            listOf(binding.edItem7Valor)
        )
        itens += ChecklistItem(
            8,
            "Condição do filtro de admissão",
            binding.spItem8Status,
            listOf(binding.edItem8Obs)
        )
        itens += ChecklistItem(
            9,
            "Última substituição do filtro de admissão (Data / Horas)",
            binding.spItem9Status,
            listOf(binding.edItem9Data, binding.edItem9Horas)
        )
        itens += ChecklistItem(
            10,
            "Verificar nível do óleo refrigerante",
            binding.spItem10Status,
            listOf(binding.edItem10Obs)
        )
        itens += ChecklistItem(
            11,
            "Inspecionar vazamentos de óleo",
            binding.spItem11Status,
            listOf(binding.edItem11Obs)
        )
        itens += ChecklistItem(
            12,
            "Substituição do filtro de óleo (2.000 h ou 1 ano)",
            binding.spItem12Status,
            listOf(binding.edItem12Obs)
        )
        itens += ChecklistItem(
            13,
            "Queda de pressão do separador de óleo do pacote (PSIG / BarG)",
            binding.spItem13Status,
            listOf(binding.edItem13Valor)
        )
        itens += ChecklistItem(
            14,
            "Data da última substituição do elemento separador",
            binding.spItem14Status,
            listOf(binding.edItem14Data)
        )
        itens += ChecklistItem(
            15,
            "Inspecionar e limpar orifício e tela do separador",
            binding.spItem15Status,
            listOf(binding.edItem15Obs)
        )
        itens += ChecklistItem(
            16,
            "Inspecionar e limpar o respiro da caixa de engrenagens",
            binding.spItem16Status,
            listOf(binding.edItem16Obs)
        )
        itens += ChecklistItem(
            17,
            "Temperatura ambiente da instalação (°F / °C)",
            binding.spItem17Status,
            listOf(binding.edItem17Valor)
        )
        itens += ChecklistItem(
            18,
            "Temp. da válvula de controle termostático (°F / °C) e abertura",
            binding.spItem18Status,
            listOf(binding.edItem18Temp, binding.edItem18Abertura)
        )
        itens += ChecklistItem(
            19,
            "Alinhamento da correia verificado e em boas condições (A / B / C)",
            binding.spItem19Status,
            listOf(binding.edItem19A, binding.edItem19B, binding.edItem19C)
        )
        itens += ChecklistItem(
            20,
            "Sistema de tensão da correia verificado",
            binding.spItem20Status,
            listOf(binding.edItem20Obs)
        )
        itens += ChecklistItem(
            21,
            "Inspecionar por vazamentos de ar",
            binding.spItem21Status,
            listOf(binding.edItem21Obs)
        )
        itens += ChecklistItem(
            22,
            "Inspecionar os núcleos dos trocadores de calor",
            binding.spItem22Status,
            listOf(binding.edItem22Obs)
        )
        itens += ChecklistItem(
            23,
            "Inspecionar e limpar o dreno de condensado",
            binding.spItem23Status,
            listOf(binding.edItem23Obs)
        )
        itens += ChecklistItem(
            24,
            "Inspecionar o motor principal e o ventilador",
            binding.spItem24Status,
            listOf(binding.edItem24Obs)
        )
        itens += ChecklistItem(
            25,
            "Última lubrificação do motor principal (Data / Horas)",
            binding.spItem25Status,
            listOf(binding.edItem25Data, binding.edItem25Horas)
        )
        itens += ChecklistItem(
            26,
            "Última lubrificação do motor do ventilador (Data / Horas)",
            binding.spItem26Status,
            listOf(binding.edItem26Data, binding.edItem26Horas)
        )
        itens += ChecklistItem(
            27,
            "Válvula de segurança instalada e operacional",
            binding.spItem27Status,
            listOf(binding.edItem27Obs)
        )
        itens += ChecklistItem(
            28,
            "Tipo do óleo refrigerante",
            binding.spItem28Status,
            listOf(binding.edItem28Tipo)
        )
        itens += ChecklistItem(
            29,
            "Última troca do óleo refrigerante (Data / Horas)",
            binding.spItem29Status,
            listOf(binding.edItem29Data, binding.edItem29Horas)
        )

        // ===== INSPEÇÕES ELÉTRICAS – 30 a 39 =====
        itens += ChecklistItem(
            30,
            "Tensão (plena carga) L1 / L2 / L3",
            binding.spItem30Status,
            listOf(binding.edItem30L1, binding.edItem30L2, binding.edItem30L3)
        )
        itens += ChecklistItem(
            31,
            "Tensão (sem carga) L1 / L2 / L3",
            binding.spItem31Status,
            listOf(binding.edItem31L1, binding.edItem31L2, binding.edItem31L3)
        )
        itens += ChecklistItem(
            32,
            "Corrente do motor (plena carga) T1 / T2 / T3",
            binding.spItem32Status,
            listOf(binding.edItem32T1, binding.edItem32T2, binding.edItem32T3)
        )
        itens += ChecklistItem(
            33,
            "Corrente do motor (sem carga) T1 / T2 / T3",
            binding.spItem33Status,
            listOf(binding.edItem33T1, binding.edItem33T2, binding.edItem33T3)
        )
        itens += ChecklistItem(
            34,
            "Queda de tensão através da chave de partida L1 / L2 / L3",
            binding.spItem34Status,
            listOf(binding.edItem34L1, binding.edItem34L2, binding.edItem34L3)
        )
        itens += ChecklistItem(
            35,
            "Corrente total do pacote (plena carga) L1 / L2 / L3",
            binding.spItem35Status,
            listOf(binding.edItem35L1, binding.edItem35L2, binding.edItem35L3)
        )
        itens += ChecklistItem(
            36,
            "Dados da placa de identificação do motor (HP/kW, RPM, V, A)",
            binding.spItem36Status,
            listOf(
                binding.edItem36HpKw,
                binding.edItem36Rpm,
                binding.edItem36V,
                binding.edItem36A
            )
        )
        itens += ChecklistItem(
            37,
            "Inspecionar os contatores",
            binding.spItem37Status,
            listOf(binding.edItem37Obs)
        )
        itens += ChecklistItem(
            38,
            "Verificar as conexões elétricas",
            binding.spItem38Status,
            listOf(binding.edItem38Obs)
        )
        itens += ChecklistItem(
            39,
            "Temp. operacional HAT (°C) / corte de alta temperatura",
            binding.spItem39Status,
            listOf(binding.edItem39Valor)
        )

        // ===== SECADOR DE REFRIGERAÇÃO – 40 a 42 =====
        itens += ChecklistItem(
            40,
            "Ponto de orvalho (°C)",
            binding.spItem40Status,
            listOf(binding.edItem40PontoOrvalho)
        )
        itens += ChecklistItem(
            41,
            "Pré-filtro (referência / observações)",
            binding.spItem41Status,
            listOf(binding.edItem41Ref)
        )
        itens += ChecklistItem(
            42,
            "Pós-filtro (referência / observações)",
            binding.spItem42Status,
            listOf(binding.edItem42Ref)
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