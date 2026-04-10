package com.soutech.relatoriotecnico.ui.tecnico

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
import com.soutech.relatoriotecnico.data.TecnicoEntity
import com.soutech.relatoriotecnico.databinding.ActivityTecnicosCadastradosBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TecnicosCadastradosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTecnicosCadastradosBinding
    private var todosTecnicos: List<TecnicoEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicosCadastradosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Técnicos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.rvTecnicos.layoutManager = LinearLayoutManager(this)

        binding.fabNovoTecnico.setOnClickListener {
            startActivity(Intent(this, CadastroTecnicoActivity::class.java))
        }

        binding.etBuscaTecnico.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrar(s.toString())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        carregarTecnicos()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarTecnicos() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvVazio.visibility = View.GONE

        lifecycleScope.launch {
            todosTecnicos = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@TecnicosCadastradosActivity).tecnicoDao().listarTodos()
            }
            binding.progressBar.visibility = View.GONE
            filtrar(binding.etBuscaTecnico.text.toString())
        }
    }

    private fun filtrar(termo: String) {
        val filtrados = if (termo.isBlank()) {
            todosTecnicos
        } else {
            todosTecnicos.filter {
                it.nome.contains(termo, ignoreCase = true) ||
                it.funcao?.contains(termo, ignoreCase = true) == true
            }
        }

        if (filtrados.isEmpty()) {
            binding.rvTecnicos.visibility = View.GONE
            binding.tvVazio.visibility = View.VISIBLE
        } else {
            binding.rvTecnicos.visibility = View.VISIBLE
            binding.tvVazio.visibility = View.GONE
            binding.rvTecnicos.adapter = TecnicoAdapter(
                itens = filtrados,
                onExcluir = { tecnico -> confirmarExclusao(tecnico) }
            )
        }
    }

    private fun confirmarExclusao(tecnico: TecnicoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir técnico")
            .setMessage("Excluir '${tecnico.nome}'?")
            .setPositiveButton("Excluir") { _, _ -> excluir(tecnico) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(tecnico: TecnicoEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@TecnicosCadastradosActivity).tecnicoDao().deletar(tecnico)
            }
            Toast.makeText(this@TecnicosCadastradosActivity, "Técnico excluído.", Toast.LENGTH_SHORT).show()
            carregarTecnicos()
        }
    }
}
