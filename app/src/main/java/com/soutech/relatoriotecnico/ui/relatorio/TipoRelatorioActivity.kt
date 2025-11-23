package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.soutech.relatoriotecnico.databinding.ActivityTipoRelatorioBinding

class TipoRelatorioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipoRelatorioBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipoRelatorioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Novo relatório"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnRelatorioGeral.setOnClickListener {
            startActivity(Intent(this, RelatorioFormActivity::class.java))
        }

        binding.btnRelatorioCompressor.setOnClickListener {
            startActivity(Intent(this, RelatorioCompressorFormActivity::class.java))
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}