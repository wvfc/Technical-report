package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.RelatorioEntity
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioDetalheBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class RelatorioDetalheActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRelatorioDetalheBinding
    private var relatorioId: Long = -1L
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRelatorioDetalheBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Detalhe do relatório"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        relatorioId = intent.getLongExtra("relatorioId", -1L)

        binding.btnAbrirPdf.setOnClickListener { abrirPdf() }
        binding.btnExcluirRelatorio.setOnClickListener { excluirRelatorio() }
    }

    override fun onResume() {
        super.onResume()
        carregarDados()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarDados() {
        if (relatorioId <= 0) return
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val completo = db.relatorioDao().buscarComCliente(relatorioId)
            completo?.let {
                binding.tvCliente.text = it.cliente.nomeFantasia
                binding.tvTipo.text = it.relatorio.tipoManutencao
                binding.tvModelo.text = it.relatorio.modeloMaquina
                binding.tvDatas.text = "Entrada: ${sdf.format(it.relatorio.dataEntrada)}\nSaída: ${sdf.format(it.relatorio.dataSaida)}"
            }
        }
    }

    private fun abrirPdf() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val relatorio: RelatorioEntity? = db.relatorioDao().buscarPorIdSimples(relatorioId)
            val caminho = relatorio?.pdfPath
            if (caminho.isNullOrBlank()) {
                Toast.makeText(this@RelatorioDetalheActivity, "PDF não encontrado.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = File(caminho)
            val uri: Uri = FileProvider.getUriForFile(
                this@RelatorioDetalheActivity,
                "com.soutech.relatoriotecnico.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this@RelatorioDetalheActivity, "Nenhum aplicativo de PDF encontrado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun excluirRelatorio() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val relatorio = db.relatorioDao().buscarPorIdSimples(relatorioId)
            if (relatorio != null) {
                // Remove imagens
                db.imagemDao().deletarPorRelatorio(relatorioId)
                // Remove arquivo PDF
                relatorio.pdfPath?.let {
                    val f = File(it)
                    if (f.exists()) f.delete()
                }
                db.relatorioDao().deletar(relatorio)
                Toast.makeText(this@RelatorioDetalheActivity, "Relatório excluído.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
