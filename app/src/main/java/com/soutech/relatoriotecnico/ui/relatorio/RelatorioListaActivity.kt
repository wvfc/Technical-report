package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.RelatorioComCliente
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioListaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RelatorioListaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioListaBinding
    private lateinit var adapter: ArrayAdapter<String>
    private var relatorios: List<RelatorioComCliente> = emptyList()
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Histórico de relatórios"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.listRelatorios.adapter = adapter

        binding.listRelatorios.setOnItemClickListener { _, _, position, _ ->
            val r = relatorios[position]
            val i = Intent(this, RelatorioDetalheActivity::class.java)
            i.putExtra("relatorioId", r.relatorio.id)
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        carregarRelatorios()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarRelatorios() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            relatorios = db.relatorioDao().listarComCliente()
            val descricoes = relatorios.map {
                val data = sdf.format(it.relatorio.dataEntrada)
                "$data - ${it.cliente.nomeFantasia} (${it.relatorio.tipoManutencao})"
            }
            adapter.clear()
            adapter.addAll(descricoes)
            adapter.notifyDataSetChanged()
            if (relatorios.isEmpty()) {
                Toast.makeText(this@RelatorioListaActivity, "Nenhum relatório cadastrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
