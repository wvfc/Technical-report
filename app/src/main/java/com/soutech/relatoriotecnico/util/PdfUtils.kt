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
        imagens: List<ImagemRelatorioEntity>
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595  // A4 (72 dpi)
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, pageNumber)
            .create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 40f
        val contentWidth = pageWidth - margin * 2
        val bottomLimit = pageHeight - 60f
        var y = 0f

        // ---------- CABEÇALHO ----------
        y = drawHeader(canvas, pageWidth.toFloat(), margin)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun novaPagina(tituloExtra: String? = null) {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber)
                .create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, pageWidth.toFloat(), margin)
            if (!tituloExtra.isNullOrBlank()) {
                y = drawSectionTitle(canvas, tituloExtra, margin, y)
            }
        }

        // ---------- CLIENTE ----------
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Cliente", margin, y)
        y = drawMultiline(
            canvas,
            """
            Nome fantasia: ${cliente.nomeFantasia}
            Razão social: ${cliente.razaoSocial ?: "-"}
            CNPJ/CPF: ${cliente.documento ?: "-"}
            Endereço: ${cliente.endereco ?: "-"}
            E-mail: ${cliente.email ?: "-"}
            Telefone: ${cliente.telefone ?: "-"}
            WhatsApp: ${cliente.whatsapp ?: "-"}
            """.trimIndent(),
            margin,
            y,
            contentWidth
        )

        // ---------- ATENDIMENTO ----------
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Atendimento", margin, y)
        y = drawMultiline(
            canvas,
            """
            Entrada: ${sdf.format(relatorio.dataEntrada)}
            Saída: ${sdf.format(relatorio.dataSaida)}
            Modelo da máquina: ${relatorio.modeloMaquina}
            Tipo de manutenção: ${relatorio.tipoManutencao}
            """.trimIndent(),
            margin,
            y,
            contentWidth
        )

        // ---------- OCORRÊNCIA ----------
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Ocorrência", margin, y)
        y = drawMultiline(
            canvas,
            relatorio.ocorrencia ?: "-",
            margin,
            y,
            contentWidth
        )

        // ---------- SOLUÇÃO ----------
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Solução proposta", margin, y)
        y = drawMultiline(
            canvas,
            relatorio.solucaoProposta ?: "-",
            margin,
            y,
            contentWidth
        )

        // ---------- PEÇAS ----------
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Lista de peças", margin, y)
        y = drawMultiline(
            canvas,
            (relatorio.pecasTexto ?: "-").replace(";", "\n"),
            margin,
            y,
            contentWidth
        )

        y += 16f
        y = drawMultiline(
            canvas,
            "Assinatura do responsável: ____________________________",
            margin,
            y,
            contentWidth
        )

        // ---------- IMAGENS ----------
        if (imagens.isNotEmpty()) {
            if (y > bottomLimit - 200f) {
                novaPagina()
            }

            y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

            val maxImageWidth = contentWidth
            val maxImageHeight = 250f

            for (img in imagens) {
                val bitmap = carregarBitmapSegura(context, img.uri) ?: continue

                val scale = minOf(
                    maxImageWidth / bitmap.width,
                    maxImageHeight / bitmap.height
                )

                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                if (y + drawHeight + 40 > pageHeight) {
                    novaPagina("Imagens do atendimento (cont.)")
                }

                val left = margin
                val top = y + 12f
                val dest = RectF(left, top, left + drawWidth, top + drawHeight)
                canvas.drawBitmap(bitmap, null, dest, null)
                bitmap.recycle()

                y = dest.bottom + 16f
            }
        }

        pdf.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "relatorio_${relatorio.id}_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        return file
    }

    // ============================================================
    // 2) PDF ESPECÍFICO – COMPRESSOR (com checklist paginado)
    // ============================================================
    fun gerarPdfRelatorioCompressor(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, pageNumber)
            .create()
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
                .Builder(pageWidth, pageHeight, pageNumber)
                .create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, pageWidth.toFloat(), margin)
            if (!tituloExtra.isNullOrBlank()) {
                y = drawSectionTitle(canvas, tituloExtra, margin, y)
            }
        }

        // Cabeçalho padrão SOUTECH
        y = drawHeader(canvas, pageWidth.toFloat(), margin)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // ===== Dados do cliente =====
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Cliente", margin, y)
        y = drawMultiline(
            canvas,
            """
            Nome fantasia: ${cliente.nomeFantasia}
            Razão social: ${cliente.razaoSocial ?: "-"}
            CNPJ/CPF: ${cliente.documento ?: "-"}
            Endereço: ${cliente.endereco ?: "-"}
            E-mail: ${cliente.email ?: "-"}
            Telefone: ${cliente.telefone ?: "-"}
            WhatsApp: ${cliente.whatsapp ?: "-"}
            """.trimIndent(),
            margin,
            y,
            contentWidth
        )

        // ===== Dados do atendimento – Compressor =====
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Dados do Atendimento – Compressor", margin, y)
        y = drawMultiline(
            canvas,
            """
            Entrada: ${sdf.format(relatorio.dataEntrada)}
            Saída: ${sdf.format(relatorio.dataSaida)}
            Modelo do compressor: ${relatorio.modeloMaquina}
            Tipo de manutenção: ${relatorio.tipoManutencao}
            """.trimIndent(),
            margin,
            y,
            contentWidth
        )

        // ===== Checklist de inspeção (PAGINADO) =====
        if (y > bottomLimit) novaPagina()
        y = drawSectionTitle(canvas, "Checklist de inspeção", margin, y)

        val linhasChecklist = (relatorio.ocorrencia ?: "")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (linhaTexto in linhasChecklist) {
            if (y > bottomLimit) {
                novaPagina("Checklist de inspeção (cont.)")
            }
            y = drawMultiline(canvas, linhaTexto, margin, y, contentWidth)
        }

        // ===== Observações gerais =====
        if (!relatorio.solucaoProposta.isNullOrBlank()) {
            if (y > bottomLimit) novaPagina()
            y = drawSectionTitle(canvas, "Observações gerais", margin, y)
            y = drawMultiline(
                canvas,
                relatorio.solucaoProposta ?: "-",
                margin,
                y,
                contentWidth
            )
        }

        // ===== Imagens =====
        if (imagens.isNotEmpty()) {
            if (y > bottomLimit - 200f) {
                novaPagina()
            }

            y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

            val maxImageWidth = contentWidth
            val maxImageHeight = 250f

            for (img in imagens) {
                val bitmap = carregarBitmapSegura(context, img.uri) ?: continue

                val scale = minOf(
                    maxImageWidth / bitmap.width,
                    maxImageHeight / bitmap.height
                )
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                if (y + drawHeight + 40 > pageHeight) {
                    novaPagina("Imagens do atendimento (cont.)")
                }

                val left = margin
                val top = y + 12f
                val dest = RectF(left, top, left + drawWidth, top + drawHeight)
                canvas.drawBitmap(bitmap, null, dest, null)
                bitmap.recycle()

                y = dest.bottom + 16f
            }
        }

        pdf.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) dir.mkdirs()
        val fileName = "relatorio_compressor_${relatorio.id}_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()

        return file
    }

    // ============================================================
    // HELPERS COMUNS
    // ============================================================

    private fun drawHeader(canvas: Canvas, pageWidth: Float, margin: Float): Float {
        val headerHeight = 70f

        val headerPaint = Paint().apply {
            color = Color.parseColor("#45132D") // vinho SOUTECH
        }
        canvas.drawRect(0f, 0f, pageWidth, headerHeight, headerPaint)

        val titlePaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText(
            "RELATÓRIO TÉCNICO DE MANUTENÇÃO",
            margin,
            headerHeight / 2f + 6f,
            titlePaint
        )

        return headerHeight + 24f
    }

    private fun drawSectionTitle(
        canvas: Canvas,
        text: String,
        margin: Float,
        yStart: Float
    ): Float {
        val paintBg = Paint().apply {
            color = Color.parseColor("#EEEEEE")
        }
        val paintText = TextPaint().apply {
            color = Color.parseColor("#333333")
            textSize = 13f
            isFakeBoldText = true
        }

        val top = yStart + 8f
        val bottom = top + 26f

        canvas.drawRect(margin, top, canvas.width - margin, bottom, paintBg)
        canvas.drawText(text, margin + 8f, bottom - 8f, paintText)

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

    /**
     * Carrega bitmap a partir do campo uri da entidade, tratando content:// e file://.
     */
    private fun carregarBitmapSegura(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao carregar imagem do URI $uriStr", e)
            null
        }
    }
}