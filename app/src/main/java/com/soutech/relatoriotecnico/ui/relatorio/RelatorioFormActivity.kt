package com.soutech.relatoriotecnico.ui.relatorio

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.ImagemRelatorioEntity
import com.soutech.relatoriotecnico.data.RelatorioEntity
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioFormBinding
import com.soutech.relatoriotecnico.util.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RelatorioFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioFormBinding
    private var clientes: List<ClienteEntity> = emptyList()
    private var imagensUris: MutableList<Uri> = mutableListOf()
    private var relatorioId: Long? = null

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
        binding = ActivityRelatorioFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Novo relatório"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        relatorioId = intent.getLongExtra("relatorioId", -1L).takeIf { it > 0 }

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

    private fun carregarClientes() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = db.clienteDao().listarTodos()
            val nomes = clientes.map { it.nomeFantasia }
            val adapter = ArrayAdapter(
                this@RelatorioFormActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spCliente.adapter = adapter

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@RelatorioFormActivity,
                    "Cadastre um cliente antes de criar relatórios.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // TODO: se relatorioId != null, carregar dados para edição
        }
    }

    private fun escolherDataHora(campo: android.widget.EditText) {
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
        val ocorrencia = binding.edOcorrencia.text.toString().trim()
        val solucao = binding.edSolucao.text.toString().trim()
        val pecas = binding.edPecas.text.toString().trim().ifEmpty { null }

        if (dataEntradaStr.isEmpty() ||
            dataSaidaStr.isEmpty() ||
            modelo.isEmpty() ||
            ocorrencia.isEmpty() ||
            solucao.isEmpty()
        ) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val dataEntrada = sdfDataHora.parse(dataEntradaStr)?.time ?: Date().time
        val dataSaida = sdfDataHora.parse(dataSaidaStr)?.time ?: Date().time

        val db = AppDatabase.getInstance(this)

        // Faz tudo em IO e volta para Main só para o Toast/finish
        lifecycleScope.launch(Dispatchers.IO) {

            // 1) salvar o relatório (sem caminho de PDF ainda)
            val relEntity = RelatorioEntity(
                id = 0,
                clienteId = cliente.id,
                dataEntrada = dataEntrada,
                dataSaida = dataSaida,
                modeloMaquina = modelo,
                tipoManutencao = tipo,
                ocorrencia = ocorrencia,
                solucaoProposta = solucao,
                pecasTexto = pecas,
                pdfPath = null
            )
            val relId = db.relatorioDao().inserir(relEntity)

            // 2) salvar as imagens associadas (URIs)
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

            // 3) buscar o "relatório completo" (relatório + cliente + imagens)
            val completo = db.relatorioDao().buscarComCliente(relId)

            if (completo != null) {
                // ATENÇÃO:
                // No PdfUtils, use completo.imagens[x].uri para carregar as imagens
                // via contentResolver.openInputStream(Uri.parse(uriString)).

                val pdfFile = PdfUtils.gerarPdfRelatorio(
                    context = this@RelatorioFormActivity,
                    relatorio = completo.relatorio,
                    cliente = completo.cliente,
                    imagens = completo.imagens
                )

                // 4) atualizar o caminho do PDF no banco
                val atualizado = completo.relatorio.copy(pdfPath = pdfFile.absolutePath)
                db.relatorioDao().atualizar(atualizado)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RelatorioFormActivity,
                        "Relatório salvo e PDF gerado.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RelatorioFormActivity,
                        "Relatório salvo, mas não foi possível gerar o PDF (dados incompletos).",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }
}
