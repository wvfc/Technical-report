package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.R
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

        val isCompressor = item.tipo == "compressor"
        val tipoLabel = if (isCompressor) "Compressor" else "Geral"

        // Barra lateral colorida por tipo
        val barColor = if (isCompressor)
            ContextCompat.getColor(context, R.color.colorAccent)
        else
            ContextCompat.getColor(context, R.color.colorPrimary)
        holder.binding.barraLateral.setBackgroundColor(barColor)

        // Chip
        holder.binding.chipTipo.text = tipoLabel
        if (isCompressor) {
            holder.binding.chipTipo.setTextColor(Color.parseColor("#E65100"))
            holder.binding.chipTipo.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
        } else {
            holder.binding.chipTipo.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            holder.binding.chipTipo.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.colorPrimaryContainer))
        }

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
