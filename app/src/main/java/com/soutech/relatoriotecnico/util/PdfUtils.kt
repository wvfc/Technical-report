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

    /**
     * Gera um PDF A4 bonito, com seções e imagens.
     *
     * @return File do PDF gerado.
     */
    fun gerarPdfRelatorio(
        context: Context,
        relatorio: RelatorioEntity,
        cliente: ClienteEntity,
        imagens: List<ImagemRelatorioEntity>
    ): File {
        val pdf = android.graphics.pdf.PdfDocument()
        val pageWidth = 595  // A4 em points (72 dpi) ~ 21cm
        val pageHeight = 842 // A4 em points ~ 29.7cm

        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, pageNumber)
            .create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 40f
        val contentWidth = pageWidth - margin * 2
        var y = 0f

        // ---------- CABEÇALHO ----------
        y = drawHeader(canvas, pageWidth.toFloat(), margin)

        // ---------- SEÇÕES DE TEXTO ----------
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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

        y = drawSectionTitle(canvas, "Ocorrência", margin, y)
        y = drawMultiline(
            canvas,
            relatorio.ocorrencia ?: "-",
            margin,
            y,
            contentWidth
        )

        y = drawSectionTitle(canvas, "Solução proposta", margin, y)
        y = drawMultiline(
            canvas,
            relatorio.solucaoProposta ?: "-",
            margin,
            y,
            contentWidth
        )

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
            // Se faltar espaço na página atual, vai para outra
            if (y > pageHeight - 200) {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                    .Builder(pageWidth, pageHeight, pageNumber)
                    .create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = drawHeader(canvas, pageWidth.toFloat(), margin)
            }

            y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

            val maxImageWidth = contentWidth
            val maxImageHeight = 250f

            for (img in imagens) {
                val uriString = img.uri
                if (uriString.isNullOrBlank()) continue

                val uri = try {
                    Uri.parse(uriString)
                } catch (e: Exception) {
                    Log.w(TAG, "URI inválida: $uriString", e)
                    continue
                }

                val input = try {
                    context.contentResolver.openInputStream(uri)
                } catch (e: Exception) {
                    Log.w(TAG, "Não foi possível abrir InputStream da URI: $uriString", e)
                    null
                }

                if (input == null) {
                    Log.w(TAG, "InputStream nulo para URI: $uriString")
                    continue
                }

                val bitmap = try {
                    BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    Log.w(TAG, "Falha ao decodificar bitmap da URI: $uriString", e)
                    null
                } finally {
                    try {
                        input.close()
                    } catch (_: Exception) { }
                }

                if (bitmap == null) {
                    Log.w(TAG, "Bitmap nulo para URI: $uriString")
                    continue
                }

                val scale = minOf(
                    maxImageWidth / bitmap.width,
                    maxImageHeight / bitmap.height
                )

                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                // quebra de página se não couber
                if (y + drawHeight + 40 > pageHeight) {
                    pdf.finishPage(page)
                    pageNumber++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                        .Builder(pageWidth, pageHeight, pageNumber)
                        .create()
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas
                    y = drawHeader(canvas, pageWidth.toFloat(), margin)
                    y = drawSectionTitle(canvas, "Imagens do atendimento (cont.)", margin, y)
                }

                val left = margin
                val top = y + 12f

                val dest = RectF(left, top, left + drawWidth, top + drawHeight)
                canvas.drawBitmap(bitmap, null, dest, null)
                bitmap.recycle()

                y = dest.bottom + 16f
            }
        }

        // ---------- FINALIZAÇÃO ----------
        pdf.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "relatorios")
        if (!dir.exists()) dir.mkdirs()

        val fileName = "relatorio_${relatorio.id}_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)

        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()

        return file
    }

    // ===== helpers =====

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
}


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
    var y = 0f

    // Cabeçalho vinho (pode reaproveitar drawHeader)
    y = drawHeader(canvas, pageWidth.toFloat(), margin)

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Dados cliente
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

    // Dados do atendimento
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

    // Checklist (texto montado na Activity, salvo em ocorrencia)
    y = drawSectionTitle(canvas, "Checklist de Inspeção", margin, y)
    y = drawMultiline(
        canvas,
        relatorio.ocorrencia ?: "-",
        margin,
        y,
        contentWidth
    )

    // Observações (salvas em solucaoProposta)
    y = drawSectionTitle(canvas, "Observações gerais", margin, y)
    y = drawMultiline(
        canvas,
        relatorio.solucaoProposta ?: "-",
        margin,
        y,
        contentWidth
    )

    // Imagens (igual padrão geral)
    if (imagens.isNotEmpty()) {
        if (y > pageHeight - 200) {
            pdf.finishPage(page)
            pageNumber++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber)
                .create()
            page = pdf.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, pageWidth.toFloat(), margin)
        }

        y = drawSectionTitle(canvas, "Imagens do atendimento", margin, y)

        val maxImageWidth = contentWidth
        val maxImageHeight = 250f

        for (img in imagens) {
            val path = img.caminho
            if (path.isNullOrBlank()) continue

            val bitmap = BitmapFactory.decodeFile(path) ?: continue

            val scale = minOf(
                maxImageWidth / bitmap.width,
                maxImageHeight / bitmap.height
            )
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale

            if (y + drawHeight + 40 > pageHeight) {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo
                    .Builder(pageWidth, pageHeight, pageNumber)
                    .create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = drawHeader(canvas, pageWidth.toFloat(), margin)
                y = drawSectionTitle(canvas, "Imagens do atendimento (cont.)", margin, y)
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