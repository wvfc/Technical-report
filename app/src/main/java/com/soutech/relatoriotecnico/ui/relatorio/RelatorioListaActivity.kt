package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.RelatorioComCliente
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioListaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class RelatorioListaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioListaBinding
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val listaClientes = mutableListOf<ClienteEntity?>()
    private var todosRelatorios: List<RelatorioComCliente> = emptyList()
    private var filtroClienteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Relatórios"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        filtroClienteId = intent.getLongExtra("clienteId", -1L).takeIf { it > 0 }

        binding.rvRelatorios.layoutManager = LinearLayoutManager(this)

        binding.btnAplicarFiltro.setOnClickListener { aplicarFiltros() }

        binding.etNumeroSerieFiltro.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = aplicarFiltros()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        carregarDados()
    }

    override fun onResume() {
        super.onResume()
        carregarDados()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarDados() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvVazio.visibility = View.GONE
        binding.rvRelatorios.visibility = View.GONE

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@RelatorioListaActivity)

            val clientes = withContext(Dispatchers.IO) { db.clienteDao().listarTodos() }
            todosRelatorios = withContext(Dispatchers.IO) {
                if (filtroClienteId != null)
                    db.relatorioDao().listarPorCliente(filtroClienteId!!)
                else
                    db.relatorioDao().listarComCliente()
            }

            listaClientes.clear()
            listaClientes.add(null)
            listaClientes.addAll(clientes)

            val nomes = listaClientes.map { it?.nomeFantasia ?: "Todos os clientes" }
            val spinnerAdapter = ArrayAdapter(
                this@RelatorioListaActivity,
                android.R.layout.simple_spinner_item,
                nomes
            )
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerClienteFiltro.adapter = spinnerAdapter

            // Pre-select client if coming from ClienteListaActivity
            if (filtroClienteId != null) {
                val idx = listaClientes.indexOfFirst { it?.id == filtroClienteId }
                if (idx >= 0) binding.spinnerClienteFiltro.setSelection(idx)
            }

            binding.progressBar.visibility = View.GONE
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        val dataDe = binding.etDataDe.text.toString().trim()
        val dataAte = binding.etDataAte.text.toString().trim()
        val termoBusca = binding.etNumeroSerieFiltro.text.toString().trim()
        val posCliente = binding.spinnerClienteFiltro.selectedItemPosition
        val clienteSelecionado = listaClientes.getOrNull(posCliente)

        val tsInicio: Long? = if (dataDe.isNotEmpty()) {
            try { sdf.parse(dataDe)?.time } catch (_: Exception) { null }
        } else null

        val tsFim: Long? = if (dataAte.isNotEmpty()) {
            try {
                val d = sdf.parse(dataAte)
                if (d != null) d.time + 86_399_999L else null
            } catch (_: Exception) { null }
        } else null

        val filtrados = todosRelatorios.filter { item ->
            val passCliente = clienteSelecionado == null || item.cliente.id == clienteSelecionado.id
            val passDataInicio = tsInicio == null || item.relatorio.dataEntrada >= tsInicio
            val passDataFim = tsFim == null || item.relatorio.dataEntrada <= tsFim
            val passBusca = termoBusca.isEmpty() ||
                item.relatorio.modeloMaquina.contains(termoBusca, ignoreCase = true) ||
                item.cliente.nomeFantasia.contains(termoBusca, ignoreCase = true)
            passCliente && passDataInicio && passDataFim && passBusca
        }

        if (filtrados.isEmpty()) {
            binding.rvRelatorios.visibility = View.GONE
            binding.tvVazio.visibility = View.VISIBLE
        } else {
            binding.rvRelatorios.visibility = View.VISIBLE
            binding.tvVazio.visibility = View.GONE

            val dtos = filtrados.map { item ->
                RelatorioDto(
                    id = item.relatorio.id,
                    tipo = item.relatorio.tipoRelatorio,
                    clienteNome = item.cliente.nomeFantasia,
                    modeloMaquina = item.relatorio.modeloMaquina,
                    tipoManutencao = item.relatorio.tipoManutencao,
                    dataEntrada = item.relatorio.dataEntrada,
                    pdfPath = item.relatorio.pdfPath
                )
            }

            binding.rvRelatorios.adapter = RelatorioAdapter(
                context = this,
                itens = dtos,
                onItemClick = { dto ->
                    val intent = Intent(this, RelatorioDetalheActivity::class.java)
                    intent.putExtra("relatorioId", dto.id)
                    startActivity(intent)
                }
            )
        }
    }
}
