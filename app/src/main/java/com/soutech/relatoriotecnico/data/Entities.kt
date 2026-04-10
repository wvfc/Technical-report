package com.soutech.relatoriotecnico.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val razaoSocial: String = "",
    val nomeFantasia: String,
    val documento: String?,
    val endereco: String?,
    val email: String?,
    val telefone: String?,
    val whatsapp: String?
)

@Entity(tableName = "maquinas")
data class MaquinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val marca: String,
    val modelo: String,
    val modeloIhm: String?,
    val numeroSerie: String,
    val fotoPlaquetaUri: String?,
    val fotoCompressorUri: String?
)

@Entity(tableName = "tecnicos")
data class TecnicoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val funcao: String?
)

@Entity(tableName = "relatorios")
data class RelatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val maquinaId: Long? = null,
    val tecnicoId: Long? = null,
    val dataEntrada: Long,
    val dataSaida: Long,
    val modeloMaquina: String,
    val tipoManutencao: String,
    val tipoRelatorio: String = "geral",
    // Campos do relatório geral
    val ocorrencia: String = "",
    val solucaoProposta: String = "",
    val pecasTexto: String? = null,
    // Campos do relatório de compressor
    val observacoes: String? = null,
    val checklistResumo: String? = null,
    val pdfPath: String? = null
)

@Entity(tableName = "imagens_relatorio")
data class ImagemRelatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relatorioId: Long,
    val uri: String,
    val ordem: Int = 0
)

@Entity(tableName = "config_logo")
data class ConfigLogoEntity(
    @PrimaryKey val id: Int = 1,
    val logoUri: String?
)
