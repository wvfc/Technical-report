package com.soutech.relatoriotecnico.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.ImagemRelatorioEntity
import com.soutech.relatoriotecnico.data.RelatorioEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfUtils {

    private const val TAG = "PdfUtils"

    // ============================================================
    // 1) PDF GERAL (relatório padrão)
    // ============================================================
    fun gerarPdfRelatorio(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>,
        logoUri: String? = null
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 40f
        val contentWidth = pageWidth - margin * 2
        val bottomLimit = pageHeight - 60f
        var y = 0f

        y = drawHeader(canvas, pageWidth.toFloat(), margin, context, logoUri)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun novaPagina(tituloExtra: String? = null) {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, pageWidth.toFloat(), margin, context, logoUri)
            if (!tituloExtra.isNullOrBlank()) {
                y = drawSectionTitle(canvas, tituloExtra, margin, y)
            }
        }

        // Cliente
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Cliente", margin, y)
        y = drawMultiline(
            canvas,
            buildString {
                appendLine("Nome fantasia: ${cliente.nomeFantasia}")
                if (cliente.razaoSocial.isNotEmpty()) appendLine("Razão social: ${cliente.razaoSocial}")
                if (!cliente.documento.isNullOrBlank()) appendLine("CNPJ/CPF: ${cliente.documento}")
                if (!cliente.endereco.isNullOrBlank()) appendLine("Endereço: ${cliente.endereco}")
                if (!cliente.email.isNullOrBlank()) appendLine("E-mail: ${cliente.email}")
                if (!cliente.telefone.isNullOrBlank()) appendLine("Telefone: ${cliente.telefone}")
                if (!cliente.whatsapp.isNullOrBlank()) appendLine("WhatsApp: ${cliente.whatsapp}")
            }.trimEnd(),
            margin, y, contentWidth
        )

        // Atendimento
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Atendimento", margin, y)
        y = drawMultiline(
            canvas,
            buildString {
                appendLine("Entrada: ${sdf.format(Date(relatorio.dataEntrada))}")
                appendLine("Saída: ${sdf.format(Date(relatorio.dataSaida))}")
                appendLine("Modelo da máquina: ${relatorio.modeloMaquina}")
                append("Tipo de manutenção: ${relatorio.tipoManutencao}")
            },
            margin, y, contentWidth
        )

        // Ocorrência
        if (relatorio.ocorrencia.isNotBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Ocorrência", margin, y)
            y = drawMultiline(canvas, relatorio.ocorrencia, margin, y, contentWidth)
        }

        // Solução proposta
        if (relatorio.solucaoProposta.isNotBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Solução proposta", margin, y)
            y = drawMultiline(canvas, relatorio.solucaoProposta, margin, y, contentWidth)
        }

        // Peças
        if (!relatorio.pecasTexto.isNullOrBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Lista de peças", margin, y)
            y = drawMultiline(
                canvas,
                relatorio.pecasTexto.replace(";", "\n"),
                margin, y, contentWidth
            )
        }

        y += 16f
        y = drawMultiline(
            canvas,
            "Assinatura do responsável: ____________________________",
            margin, y, contentWidth
        )

        // Imagens
        if (imagens.isNotEmpty()) {
            if (y > bottomLimit - 200f) novaPagina()
            y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

            for (img in imagens) {
                val bitmap = carregarBitmapSegura(context, img.uri) ?: continue
                val scale = minOf(contentWidth / bitmap.width, 250f / bitmap.height)
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                if (y + drawHeight + 40 > pageHeight) {
                    novaPagina("Imagens do atendimento (cont.)")
                }

                val dest = RectF(margin, y + 12f, margin + drawWidth, y + 12f + drawHeight)
                canvas.drawBitmap(bitmap, null, dest, null)
                bitmap.recycle()
                y = dest.bottom + 16f
            }
        }

        pdf.finishPage(page)
        return salvarPdf(pdf, context, "relatorio_${relatorio.id}_${System.currentTimeMillis()}.pdf")
    }

    // ============================================================
    // 2) PDF COMPRESSOR (com checklist paginado)
    // ============================================================
    fun gerarPdfRelatorioCompressor(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>,
        logoUri: String? = null
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 40f
        val contentWidth = pageWidth - margin * 2
        val bottomLimit = pageHeight - 60f
        var y = 0f

        fun novaPagina(tituloExtra: String? = null) {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, pageWidth.toFloat(), margin, context, logoUri)
            if (!tituloExtra.isNullOrBlank()) {
                y = drawSectionTitle(canvas, tituloExtra, margin, y)
            }
        }

        y = drawHeader(canvas, pageWidth.toFloat(), margin, context, logoUri)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Cliente
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Cliente", margin, y)
        y = drawMultiline(
            canvas,
            buildString {
                appendLine("Nome fantasia: ${cliente.nomeFantasia}")
                if (cliente.razaoSocial.isNotEmpty()) appendLine("Razão social: ${cliente.razaoSocial}")
                if (!cliente.documento.isNullOrBlank()) appendLine("CNPJ/CPF: ${cliente.documento}")
                if (!cliente.endereco.isNullOrBlank()) appendLine("Endereço: ${cliente.endereco}")
                if (!cliente.email.isNullOrBlank()) appendLine("E-mail: ${cliente.email}")
                if (!cliente.telefone.isNullOrBlank()) appendLine("Telefone: ${cliente.telefone}")
                if (!cliente.whatsapp.isNullOrBlank()) appendLine("WhatsApp: ${cliente.whatsapp}")
            }.trimEnd(),
            margin, y, contentWidth
        )

        // Atendimento
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Atendimento – Compressor", margin, y)
        y = drawMultiline(
            canvas,
            buildString {
                appendLine("Entrada: ${sdf.format(Date(relatorio.dataEntrada))}")
                appendLine("Saída: ${sdf.format(Date(relatorio.dataSaida))}")
                appendLine("Modelo do compressor: ${relatorio.modeloMaquina}")
                append("Tipo de manutenção: ${relatorio.tipoManutencao}")
            },
            margin, y, contentWidth
        )

        // Checklist
        val checklistTexto = relatorio.checklistResumo ?: ""
        if (checklistTexto.isNotBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Checklist de inspeção", margin, y)
            for (linha in checklistTexto.split("\n").map { it.trim() }.filter { it.isNotEmpty() }) {
                if (y > bottomLimit) novaPagina("Checklist de inspeção (cont.)")
                y = drawMultiline(canvas, linha, margin, y, contentWidth)
            }
        }

        // Observações gerais
        val obs = relatorio.observacoes
        if (!obs.isNullOrBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Observações gerais", margin, y)
            y = drawMultiline(canvas, obs, margin, y, contentWidth)
        }

        y += 16f
        y = drawMultiline(
            canvas,
            "Assinatura do responsável: ____________________________",
            margin, y, contentWidth
        )

        // Imagens
        if (imagens.isNotEmpty()) {
            if (y > bottomLimit - 200f) novaPagina()
            y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

            for (img in imagens) {
                val bitmap = carregarBitmapSegura(context, img.uri) ?: continue
                val scale = minOf(contentWidth / bitmap.width, 250f / bitmap.height)
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                if (y + drawHeight + 40 > pageHeight) {
                    novaPagina("Imagens do atendimento (cont.)")
                }

                val dest = RectF(margin, y + 12f, margin + drawWidth, y + 12f + drawHeight)
                canvas.drawBitmap(bitmap, null, dest, null)
                bitmap.recycle()
                y = dest.bottom + 16f
            }
        }

        pdf.finishPage(page)
        return salvarPdf(pdf, context, "relatorio_compressor_${relatorio.id}_${System.currentTimeMillis()}.pdf")
    }

    // ============================================================
    // HELPERS PRIVADOS
    // ============================================================

    private fun salvarPdf(
        pdf: android.graphics.pdf.PdfDocument,
        context: Context,
        fileName: String
    ): File {
        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) {
            val criado = dir.mkdirs()
            if (!criado) Log.w(TAG, "Não foi possível criar o diretório: ${dir.absolutePath}")
        }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }

    private fun drawHeader(
        canvas: Canvas,
        pageWidth: Float,
        margin: Float,
        context: Context? = null,
        logoUri: String? = null
    ): Float {
        val headerHeight = 70f
        canvas.drawRect(
            0f, 0f, pageWidth, headerHeight,
            Paint().apply { color = Color.parseColor("#546E7A") }
        )

        // Tentar desenhar logo à esquerda
        var textStartX = margin
        if (context != null && !logoUri.isNullOrBlank()) {
            val logoBitmap = carregarBitmapSegura(context, logoUri)
            if (logoBitmap != null) {
                val logoSize = headerHeight - 16f
                val scale = minOf(logoSize / logoBitmap.width, logoSize / logoBitmap.height)
                val lw = logoBitmap.width * scale
                val lh = logoBitmap.height * scale
                val top = (headerHeight - lh) / 2f
                val dest = RectF(margin, top, margin + lw, top + lh)
                canvas.drawBitmap(logoBitmap, null, dest, null)
                logoBitmap.recycle()
                textStartX = margin + lw + 12f
            }
        }

        canvas.drawText(
            "RELATÓRIO TÉCNICO DE MANUTENÇÃO",
            textStartX,
            headerHeight / 2f + 6f,
            TextPaint().apply {
                color = Color.WHITE
                textSize = 18f
                isFakeBoldText = true
            }
        )
        return headerHeight + 24f
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, margin: Float, yStart: Float): Float {
        val top = yStart + 8f
        val bottom = top + 26f
        canvas.drawRect(
            margin, top, canvas.width - margin, bottom,
            Paint().apply { color = Color.parseColor("#EEEEEE") }
        )
        canvas.drawText(
            text, margin + 8f, bottom - 8f,
            TextPaint().apply {
                color = Color.parseColor("#333333")
                textSize = 13f
                isFakeBoldText = true
            }
        )
        return bottom + 8f
    }

    private fun drawMultiline(
        canvas: Canvas,
        text: String,
        margin: Float,
        yStart: Float,
        contentWidth: Float
    ): Float {
        if (text.isBlank()) return yStart + 8f
        val paint = TextPaint().apply {
            color = Color.parseColor("#222222")
            textSize = 12f
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()
        canvas.save()
        canvas.translate(margin, yStart)
        layout.draw(canvas)
        canvas.restore()
        return yStart + layout.height + 8f
    }

    private fun carregarBitmapSegura(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao carregar imagem do URI $uriStr", e)
            null
        }
    }
}
