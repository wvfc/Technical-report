package com.soutech.relatoriotecnico.ui.maquina

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
import com.soutech.relatoriotecnico.data.MaquinaEntity
import com.soutech.relatoriotecnico.databinding.ActivityMaquinasCadastradasBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MaquinasCadastradasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaquinasCadastradasBinding
    private val mapaClientes = mutableMapOf<Long, String>()
    private var todasMaquinas: List<MaquinaEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaquinasCadastradasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Máquinas"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.rvMaquinas.layoutManager = LinearLayoutManager(this)

        binding.fabNovaMaquina.setOnClickListener {
            startActivity(Intent(this, CadastroMaquinaActivity::class.java))
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
        carregarClientesEMaquinas()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarClientesEMaquinas() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvVazio.visibility = View.GONE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@MaquinasCadastradasActivity)
                val clientes: List<ClienteEntity> = db.clienteDao().listarTodos()
                mapaClientes.clear()
                clientes.forEach { mapaClientes[it.id] = it.nomeFantasia }
                todasMaquinas = db.maquinaDao().listarTodos()
            }
            binding.progressBar.visibility = View.GONE
            filtrar(binding.etBusca.text.toString())
        }
    }

    private fun filtrar(termo: String) {
        val filtradas = if (termo.isBlank()) {
            todasMaquinas
        } else {
            todasMaquinas.filter {
                it.modelo.contains(termo, ignoreCase = true) ||
                it.marca.contains(termo, ignoreCase = true) ||
                it.numeroSerie.contains(termo, ignoreCase = true) ||
                mapaClientes[it.clienteId]?.contains(termo, ignoreCase = true) == true
            }
        }

        if (filtradas.isEmpty()) {
            binding.rvMaquinas.visibility = View.GONE
            binding.tvVazio.visibility = View.VISIBLE
        } else {
            binding.rvMaquinas.visibility = View.VISIBLE
            binding.tvVazio.visibility = View.GONE
            binding.rvMaquinas.adapter = MaquinaAdapter(
                itens = filtradas,
                mapaClientes = mapaClientes,
                onExcluir = { maquina -> confirmarExclusao(maquina) }
            )
        }
    }

    private fun confirmarExclusao(maquina: MaquinaEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir máquina")
            .setMessage("Excluir '${maquina.marca} ${maquina.modelo}' (S/N: ${maquina.numeroSerie})?")
            .setPositiveButton("Excluir") { _, _ -> excluir(maquina) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(maquina: MaquinaEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@MaquinasCadastradasActivity)
                    .maquinaDao().deletar(maquina)
            }
            Toast.makeText(this@MaquinasCadastradasActivity, "Máquina excluída.", Toast.LENGTH_SHORT).show()
            carregarClientesEMaquinas()
        }
    }
}
