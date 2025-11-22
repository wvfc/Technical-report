package com.soutech.relatoriotecnico.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val razaoSocial: String,
    val nomeFantasia: String,
    val documento: String?,
    val endereco: String?,
    val email: String?,
    val telefone: String?,
    val whatsapp: String?
)

@Entity(tableName = "relatorios")
data class RelatorioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val dataEntrada: Long,
    val dataSaida: Long,
    val modeloMaquina: String,
    val tipoManutencao: String,
    val ocorrencia: String,
    val solucaoProposta: String,
    val pecasTexto: String?,
    val pdfPath: String?
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
