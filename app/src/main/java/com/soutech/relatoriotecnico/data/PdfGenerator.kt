package com.soutech.relatoriotecnico.data

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun gerarPdf(context: Context, relatorio: RelatorioComCliente): File? {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 aproximado
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 18f
        }

        val paintSub = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 12f
        }

        val paintText = Paint().apply {
            textSize = 10f
        }

        var y = 60f

        // Título
        canvas.drawText("Relatório Técnico de Manutenção", 50f, y, paintTitle)
        y += 20f

        // Cliente
        canvas.drawText("Dados do Cliente", 50f, y, paintSub)
        y += 16f
        canvas.drawText("Nome fantasia: ${relatorio.cliente.nomeFantasia}", 50f, y, paintText); y += 14f
        canvas.drawText("Razão social: ${relatorio.cliente.razaoSocial}", 50f, y, paintText); y += 14f
        canvas.drawText("CNPJ/CPF: ${relatorio.cliente.documento ?: ""}", 50f, y, paintText); y += 14f
        canvas.drawText("Endereço: ${relatorio.cliente.endereco ?: ""}", 50f, y, paintText); y += 14f

        y += 10f
        canvas.drawText("Dados do Atendimento", 50f, y, paintSub)
        y += 16f

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Entrada: ${sdf.format(Date(relatorio.relatorio.dataEntrada))}", 50f, y, paintText); y += 14f
        canvas.drawText("Saída:   ${sdf.format(Date(relatorio.relatorio.dataSaida))}", 50f, y, paintText); y += 14f
        canvas.drawText("Modelo da máquina: ${relatorio.relatorio.modeloMaquina}", 50f, y, paintText); y += 14f
        canvas.drawText("Tipo de manutenção: ${relatorio.relatorio.tipoManutencao}", 50f, y, paintText); y += 20f

        fun drawWrappedText(label: String, text: String?) {
            if (text.isNullOrBlank()) return
            canvas.drawText(label, 50f, y, paintSub)
            y += 16f
            val maxWidth = 480f
            val words = text.split(" ")
            var line = ""
            for (w in words) {
                val testLine = if (line.isEmpty()) w else "$line $w"
                if (paintText.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, 50f, y, paintText)
                    y += 14f
                    line = w
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 50f, y, paintText)
                y += 16f
            }
            y += 8f
        }

        drawWrappedText("Ocorrência", relatorio.relatorio.ocorrencia)
        drawWrappedText("Solução proposta", relatorio.relatorio.solucaoProposta)
        drawWrappedText("Lista de peças", relatorio.relatorio.pecasTexto)

        // Assinatura
        y += 30f
        canvas.drawText("Assinatura do responsável: ____________________________", 50f, y, paintText)

        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "relatorio_${relatorio.relatorio.id}.pdf")

        return try {
            file.outputStream().use { out ->
                doc.writeTo(out)
            }
            doc.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            doc.close()
            null
        }
    }
}
