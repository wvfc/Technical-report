package com.soutech.relatoriotecnico.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.soutech.relatoriotecnico.databinding.ActivityMainBinding
import com.soutech.relatoriotecnico.ui.cliente.ClienteFormActivity
import com.soutech.relatoriotecnico.ui.cliente.ClienteListaActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioFormActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioListaActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        binding.btnNovoRelatorio.setOnClickListener {
            startActivity(Intent(this, RelatorioFormActivity::class.java))
        }

        binding.btnHistoricoRelatorios.setOnClickListener {
            startActivity(Intent(this, RelatorioListaActivity::class.java))
        }

        binding.btnCadastroCliente.setOnClickListener {
            startActivity(Intent(this, ClienteFormActivity::class.java))
        }

        binding.btnClientesCadastrados.setOnClickListener {
            startActivity(Intent(this, ClienteListaActivity::class.java))
        }
    }
}
