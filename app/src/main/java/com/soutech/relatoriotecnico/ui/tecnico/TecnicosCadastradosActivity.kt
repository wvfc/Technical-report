package com.soutech.relatoriotecnico.ui.tecnico

import android.os.Bundle
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
    private val listaTecnicos = mutableListOf<TecnicoEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTecnicosCadastradosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Técnicos cadastrados"

        binding.rvTecnicos.layoutManager = LinearLayoutManager(this)

        binding.btnBuscarTecnico.setOnClickListener {
            carregarTecnicos()
        }

        carregarTecnicos()
    }

    override fun onResume() {
        super.onResume()
        carregarTecnicos()
    }

    private fun carregarTecnicos() {
        val termo = binding.etBuscaTecnico.text.toString().trim()

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@TecnicosCadastradosActivity)
                if (termo.isEmpty()) {
                    db.tecnicoDao().listarTodos()
                } else {
                    db.tecnicoDao().buscarPorNome(termo)
                }
            }

            listaTecnicos.clear()
            listaTecnicos.addAll(resultado)

            if (listaTecnicos.isEmpty()) {
                binding.rvTecnicos.visibility = View.GONE
                binding.tvVazio.visibility = View.VISIBLE
            } else {
                binding.rvTecnicos.visibility = View.VISIBLE
                binding.tvVazio.visibility = View.GONE
                binding.rvTecnicos.adapter = TecnicoAdapter(
                    itens = listaTecnicos.toList(),
                    onExcluir = { tecnico -> confirmarExclusao(tecnico) }
                )
            }
        }
    }

    private fun confirmarExclusao(tecnico: TecnicoEntity) {
        AlertDialog.Builder(this)
            .setTitle("Excluir técnico")
            .setMessage("Deseja excluir '${tecnico.nome}'?")
            .setPositiveButton("Excluir") { _, _ -> excluir(tecnico) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluir(tecnico: TecnicoEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(this@TecnicosCadastradosActivity)
                    .tecnicoDao()
                    .deletar(tecnico)
            }
            Toast.makeText(this@TecnicosCadastradosActivity, "Técnico excluído.", Toast.LENGTH_SHORT).show()
            carregarTecnicos()
        }
    }
}
