package com.soutech.relatoriotecnico.ui.maquina

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.MaquinaEntity
import com.soutech.relatoriotecnico.databinding.ActivityMaquinasCadastradasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MaquinasCadastradasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaquinasCadastradasBinding
    private val listaMaquinas = mutableListOf<MaquinaEntity>()
    private val mapaClientes = mutableMapOf<Long, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaquinasCadastradasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Máquinas cadastradas"

        binding.rvMaquinas.layoutManager = LinearLayoutManager(this)

        binding.btnBuscar.setOnClickListener {
            carregarMaquinas()
        }

        carregarClientesEMaquinas()
    }

    override fun onResume() {
        super.onResume()
        carregarClientesEMaquinas()
    }

    private fun carregarClientesEMaquinas() {
        lifecycleScope.launch {
            // Carrega clientes para o mapa nome
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MaquinasCadastradasActivity)
                val clientes: List<ClienteEntity> = db.clienteDao().listarTodos()
                mapaClientes.clear()
                clientes.forEach { mapaClientes[it.id] = it.nomeFantasia }
            }
            carregarMaquinas()
        }
    }

    private fun carregarMaquinas() {
        val termoBusca = binding.etBusca.text.toString().trim()

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MaquinasCadastradasActivity)
                if (termoBusca.isEmpty()) {
                    db.maquinaDao().listarTodos()
                } else {
                    db.maquinaDao().buscarPorTermo(termoBusca)
                }
            }

            listaMaquinas.clear()
            listaMaquinas.addAll(resultado)

            if (listaMaquinas.isEmpty()) {
                binding.rvMaquinas.visibility = View.GONE
                binding.tvVazio.visibility = View.VISIBLE
            } else {
                binding.rvMaquinas.visibility = View.VISIBLE
                binding.tvVazio.visibility = View.GONE
                binding.rvMaquinas.adapter = MaquinaAdapter(
                    itens = listaMaquinas.toList(),
                    mapaClientes = mapaClientes,
                    onExcluir = { maquina -> confirmarExclusao(maquina) }
                )
            }
        }
    }

    private fun confirmarExclusao(maquina: MaquinaEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir máquina")
            .setMessage("Deseja excluir '${maquina.marca} ${maquina.modelo}'?")
            .setPositiveButton("Excluir") { _, _ -> excluir(maquina) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(maquina: MaquinaEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@MaquinasCadastradasActivity)
                    .maquinaDao()
                    .deletar(maquina)
            }
            Toast.makeText(this@MaquinasCadastradasActivity, "Máquina excluída.", Toast.LENGTH_SHORT).show()
            carregarMaquinas()
        }
    }
}
