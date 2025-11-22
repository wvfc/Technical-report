package com.soutech.relatoriotecnico.data

import androidx.room.*

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nomeFantasia")
    suspend fun listarTodos(): List<ClienteEntity>

    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Long): ClienteEntity?

    @Insert
    suspend fun inserir(cliente: ClienteEntity): Long

    @Update
    suspend fun atualizar(cliente: ClienteEntity)

    @Delete
    suspend fun deletar(cliente: ClienteEntity)
}

data class RelatorioComCliente(
    @Embedded val relatorio: RelatorioEntity,
    @Relation(
        parentColumn = "clienteId",
        entityColumn = "id"
    )
    val cliente: ClienteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "relatorioId"
    )
    val imagens: List<ImagemRelatorioEntity>
)

@Dao
interface RelatorioDao {
    @Transaction
    @Query("SELECT * FROM relatorios ORDER BY dataEntrada DESC")
    suspend fun listarComCliente(): List<RelatorioComCliente>

    @Transaction
    @Query("SELECT * FROM relatorios WHERE id = :id LIMIT 1")
    suspend fun buscarComCliente(id: Long): RelatorioComCliente?

    @Insert
    suspend fun inserir(relatorio: RelatorioEntity): Long

    @Update
    suspend fun atualizar(relatorio: RelatorioEntity)

    @Delete
    suspend fun deletar(relatorio: RelatorioEntity)

    @Query("SELECT * FROM relatorios WHERE id = :id LIMIT 1")
    suspend fun buscarPorIdSimples(id: Long): RelatorioEntity?
}

@Dao
interface ImagemRelatorioDao {
    @Query("SELECT * FROM imagens_relatorio WHERE relatorioId = :relatorioId ORDER BY ordem")
    suspend fun listarPorRelatorio(relatorioId: Long): List<ImagemRelatorioEntity>

    @Insert
    suspend fun inserir(imagem: ImagemRelatorioEntity): Long

    @Insert
    suspend fun inserirLista(imagens: List<ImagemRelatorioEntity>)

    @Query("DELETE FROM imagens_relatorio WHERE relatorioId = :relatorioId")
    suspend fun deletarPorRelatorio(relatorioId: Long)
}

@Dao
interface ConfigLogoDao {
    @Query("SELECT * FROM config_logo WHERE id = 1 LIMIT 1")
    suspend fun obterConfig(): ConfigLogoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(config: ConfigLogoEntity)
}
