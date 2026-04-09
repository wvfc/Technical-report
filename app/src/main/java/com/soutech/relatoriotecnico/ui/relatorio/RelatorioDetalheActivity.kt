package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.RelatorioEntity
import com.soutech.relatoriotecnico.databinding.ActivityRelatorioDetalheBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        // Bug corrigido: listeners registrados apenas uma vez
        binding.btnAbrirPdf.setOnClickListener { abrirPdf() }
        binding.btnExcluirRelatorio.setOnClickListener { confirmarExclusao() }
        binding.btnVoltar.setOnClickListener { finish() }
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
            val completo = withContext(Dispatchers.IO) {
                db.relatorioDao().buscarComCliente(relatorioId)
            }
            completo?.let {
                val tipo = if (it.relatorio.tipoRelatorio == "compressor") "Compressor" else "Geral"
                binding.tvCliente.text = it.cliente.nomeFantasia
                binding.tvTipo.text = tipo
                binding.tvModelo.text = it.relatorio.modeloMaquina
                binding.tvDatas.text = buildString {
                    append("Entrada: ${sdf.format(it.relatorio.dataEntrada)}")
                    append("\nSaída: ${sdf.format(it.relatorio.dataSaida)}")
                }
            }
        }
    }

    private fun abrirPdf() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val relatorio: RelatorioEntity? = withContext(Dispatchers.IO) {
                db.relatorioDao().buscarPorIdSimples(relatorioId)
            }
            val caminho = relatorio?.pdfPath
            if (caminho.isNullOrBlank()) {
                Toast.makeText(this@RelatorioDetalheActivity, "PDF não encontrado.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val file = File(caminho)
            if (!file.exists()) {
                Toast.makeText(this@RelatorioDetalheActivity, "Arquivo PDF não encontrado no dispositivo.", Toast.LENGTH_SHORT).show()
                return@launch
            }
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
                Toast.makeText(
                    this@RelatorioDetalheActivity,
                    "Nenhum aplicativo de PDF encontrado.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmarExclusao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir relatório")
            .setMessage("Tem certeza que deseja excluir este relatório? O PDF também será removido.")
            .setPositiveButton("Excluir") { _, _ -> excluirRelatorio() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirRelatorio() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val relatorio = db.relatorioDao().buscarPorIdSimples(relatorioId)
                if (relatorio != null) {
                    db.imagemDao().deletarPorRelatorio(relatorioId)
                    relatorio.pdfPath?.let { path ->
                        val f = File(path)
                        if (f.exists()) f.delete()
                    }
                    db.relatorioDao().deletar(relatorio)
                }
            }
            Toast.makeText(this@RelatorioDetalheActivity, "Relatório excluído.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
