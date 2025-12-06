package com.soutech.relatoriotecnico.ui.relatorio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.soutech.relatoriotecnico.databinding.ItemRelatorioBinding
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class RelatorioDto(
    val id: Int,
    val type: String,
    val clientId: Int,
    val clientName: String?,
    val machineId: Int?,
    val serialNumber: String?,
    val title: String,
    val dateIso: String,
    val pdfUrl: String?
)

class RelatorioAdapter(
    private val context: Context,
    private val itens: List<RelatorioDto>
) : RecyclerView.Adapter<RelatorioAdapter.RelatorioViewHolder>() {

    inner class RelatorioViewHolder(val binding: ItemRelatorioBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatorioViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRelatorioBinding.inflate(inflater, parent, false)
        return RelatorioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelatorioViewHolder, position: Int) {
        val item = itens[position]

        holder.binding.tvTituloRelatorio.text = item.title

        val tipo = if (item.type.equals("compressor", ignoreCase = true)) {
            "Compressor"
        } else {
            "Geral"
        }

        val dataFormatada = try {
            val zdt = ZonedDateTime.parse(item.dateIso)
            zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            item.dateIso
        }

        holder.binding.tvLinha2Relatorio.text = "$tipo • $dataFormatada"

        val cliente = item.clientName ?: "-"
        val numeroSerie = item.serialNumber ?: "-"
        holder.binding.tvLinha3Relatorio.text = "Cliente: $cliente • Nº série: $numeroSerie"

        holder.binding.btnWhatsapp.setOnClickListener {
            enviarWhatsapp(item)
        }
    }

    override fun getItemCount(): Int = itens.size

    private fun enviarWhatsapp(item: RelatorioDto) {
        val cliente = item.clientName ?: "-"
        val numeroSerie = item.serialNumber ?: "-"
        val pdfUrl = item.pdfUrl ?: "PDF não disponível."

        val mensagem = """
            Relatório Técnico - ${item.title}
            
            Tipo: ${item.type}
            Cliente: $cliente
            Nº série: $numeroSerie
            
            Link do relatório (PDF):
            $pdfUrl
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, mensagem)
                // Se quiser forçar WhatsApp:
                // setPackage("com.whatsapp")
            }
            startActivity(context, Intent.createChooser(intent, "Enviar relatório"), null)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o WhatsApp.", Toast.LENGTH_LONG).show()
        }
    }
}
