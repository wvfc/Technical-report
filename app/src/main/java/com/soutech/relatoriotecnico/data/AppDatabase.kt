package com.soutech.relatoriotecnico.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClienteEntity::class,
        MaquinaEntity::class,
        TecnicoEntity::class,
        RelatorioEntity::class,
        ImagemRelatorioEntity::class,
        ConfigLogoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun maquinaDao(): MaquinaDao
    abstract fun tecnicoDao(): TecnicoDao
    abstract fun relatorioDao(): RelatorioDao
    abstract fun imagemDao(): ImagemRelatorioDao
    abstract fun configLogoDao(): ConfigLogoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Nova tabela: maquinas
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maquinas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clienteId` INTEGER NOT NULL,
                        `marca` TEXT NOT NULL,
                        `modelo` TEXT NOT NULL,
                        `modeloIhm` TEXT,
                        `numeroSerie` TEXT NOT NULL,
                        `fotoPlaquetaUri` TEXT,
                        `fotoCompressorUri` TEXT
                    )
                """.trimIndent())

                // Nova tabela: tecnicos
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tecnicos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nome` TEXT NOT NULL,
                        `funcao` TEXT
                    )
                """.trimIndent())

                // Novas colunas na tabela relatorios
                database.execSQL("ALTER TABLE `relatorios` ADD COLUMN `tecnicoId` INTEGER")
                database.execSQL(
                    "ALTER TABLE `relatorios` ADD COLUMN `tipoRelatorio` TEXT NOT NULL DEFAULT 'geral'"
                )
                database.execSQL("ALTER TABLE `relatorios` ADD COLUMN `observacoes` TEXT")
                database.execSQL("ALTER TABLE `relatorios` ADD COLUMN `checklistResumo` TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relatorio_tecnico.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
