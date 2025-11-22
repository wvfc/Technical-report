package com.soutech.relatoriotecnico.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClienteEntity::class,
        RelatorioEntity::class,
        ImagemRelatorioEntity::class,
        ConfigLogoEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun relatorioDao(): RelatorioDao
    abstract fun imagemDao(): ImagemRelatorioDao
    abstract fun configLogoDao(): ConfigLogoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relatorio_tecnico.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
