package com.margen

import android.app.Application
import android.content.Context
import com.margen.data.HistoryRepository
import com.margen.data.ProductRepository
import com.margen.data.SettingsStore
import com.margen.data.local.MargenDatabase
import com.margen.domain.Entitlements
import com.margen.domain.MvpEntitlements

/** Contenedor de dependencias manual (sin librerías de inyección). */
class AppContainer(context: Context) {
    val database: MargenDatabase = MargenDatabase.create(context)
    val productRepository = ProductRepository(database)
    val historyRepository = HistoryRepository(database.historyDao())
    val settingsStore = SettingsStore(context)

    /** Cambiar aquí por una implementación gratuita/PRO cuando se integre la monetización. */
    val entitlements: Entitlements = MvpEntitlements
}

class MargenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
