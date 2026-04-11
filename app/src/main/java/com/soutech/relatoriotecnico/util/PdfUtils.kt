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

    // A4 dimensions at 72 dpi
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private val CONTENT_W = PAGE_W - MARGIN * 2
    private const val BOTTOM_LIMIT = PAGE_H - 60f

    // Brand colors
    private const val COLOR_PRIMARY   = 0xFF1565C0.toInt()  // Blue 800
    private const val COLOR_ACCENT    = 0xFF0D47A1.toInt()  // Blue 900 (for group sub-headers)
    private const val COLOR_SECTION_BG= 0xFFE3F2FD.toInt()  // Blue 50
    private const val COLOR_ROW_ALT   = 0xFFF5F5F5.toInt()  // Gray 100
    private const val COLOR_DIVIDER   = 0xFFBBDEFB.toInt()  // Blue 100
    private const val COLOR_TEXT      = 0xFF212121.toInt()   // Gray 900
    private const val COLOR_TEXT_SEC  = 0xFF616161.toInt()   // Gray 700
    private const val COLOR_WHITE     = 0xFFFFFFFF.toInt()
    private const val COLOR_GROUP_BG  = 0xFF37474F.toInt()   // Blue-Grey 800

    // ============================================================
    // 1) PDF GERAL
    // ============================================================
    fun gerarPdfRelatorio(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>,
        logoUri: String? = null
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        var pageNum = 1
        var pageInfo = newPageInfo(pageNum)
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        var y = drawHeader(canvas, context, logoUri)

        fun novaPagina(subtitulo: String? = null) {
            pdf.finishPage(page)
            pageNum++
            pageInfo = newPageInfo(pageNum)
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, context, logoUri)
            drawPageNumber(canvas, pageNum)
            if (!subtitulo.isNullOrBlank()) y = drawSectionTitle(canvas, subtitulo, y)
        }

        // Dados do Cliente
        if (y > BOTTOM_LIMIT) novaPagina()
        y = drawSectionTitle(canvas, "DADOS DO CLIENTE", y)
        y = drawInfoGrid(canvas, buildList {
            add("Nome fantasia" to cliente.nomeFantasia)
            if (cliente.razaoSocial.isNotEmpty()) add("Razão social" to cliente.razaoSocial)
            if (!cliente.documento.isNullOrBlank()) add("CNPJ/CPF" to cliente.documento)
            if (!cliente.endereco.isNullOrBlank()) add("Endereço" to cliente.endereco)
            if (!cliente.email.isNullOrBlank()) add("E-mail" to cliente.email)
            if (!cliente.telefone.isNullOrBlank()) add("Telefone" to cliente.telefone)
            if (!cliente.whatsapp.isNullOrBlank()) add("WhatsApp" to cliente.whatsapp)
        }, y)

        // Dados do Atendimento
        if (y > BOTTOM_LIMIT) novaPagina()
        y = drawSectionTitle(canvas, "DADOS DO ATENDIMENTO", y)
        y = drawInfoGrid(canvas, listOf(
            "Entrada" to sdf.format(Date(relatorio.dataEntrada)),
            "Saída" to sdf.format(Date(relatorio.dataSaida)),
            "Modelo da máquina" to relatorio.modeloMaquina,
            "Tipo de manutenção" to relatorio.tipoManutencao
        ), y)

        // Ocorrência
        if (relatorio.ocorrencia.isNotBlank()) {
            if (y > BOTTOM_LIMIT) novaPagina()
            y = drawSectionTitle(canvas, "OCORRÊNCIA", y)
            y = drawBodyText(canvas, relatorio.ocorrencia, y)
        }

        // Solução proposta
        if (relatorio.solucaoProposta.isNotBlank()) {
            if (y > BOTTOM_LIMIT) novaPagina()
            y = drawSectionTitle(canvas, "SOLUÇÃO PROPOSTA", y)
            y = drawBodyText(canvas, relatorio.solucaoProposta, y)
        }

        // Peças
        if (!relatorio.pecasTexto.isNullOrBlank()) {
            if (y > BOTTOM_LIMIT) novaPagina()
            y = drawSectionTitle(canvas, "LISTA DE PEÇAS", y)
            y = drawBodyText(canvas, relatorio.pecasTexto.replace(";", "\n"), y)
        }

        y += 20f
        y = drawAssinaturaLine(canvas, y)

        // Imagens
        if (imagens.isNotEmpty()) {
            if (y > BOTTOM_LIMIT - 200f) novaPagina()
            y = drawSectionTitle(canvas, "IMAGENS DO ATENDIMENTO", y)
            for (img in imagens) {
                val bmp = carregarBitmapHQ(context, img.uri) ?: continue
                y = drawImage(pdf, bmp, canvas, y) { newCanvas, newY ->
                    canvas = newCanvas
                    y = newY
                    novaPagina("IMAGENS DO ATENDIMENTO (cont.)")
                }
                bmp.recycle()
            }
        }

        drawPageNumber(canvas, pageNum)
        pdf.finishPage(page)
        return salvarPdf(pdf, context, "relatorio_${relatorio.id}_${System.currentTimeMillis()}.pdf")
    }

    // ============================================================
    // 2) PDF COMPRESSOR
    // ============================================================
    fun gerarPdfRelatorioCompressor(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>,
        logoUri: String? = null
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        var pageNum = 1
        var pageInfo = newPageInfo(pageNum)
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        var y = drawHeader(canvas, context, logoUri)

        fun novaPagina(subtitulo: String? = null) {
            pdf.finishPage(page)
            pageNum++
            pageInfo = newPageInfo(pageNum)
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, context, logoUri)
            drawPageNumber(canvas, pageNum)
            if (!subtitulo.isNullOrBlank()) y = drawSectionTitle(canvas, subtitulo, y)
        }

        // Cliente
        if (y > BOTTOM_LIMIT) novaPagina()
        y = drawSectionTitle(canvas, "DADOS DO CLIENTE", y)
        y = drawInfoGrid(canvas, buildList {
            add("Nome fantasia" to cliente.nomeFantasia)
            if (cliente.razaoSocial.isNotEmpty()) add("Razão social" to cliente.razaoSocial)
            if (!cliente.documento.isNullOrBlank()) add("CNPJ/CPF" to cliente.documento)
            if (!cliente.endereco.isNullOrBlank()) add("Endereço" to cliente.endereco)
            if (!cliente.email.isNullOrBlank()) add("E-mail" to cliente.email)
            if (!cliente.telefone.isNullOrBlank()) add("Telefone" to cliente.telefone)
        }, y)

        // Atendimento
        if (y > BOTTOM_LIMIT) novaPagina()
        y = drawSectionTitle(canvas, "DADOS DO ATENDIMENTO – COMPRESSOR", y)
        y = drawInfoGrid(canvas, listOf(
            "Entrada" to sdf.format(Date(relatorio.dataEntrada)),
            "Saída" to sdf.format(Date(relatorio.dataSaida)),
            "Modelo do compressor" to relatorio.modeloMaquina,
            "Tipo de manutenção" to relatorio.tipoManutencao
        ), y)

        // Checklist com grupos
        val checklistTexto = relatorio.checklistResumo ?: ""
        if (checklistTexto.isNotBlank()) {
            if (y > BOTTOM_LIMIT) novaPagina()
            y = drawSectionTitle(canvas, "CHECKLIST DE INSPEÇÃO", y)

            var rowIndex = 0
            for (linha in checklistTexto.split("\n").map { it.trim() }.filter { it.isNotEmpty() }) {
                if (linha.startsWith("===") && linha.endsWith("===")) {
                    // Group header
                    val groupName = linha.removePrefix("===").removeSuffix("===").trim()
                    if (y + 28f > BOTTOM_LIMIT) novaPagina("CHECKLIST (cont.)")
                    y = drawGroupHeader(canvas, groupName, y)
                    rowIndex = 0
                } else {
                    if (y + 22f > BOTTOM_LIMIT) novaPagina("CHECKLIST (cont.)")
                    y = drawChecklistRow(canvas, linha, rowIndex, y)
                    rowIndex++
                }
            }
        }

        // Observações
        val obs = relatorio.observacoes
        if (!obs.isNullOrBlank()) {
            if (y > BOTTOM_LIMIT) novaPagina()
            y = drawSectionTitle(canvas, "OBSERVAÇÕES GERAIS", y)
            y = drawBodyText(canvas, obs, y)
        }

        y += 20f
        y = drawAssinaturaLine(canvas, y)

        // Imagens
        if (imagens.isNotEmpty()) {
            if (y > BOTTOM_LIMIT - 200f) novaPagina()
            y = drawSectionTitle(canvas, "IMAGENS DO ATENDIMENTO", y)
            for (img in imagens) {
                val bmp = carregarBitmapHQ(context, img.uri) ?: continue
                y = drawImage(pdf, bmp, canvas, y) { newCanvas, newY ->
                    canvas = newCanvas
                    y = newY
                    novaPagina("IMAGENS DO ATENDIMENTO (cont.)")
                }
                bmp.recycle()
            }
        }

        drawPageNumber(canvas, pageNum)
        pdf.finishPage(page)
        return salvarPdf(pdf, context, "relatorio_compressor_${relatorio.id}_${System.currentTimeMillis()}.pdf")
    }

    // ============================================================
    // DRAWING HELPERS
    // ============================================================

    private fun newPageInfo(num: Int) =
        android.graphics.pdf.PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, num).create()

    private fun drawHeader(canvas: Canvas, context: Context?, logoUri: String?): Float {
        val headerH = 76f
        // Background gradient simulation: draw two rects
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), headerH, Paint().apply { color = COLOR_PRIMARY })
        // Accent bar at bottom of header
        canvas.drawRect(0f, headerH - 4f, PAGE_W.toFloat(), headerH, Paint().apply {
            color = Color.parseColor("#FF8F00") // colorAccent
        })

        var textX = MARGIN
        if (context != null && !logoUri.isNullOrBlank()) {
            val logoBmp = carregarBitmapHQ(context, logoUri)
            if (logoBmp != null) {
                val logoH = headerH - 20f
                val scale = minOf(logoH / logoBmp.height, 100f / logoBmp.width)
                val lw = logoBmp.width * scale
                val lh = logoBmp.height * scale
                val top = (headerH - 4f - lh) / 2f
                val dest = RectF(MARGIN, top, MARGIN + lw, top + lh)
                canvas.drawBitmap(logoBmp, null, dest, hqPaint())
                logoBmp.recycle()
                textX = MARGIN + lw + 12f
            }
        }

        canvas.drawText(
            "RELATÓRIO TÉCNICO DE MANUTENÇÃO",
            textX,
            headerH / 2f + 4f,
            TextPaint().apply {
                color = COLOR_WHITE
                textSize = 17f
                isFakeBoldText = true
                isAntiAlias = true
            }
        )
        return headerH + 18f
    }

    private fun drawSectionTitle(canvas: Canvas, text: String, yStart: Float): Float {
        val top = yStart + 6f
        val bottom = top + 24f
        // Filled background
        canvas.drawRect(MARGIN, top, PAGE_W - MARGIN, bottom, Paint().apply {
            color = COLOR_PRIMARY
        })
        // Left accent bar
        canvas.drawRect(MARGIN, top, MARGIN + 4f, bottom, Paint().apply {
            color = Color.parseColor("#FF8F00")
        })
        canvas.drawText(
            text,
            MARGIN + 12f,
            bottom - 7f,
            TextPaint().apply {
                color = COLOR_WHITE
                textSize = 11f
                isFakeBoldText = true
                isAntiAlias = true
                letterSpacing = 0.08f
            }
        )
        return bottom + 6f
    }

    private fun drawGroupHeader(canvas: Canvas, text: String, yStart: Float): Float {
        val top = yStart + 4f
        val bottom = top + 20f
        canvas.drawRect(MARGIN, top, PAGE_W - MARGIN, bottom, Paint().apply {
            color = COLOR_GROUP_BG
        })
        canvas.drawText(
            "  $text",
            MARGIN + 8f,
            bottom - 5f,
            TextPaint().apply {
                color = COLOR_WHITE
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
                letterSpacing = 0.06f
            }
        )
        return bottom + 4f
    }

    private fun drawInfoGrid(canvas: Canvas, rows: List<Pair<String, String>>, yStart: Float): Float {
        var y = yStart
        val labelPaint = TextPaint().apply {
            color = COLOR_TEXT_SEC; textSize = 11f; isAntiAlias = true
        }
        val valuePaint = TextPaint().apply {
            color = COLOR_TEXT; textSize = 11f; isFakeBoldText = true; isAntiAlias = true
        }
        val rowH = 18f
        rows.forEachIndexed { i, (label, value) ->
            if (i % 2 == 0) {
                canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + rowH, Paint().apply {
                    color = COLOR_ROW_ALT
                })
            }
            canvas.drawText(label, MARGIN + 6f, y + rowH - 5f, labelPaint)
            val valueX = MARGIN + CONTENT_W * 0.42f
            // Wrap value text if too long
            val availW = (CONTENT_W * 0.56f).toInt()
            val sl = StaticLayout.Builder.obtain(value, 0, value.length, valuePaint, availW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
            canvas.save()
            canvas.translate(valueX, y + 4f)
            sl.draw(canvas)
            canvas.restore()
            y += maxOf(rowH, sl.height.toFloat() + 6f)
        }
        // Bottom divider
        canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1f, Paint().apply { color = COLOR_DIVIDER })
        return y + 8f
    }

    private fun drawChecklistRow(canvas: Canvas, text: String, rowIndex: Int, yStart: Float): Float {
        val parts = text.split("|").map { it.trim() }
        val descricao = parts.getOrElse(0) { text }
        val status = parts.getOrElse(1) { "" }.removePrefix("Status: ").trim()
        val valores = parts.drop(2).joinToString(" | ")

        val rowH = 18f
        if (rowIndex % 2 == 0) {
            canvas.drawRect(MARGIN, yStart, PAGE_W - MARGIN, yStart + rowH, Paint().apply {
                color = COLOR_ROW_ALT
            })
        }

        val descPaint = TextPaint().apply {
            color = COLOR_TEXT; textSize = 10f; isAntiAlias = true
        }
        val statusColor = when {
            status.contains("reparo", ignoreCase = true) || status.contains("limpeza", ignoreCase = true) ->
                Color.parseColor("#C62828")
            status.contains("Reparado", ignoreCase = true) ->
                Color.parseColor("#2E7D32")
            else -> Color.parseColor("#1565C0")
        }
        val statusPaint = TextPaint().apply {
            color = statusColor; textSize = 9f; isFakeBoldText = true; isAntiAlias = true
        }

        val colDesc = CONTENT_W * 0.50f
        val colStatus = CONTENT_W * 0.28f

        val descSl = StaticLayout.Builder.obtain(descricao, 0, descricao.length, descPaint, colDesc.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        canvas.save()
        canvas.translate(MARGIN + 4f, yStart + 3f)
        descSl.draw(canvas)
        canvas.restore()

        canvas.drawText(status, MARGIN + colDesc + 8f, yStart + rowH - 5f, statusPaint)

        if (valores.isNotEmpty()) {
            val valPaint = TextPaint().apply {
                color = COLOR_TEXT_SEC; textSize = 9f; isAntiAlias = true
            }
            canvas.drawText(valores, MARGIN + colDesc + colStatus + 12f, yStart + rowH - 5f, valPaint)
        }

        val actualH = maxOf(rowH, descSl.height.toFloat() + 6f)
        return yStart + actualH
    }

    private fun drawBodyText(canvas: Canvas, text: String, yStart: Float): Float {
        if (text.isBlank()) return yStart + 4f
        val paint = TextPaint().apply {
            color = COLOR_TEXT; textSize = 11f; isAntiAlias = true
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, CONTENT_W.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.2f)
            .build()
        canvas.save()
        canvas.translate(MARGIN, yStart + 4f)
        layout.draw(canvas)
        canvas.restore()
        return yStart + 4f + layout.height + 10f
    }

    private fun drawAssinaturaLine(canvas: Canvas, yStart: Float): Float {
        val paint = Paint().apply { color = COLOR_TEXT; strokeWidth = 1f }
        val lineY = yStart + 24f
        canvas.drawLine(MARGIN, lineY, MARGIN + 180f, lineY, paint)
        canvas.drawText(
            "Assinatura do técnico responsável",
            MARGIN, lineY + 14f,
            TextPaint().apply { color = COLOR_TEXT_SEC; textSize = 9f; isAntiAlias = true }
        )
        return lineY + 24f
    }

    private fun drawPageNumber(canvas: Canvas, pageNum: Int) {
        canvas.drawText(
            "Página $pageNum",
            (PAGE_W - MARGIN),
            (PAGE_H - 20f),
            TextPaint().apply {
                color = COLOR_TEXT_SEC
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
        )
    }

    /** Draw image with high-quality paint; calls [onNewPage] if page break needed. Returns new y. */
    private fun drawImage(
        pdf: android.graphics.pdf.PdfDocument,
        bmp: Bitmap,
        canvas: Canvas,
        yStart: Float,
        onNewPage: (Canvas, Float) -> Unit
    ): Float {
        // Max image height 360px in PDF space; keep aspect ratio
        val maxW = CONTENT_W
        val maxH = 360f
        val scale = minOf(maxW / bmp.width, maxH / bmp.height, 1f)
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale

        var y = yStart
        if (y + drawH + 16f > BOTTOM_LIMIT) {
            onNewPage(canvas, y)
            // y is updated by onNewPage via the closure capturing y
        }

        val dest = RectF(MARGIN, y + 8f, MARGIN + drawW, y + 8f + drawH)
        canvas.drawBitmap(bmp, null, dest, hqPaint())
        // Thin border around image
        canvas.drawRect(dest, Paint().apply {
            style = Paint.Style.STROKE
            color = COLOR_DIVIDER
            strokeWidth = 1f
        })
        return dest.bottom + 12f
    }

    private fun hqPaint() = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
        isDither = true
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun salvarPdf(
        pdf: android.graphics.pdf.PdfDocument,
        context: Context,
        fileName: String
    ): File {
        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> pdf.writeTo(out) }
        pdf.close()
        return file
    }

    /**
     * Loads a bitmap from URI at the highest available quality.
     * Decodes at full resolution without sub-sampling.
     */
    private fun carregarBitmapHQ(context: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1  // full resolution
                }
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao carregar imagem do URI $uriStr", e)
            null
        }
    }
}
