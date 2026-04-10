package com.soutech.relatoriotecnico.ui.cliente

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.databinding.ActivityClienteListaBinding
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioListaActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClienteListaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteListaBinding
    private lateinit var adapter: ClienteAdapter
    private var todosClientes: List<ClienteEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Clientes"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ClienteAdapter(emptyList()) { cliente -> mostrarOpcoes(cliente) }
        binding.rvClientes.layoutManager = LinearLayoutManager(this)
        binding.rvClientes.adapter = adapter

        binding.fabNovoCliente.setOnClickListener {
            startActivity(Intent(this, ClienteFormActivity::class.java))
        }

        binding.etBusca.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        carregarClientes()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarClientes() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvVazio.visibility = View.GONE
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            todosClientes = withContext(Dispatchers.IO) { db.clienteDao().listarTodos() }
            binding.progressBar.visibility = View.GONE
            filtrar(binding.etBusca.text.toString())
        }
    }

    private fun filtrar(termo: String) {
        val filtrados = if (termo.isBlank()) {
            todosClientes
        } else {
            todosClientes.filter {
                it.nomeFantasia.contains(termo, ignoreCase = true) ||
                it.razaoSocial.contains(termo, ignoreCase = true) ||
                it.documento?.contains(termo, ignoreCase = true) == true
            }
        }
        adapter.atualizar(filtrados)
        binding.tvVazio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        binding.rvClientes.visibility = if (filtrados.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarOpcoes(cliente: ClienteEntity) {
        val opcoes = arrayOf("Editar", "Ver relatórios", "Excluir")
        AlertDialog.Builder(this)
            .setTitle(cliente.nomeFantasia)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> editar(cliente)
                    1 -> verRelatorios(cliente)
                    2 -> confirmarExclusao(cliente)
                }
            }
            .show()
    }

    private fun editar(cliente: ClienteEntity) {
        val i = Intent(this, ClienteFormActivity::class.java)
        i.putExtra("clienteId", cliente.id)
        startActivity(i)
    }

    private fun verRelatorios(cliente: ClienteEntity) {
        val i = Intent(this, RelatorioListaActivity::class.java)
        i.putExtra("clienteId", cliente.id)
        startActivity(i)
    }

    private fun confirmarExclusao(cliente: ClienteEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir cliente")
            .setMessage("Excluir \"${cliente.nomeFantasia}\"? Os relatórios associados não serão removidos automaticamente.")
            .setPositiveButton("Excluir") { _, _ -> excluir(cliente) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(cliente: ClienteEntity) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { db.clienteDao().deletar(cliente) }
            Toast.makeText(this@ClienteListaActivity, "Cliente excluído.", Toast.LENGTH_SHORT).show()
            carregarClientes()
        }
    }
}
