package com.soutech.relatoriotecnico.ui.cliente

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.databinding.ActivityClienteListaBinding
import kotlinx.coroutines.launch

class ClienteListaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteListaBinding
    private lateinit var adapter: ArrayAdapter<String>
    private var clientes: List<ClienteEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteListaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Clientes cadastrados"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.listClientes.adapter = adapter

        binding.listClientes.setOnItemClickListener { _, _, position, _ ->
            val cliente = clientes[position]
            mostrarOpcoes(cliente)
        }

        binding.btnVoltar.setOnClickListener {
           finish()
        }

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
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = db.clienteDao().listarTodos()
            val nomes = clientes.map { it.nomeFantasia }
            adapter.clear()
            adapter.addAll(nomes)
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarOpcoes(cliente: ClienteEntity) {
        val opcoes = arrayOf("Editar", "Excluir")
        AlertDialog.Builder(this)
            .setTitle(cliente.nomeFantasia)
            .setItems(opcoes) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> editar(cliente)
                    1 -> confirmarExclusao(cliente)
                }
            }
            .show()
    }

    private fun editar(cliente: ClienteEntity) {
        val i = Intent(this, ClienteFormActivity::class.java)
        i.putExtra("clienteId", cliente.id)
        startActivity(i)
    }

    private fun confirmarExclusao(cliente: ClienteEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir cliente")
            .setMessage("Tem certeza que deseja excluir este cliente? Se houver relatórios associados, eles não serão excluídos automaticamente.")
            .setPositiveButton("Excluir") { _, _ -> excluir(cliente) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(cliente: ClienteEntity) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.clienteDao().deletar(cliente)
            Toast.makeText(this@ClienteListaActivity, "Cliente excluído.", Toast.LENGTH_SHORT).show()
            carregarClientes()
        }
    }
}
