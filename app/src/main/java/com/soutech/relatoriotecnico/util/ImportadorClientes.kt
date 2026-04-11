package com.soutech.relatoriotecnico.util

import android.content.Context
import android.net.Uri
import com.soutech.relatoriotecnico.data.ClienteEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Importa clientes a partir de arquivos CSV (ponto-e-vírgula) ou XLSX.
 *
 * Formato esperado das colunas (primeira linha = cabeçalho, ignorado):
 * Nome/Razão Social ; Apelido/Nome fantasia ; Tipo ; Sexo ; CPF ; RG ;
 * Expedição RG ; UF do RG ; Indicador IE ; CNPJ ; IE ; Telefone ; Celular ;
 * Fax ; Email ; Site ; Endereço ; Número ; Complemento ; Bairro ;
 * Cidade ; Estado ; CEP ; Data de nascimento
 *
 * Índices (0-based):
 *  0  – razão social
 *  1  – nome fantasia
 *  9  – CNPJ  (preferencial para documento)
 *  4  – CPF   (fallback para documento)
 * 11  – Telefone
 * 12  – Celular
 * 14  – Email
 * 16..22 – Endereço completo (rua, número, complemento, bairro, cidade, estado, CEP)
 */
object ImportadorClientes {

    fun importar(context: Context, uri: Uri): List<ClienteEntity> {
        val mime = context.contentResolver.getType(uri) ?: ""
        val name = uri.lastPathSegment?.lowercase() ?: ""
        return when {
            mime.contains("spreadsheet") || name.endsWith(".xlsx") ->
                importarXlsx(context, uri)
            else ->
                importarCsv(context, uri)
        }
    }

    // ----------------------------------------------------------------
    // CSV (semicolon-delimited, UTF-8)
    // ----------------------------------------------------------------
    private fun importarCsv(context: Context, uri: Uri): List<ClienteEntity> {
        val linhas = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readLines()
        } ?: return emptyList()

        return linhas
            .drop(1)                    // skip header row
            .filter { it.isNotBlank() }
            .mapNotNull { linha ->
                val cols = linha.split(";").map { it.trim().removeSurrounding("\"") }
                parsearColunas(cols)
            }
    }

    // ----------------------------------------------------------------
    // XLSX (ZIP → XML, no external dependencies)
    // ----------------------------------------------------------------
    private fun importarXlsx(context: Context, uri: Uri): List<ClienteEntity> {
        val stream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        return stream.use { parseXlsx(it) }
    }

    private fun parseXlsx(stream: InputStream): List<ClienteEntity> {
        val sharedStrings = mutableListOf<String>()
        var sheetXml: String? = null

        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> {
                        sharedStrings.addAll(parseSharedStrings(zip.readBytes()))
                    }
                    "xl/worksheets/sheet1.xml" -> {
                        sheetXml = String(zip.readBytes(), Charsets.UTF_8)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheet = sheetXml ?: return emptyList()
        val rows = parseSheet(sheet, sharedStrings)

        return rows
            .drop(1)                    // skip header row
            .filter { it.isNotEmpty() }
            .mapNotNull { parsearColunas(it) }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")

        var inT = false
        var sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") sb = StringBuilder()
                    if (parser.name == "t") inT = true
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") inT = false
                    if (parser.name == "si") result.add(sb.toString())
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheet(xml: String, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.byteInputStream(Charsets.UTF_8), "UTF-8")

        var currentRow = mutableListOf<String>()
        var cellType = ""
        var inV = false
        var cellValue = StringBuilder()
        var currentColIndex = 0
        var lastColIndex = -1

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> {
                        currentRow = mutableListOf()
                        lastColIndex = -1
                    }
                    "c" -> {
                        val ref = parser.getAttributeValue(null, "r") ?: ""
                        currentColIndex = colLetterToIndex(ref.filter { it.isLetter() })
                        // Fill gaps with empty strings
                        while (currentRow.size < currentColIndex) currentRow.add("")
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        cellValue = StringBuilder()
                        lastColIndex = currentColIndex
                    }
                    "v", "t" -> { inV = true }
                }
                XmlPullParser.TEXT -> if (inV) cellValue.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inV = false
                    "c" -> {
                        val raw = cellValue.toString()
                        val resolved = if (cellType == "s") {
                            sharedStrings.getOrElse(raw.toIntOrNull() ?: -1) { raw }
                        } else raw
                        currentRow.add(resolved.trim())
                    }
                    "row" -> rows.add(currentRow)
                }
            }
            event = parser.next()
        }
        return rows
    }

    /** "AB" → 27 (0-based) */
    private fun colLetterToIndex(letters: String): Int {
        var result = 0
        for (ch in letters.uppercase()) result = result * 26 + (ch - 'A' + 1)
        return result - 1
    }

    // ----------------------------------------------------------------
    // Column mapping
    // ----------------------------------------------------------------
    private fun parsearColunas(cols: List<String>): ClienteEntity? {
        val razaoSocial  = cols.getOrElse(0) { "" }
        val nomeFantasia = cols.getOrElse(1) { "" }.ifBlank { razaoSocial }

        if (nomeFantasia.isBlank()) return null   // skip empty rows

        val cnpj     = cols.getOrElse(9) { "" }
        val cpf      = cols.getOrElse(4) { "" }
        val documento = cnpj.ifBlank { cpf }.ifBlank { null }

        val telefone = cols.getOrElse(11) { "" }.ifBlank { null }
        val celular  = cols.getOrElse(12) { "" }.ifBlank { null }
        val email    = cols.getOrElse(14) { "" }.ifBlank { null }

        val rua          = cols.getOrElse(16) { "" }
        val numero       = cols.getOrElse(17) { "" }
        val complemento  = cols.getOrElse(18) { "" }
        val bairro       = cols.getOrElse(19) { "" }
        val cidade       = cols.getOrElse(20) { "" }
        val estado       = cols.getOrElse(21) { "" }
        val cep          = cols.getOrElse(22) { "" }

        val endereco = buildString {
            if (rua.isNotBlank()) append(rua)
            if (numero.isNotBlank()) append(", $numero")
            if (complemento.isNotBlank()) append(" – $complemento")
            if (bairro.isNotBlank()) append(", $bairro")
            if (cidade.isNotBlank()) append(", $cidade")
            if (estado.isNotBlank()) append("/$estado")
            if (cep.isNotBlank()) append(" – CEP $cep")
        }.ifBlank { null }

        return ClienteEntity(
            razaoSocial  = razaoSocial,
            nomeFantasia = nomeFantasia,
            documento    = documento,
            endereco     = endereco,
            email        = email,
            telefone     = telefone,
            whatsapp     = celular
        )
    }
}
