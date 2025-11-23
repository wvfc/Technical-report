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
            Toast.makeText(this, "${uris.size} imagem(ns) selecionada(s).", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioCompressorFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Relatório – Compressor"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        configurarSpinners()
        carregarClientes()

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

    private fun configurarSpinners() {
        val tipos = listOf("Preventiva", "Preditiva", "Corretiva", "Inspeção")
        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_spinner_item, tipos)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoManutencao.adapter = adapterTipo
    }

    private fun carregarClientes() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = db.clienteDao().listarTodos()
            val nomes = clientes.map { it.nomeFantasia }
            val adapter = ArrayAdapter(
                this@RelatorioCompressorFormActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spCliente.adapter = adapter

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
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(this, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                campo.setText(sdfDataHora.format(cal.time))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

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

        // Monta um texto resumo do checklist (para salvar e mandar pro PDF)
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

            // salvar imagens
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

            // atualiza caminho do PDF
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

    private fun montarResumoChecklist(): String {
        val sb = StringBuilder()

        fun linha(numero: Int, titulo: String, sp: Spinner, ed: EditText) {
            val status = sp.selectedItem?.toString() ?: "-"
            val valor = ed.text.toString().trim()
            sb.append(numero)
                .append(" - ")
                .append(titulo)
                .append(" | Status: ")
                .append(status)
                .append(if (valor.isNotEmpty()) " | Valor/Obs: $valor" else "")
                .append("\n")
        }

        linha(
            1,
            "Total de horas de funcionamento (em carga)",
            binding.spItem1Status,
            binding.edItem1Valor
        )

        linha(
            2,
            "Pressão de descarga do pacote (carga/alívio)",
            binding.spItem2Status,
            binding.edItem2Valor
        )

        // aqui você pode adicionar mais linhas linha(3,...), linha(4,...) copiando o padrão

        return sb.toString()
    }
}