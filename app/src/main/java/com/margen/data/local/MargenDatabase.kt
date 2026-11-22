package com.margen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class, CostItemEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MargenDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "margen.db"

        /**
         * Importante: NO se usa fallbackToDestructiveMigration().
         * Si algún día cambia el esquema, se escribe una migración para no perder datos.
         */
        fun create(context: Context): MargenDatabase =
            Room.databaseBuilder(context.applicationContext, MargenDatabase::class.java, NAME)
                .build()
    }
}
