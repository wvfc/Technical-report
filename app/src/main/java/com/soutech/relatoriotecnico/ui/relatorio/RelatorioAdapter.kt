package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.databinding.ItemRelatorioBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RelatorioDto(
    val id: Long,
    val tipo: String,
    val clienteNome: String,
    val modeloMaquina: String,
    val tipoManutencao: String,
    val dataEntrada: Long,
    val pdfPath: String?
)

class RelatorioAdapter(
    private val context: Context,
    private val itens: List<RelatorioDto>,
    private val onItemClick: ((RelatorioDto) -> Unit)? = null
) : RecyclerView.Adapter<RelatorioAdapter.RelatorioViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class RelatorioViewHolder(val binding: ItemRelatorioBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatorioViewHolder {
        val binding = ItemRelatorioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RelatorioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelatorioViewHolder, position: Int) {
        val item = itens[position]

        val tipoLabel = if (item.tipo == "compressor") "Compressor" else "Geral"

        holder.binding.chipTipo.text = tipoLabel
        holder.binding.tvTituloRelatorio.text = item.modeloMaquina.ifEmpty { "Relatório $tipoLabel" }
        holder.binding.tvLinha2Relatorio.text = "${item.tipoManutencao} • ${sdf.format(Date(item.dataEntrada))}"
        holder.binding.tvLinha3Relatorio.text = item.clienteNome

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }

        holder.binding.btnWhatsapp.setOnClickListener {
            compartilharWhatsapp(item)
        }
    }

    override fun getItemCount(): Int = itens.size

    private fun compartilharWhatsapp(item: RelatorioDto) {
        val tipoLabel = if (item.tipo == "compressor") "Compressor" else "Geral"
        val mensagem = """
            Relatório Técnico – $tipoLabel

            Equipamento: ${item.modeloMaquina}
            Manutenção: ${item.tipoManutencao}
            Cliente: ${item.clienteNome}
            Data: ${sdf.format(Date(item.dataEntrada))}
            ${if (!item.pdfPath.isNullOrBlank()) "\nPDF gerado e salvo no dispositivo." else ""}
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensagem)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_LONG).show()
        }
    }
}
