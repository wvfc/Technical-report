package com.soutech.relatoriotecnico.ui.relatorio

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.soutech.relatoriotecnico.R
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.ImagemRelatorioEntity
import com.soutech.relatoriotecnico.data.MaquinaEntity
import com.soutech.relatoriotecnico.data.RelatorioEntity
import com.soutech.relatoriotecnico.util.PdfUtils

import com.soutech.relatoriotecnico.databinding.ActivityRelatorioFormBinding

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RelatorioFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioFormBinding
    private var clientes: List<ClienteEntity> = emptyList()
    private var maquinas: List<MaquinaEntity> = emptyList()
    private var imagensUris: MutableList<Uri> = mutableListOf()
    private var relatorioId: Long? = null

    private val sdfDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val pickerImagens = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            imagensUris.clear()
            imagensUris.addAll(uris)
            Toast.makeText(this, "${uris.size} imagem(ns) selecionada(s).", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tipos = resources.getStringArray(R.array.tipos_manutencao).toList()
        val tipoAdapter = ArrayAdapter(this, R.layout.spinner_item, tipos)
        tipoAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spTipoManutencao.adapter = tipoAdapter

        supportActionBar?.title = "Novo relatório"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        relatorioId = intent.getLongExtra("relatorioId", -1L).takeIf { it > 0 }

        carregarClientes()
        configurarBackPress()

        binding.edDataEntrada.setOnClickListener { escolherDataHora(binding.edDataEntrada) }
        binding.edDataSaida.setOnClickListener { escolherDataHora(binding.edDataSaida) }
        binding.btnSelecionarImagens.setOnClickListener { pickerImagens.launch(arrayOf("image/*")) }
        binding.btnSalvarRelatorio.setOnClickListener { salvarRelatorio() }
        binding.btnVoltar.setOnClickListener { finish() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun configurarBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (formTemDados()) {
                    AlertDialog.Builder(this@RelatorioFormActivity)
                        .setTitle("Descartar dados?")
                        .setMessage("Os dados preenchidos serão perdidos. Deseja sair mesmo assim?")
                        .setPositiveButton("Sair") { _, _ ->
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                        .setNegativeButton("Continuar editando", null)
                        .show()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun formTemDados(): Boolean =
        binding.edOcorrencia.text.isNotEmpty() ||
        binding.edSolucao.text.isNotEmpty() ||
        binding.edDataEntrada.text.isNotEmpty() ||
        binding.edModeloMaquina.text.isNotEmpty()

    private fun carregarClientes() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = withContext(Dispatchers.IO) { db.clienteDao().listarTodos() }

            val nomes = clientes.map { it.nomeFantasia }
            val adapter = ArrayAdapter(this@RelatorioFormActivity, R.layout.spinner_item, nomes)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spCliente.adapter = adapter

            if (clientes.isEmpty()) {
                Toast.makeText(this@RelatorioFormActivity,
                    "Cadastre um cliente antes de criar relatórios.", Toast.LENGTH_LONG).show()
            }

            // Atualizar spinner de máquinas quando cliente mudar
            binding.spCliente.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    val cliente = clientes.getOrNull(pos)
                    if (cliente != null) atualizarMaquinas(cliente.id)
                }
                override fun onNothingSelected(p: AdapterView<*>?) = Unit
            }
        }
    }

    private fun atualizarMaquinas(clienteId: Long) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            maquinas = withContext(Dispatchers.IO) { db.maquinaDao().listarPorCliente(clienteId) }
            val nomes = mutableListOf("Nenhuma (digitar manualmente)")
            nomes.addAll(maquinas.map { "${it.marca} ${it.modelo} — S/N: ${it.numeroSerie}" })
            val adapter = ArrayAdapter(this@RelatorioFormActivity, R.layout.spinner_item, nomes)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spMaquina.adapter = adapter

            binding.spMaquina.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    if (pos > 0) {
                        val maquina = maquinas[pos - 1]
                        binding.edModeloMaquina.setText("${maquina.marca} ${maquina.modelo}")
                    }
                }
                override fun onNothingSelected(p: AdapterView<*>?) = Unit
            }
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

        if (dataEntradaStr.isEmpty() || dataSaidaStr.isEmpty() || modelo.isEmpty() ||
            ocorrencia.isEmpty() || solucao.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos obrigatórios (*).", Toast.LENGTH_SHORT).show()
            return
        }

        val dataEntrada = sdfDataHora.parse(dataEntradaStr)?.time
        val dataSaida = sdfDataHora.parse(dataSaidaStr)?.time

        if (dataEntrada == null || dataSaida == null) {
            Toast.makeText(this, "Data/hora inválida. Use o seletor de data.", Toast.LENGTH_SHORT).show()
            return
        }

        val idxMaquina = binding.spMaquina.selectedItemPosition
        val maquinaId = if (idxMaquina > 0) maquinas.getOrNull(idxMaquina - 1)?.id else null

        val db = AppDatabase.getInstance(this)

        binding.btnSalvarRelatorio.isEnabled = false
        binding.progressSalvar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val relEntity = RelatorioEntity(
                id = 0,
                clienteId = cliente.id,
                maquinaId = maquinaId,
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

            val imagens = imagensUris.mapIndexed { idx, uri ->
                ImagemRelatorioEntity(relatorioId = relId, uri = uri.toString(), ordem = idx)
            }
            if (imagens.isNotEmpty()) db.imagemDao().inserirLista(imagens)

            val logoUri = db.configLogoDao().obterConfig()?.logoUri
            val completo = db.relatorioDao().buscarComCliente(relId)

            if (completo != null) {
                val pdfFile = PdfUtils.gerarPdfRelatorio(
                    context = this@RelatorioFormActivity,
                    relatorio = completo.relatorio,
                    cliente = completo.cliente,
                    imagens = completo.imagens,
                    logoUri = logoUri
                )
                db.relatorioDao().atualizar(completo.relatorio.copy(pdfPath = pdfFile.absolutePath))

                withContext(Dispatchers.Main) {
                    binding.progressSalvar.visibility = View.GONE
                    Toast.makeText(this@RelatorioFormActivity,
                        "Relatório salvo e PDF gerado.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.progressSalvar.visibility = View.GONE
                    binding.btnSalvarRelatorio.isEnabled = true
                    Toast.makeText(this@RelatorioFormActivity,
                        "Relatório salvo, mas PDF não pôde ser gerado.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}
