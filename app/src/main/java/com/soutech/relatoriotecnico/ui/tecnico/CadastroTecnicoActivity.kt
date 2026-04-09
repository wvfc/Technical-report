package com.soutech.relatoriotecnico.ui.tecnico

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.TecnicoEntity
import com.soutech.relatoriotecnico.databinding.ActivityCadastroTecnicoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CadastroTecnicoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroTecnicoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroTecnicoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Técnico"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSalvarTecnico.setOnClickListener {
            salvarTecnico()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun salvarTecnico() {
        val nome = binding.etNomeTecnico.text.toString().trim()
        val funcao = binding.etFuncaoTecnico.text.toString().trim()

        if (nome.isEmpty()) {
            Toast.makeText(this, "Informe o nome do técnico.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSalvarTecnico.isEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@CadastroTecnicoActivity)
                db.tecnicoDao().inserir(
                    TecnicoEntity(
                        nome = nome,
                        funcao = funcao.ifEmpty { null }
                    )
                )
            }

            Toast.makeText(
                this@CadastroTecnicoActivity,
                "Técnico salvo com sucesso.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}
