package com.margen.domain

/** Funciones que en el futuro pueden depender de la versión (gratuita o PRO). */
enum class Feature {
    UNLIMITED_PRODUCTS,
    ADVANCED_HISTORY,
    PRICE_COMPARATOR,
    PDF_REPORTS,
    EXPORT,
    ADVANCED_GOALS
}

/**
 * Punto único donde se decide qué puede hacer el usuario.
 * Las pantallas consultan esta interfaz, nunca "si pagó o no".
 * Para activar la versión gratuita/PRO más adelante solo hay que cambiar
 * la implementación que se entrega en AppContainer (ver README).
 */
interface Entitlements {
    /** Máximo de productos; null = ilimitado. */
    val maxProducts: Int?

    fun isEnabled(feature: Feature): Boolean

    fun canCreateProduct(currentCount: Int): Boolean =
        maxProducts?.let { currentCount < it } ?: true
}

/** MVP: todo disponible, sin límites. Es la implementación activa hoy. */
object MvpEntitlements : Entitlements {
    override val maxProducts: Int? = null
    override fun isEnabled(feature: Feature): Boolean = true
}

/**
 * Plan gratuito futuro: 5 productos y funciones básicas.
 * NO está conectado en el MVP. Queda aquí como referencia de la arquitectura.
 */
object FreeEntitlements : Entitlements {
    override val maxProducts: Int? = 5
    override fun isEnabled(feature: Feature): Boolean = false
}
