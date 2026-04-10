package com.soutech.relatoriotecnico.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.R
import com.soutech.relatoriotecnico.core.SessionManager
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.databinding.ActivityMainBinding
import com.soutech.relatoriotecnico.ui.cliente.ClienteListaActivity
import com.soutech.relatoriotecnico.ui.maquina.MaquinasCadastradasActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioCompressorFormActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioFormActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioListaActivity
import com.soutech.relatoriotecnico.ui.tecnico.TecnicosCadastradosActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val session by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        configurarSaudacao()
        configurarBotoes()
        configurarBottomNav()
    }

    override fun onResume() {
        super.onResume()
        carregarEstatisticas()
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun configurarSaudacao() {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saudacao = when {
            hora < 12 -> "Bom dia"
            hora < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
        val nome = session.getNome()?.takeIf { it.isNotBlank() } ?: "Técnico"
        binding.tvSaudacao.text = "$saudacao, $nome!"
        binding.tvNomeUsuario.text = session.getEmail() ?: "SOUTECH Automação"
    }

    private fun carregarEstatisticas() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val numClientes = withContext(Dispatchers.IO) { db.clienteDao().contarTodos() }
            val numRelatorios = withContext(Dispatchers.IO) { db.relatorioDao().contarTodos() }
            val numTecnicos = withContext(Dispatchers.IO) { db.tecnicoDao().contarTodos() }
            binding.tvNumClientes.text = numClientes.toString()
            binding.tvNumRelatorios.text = numRelatorios.toString()
            binding.tvNumTecnicos.text = numTecnicos.toString()
        }
    }

    private fun configurarBotoes() {
        binding.btnNovoRelatorioGeral.setOnClickListener {
            startActivity(Intent(this, RelatorioFormActivity::class.java))
        }
        binding.btnNovoRelatorioCompressor.setOnClickListener {
            startActivity(Intent(this, RelatorioCompressorFormActivity::class.java))
        }
        binding.btnHistoricoRelatorios.setOnClickListener {
            startActivity(Intent(this, RelatorioListaActivity::class.java))
        }
        binding.cardClientes.setOnClickListener {
            startActivity(Intent(this, ClienteListaActivity::class.java))
        }
        binding.cardMaquinas.setOnClickListener {
            startActivity(Intent(this, MaquinasCadastradasActivity::class.java))
        }
        binding.cardTecnicos.setOnClickListener {
            startActivity(Intent(this, TecnicosCadastradosActivity::class.java))
        }
    }

    private fun configurarBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_relatorios -> {
                    startActivity(Intent(this, RelatorioListaActivity::class.java))
                    false
                }
                R.id.nav_cadastros -> {
                    startActivity(Intent(this, ClienteListaActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
}
