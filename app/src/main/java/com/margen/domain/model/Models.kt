package com.margen.domain.model

import com.margen.domain.calc.PricingCalculator

const val DEFAULT_UNIT = "unidad"

enum class CostCategory(val label: String) {
    MATERIAL("Material"),
    INGREDIENT("Ingrediente"),
    PACKAGING("Empaque"),
    TRANSPORT("Transporte"),
    LABOR("Mano de obra"),
    OTHER("Otro")
}

/**
 * Un componente de costo.
 * [quantityMilli]: cantidad en milésimas (1,5 kg = 1500).
 * [unitCostPesos]: costo por unidad en pesos enteros.
 */
data class CostItem(
    val id: Long = 0,
    val productId: Long = 0,
    val category: CostCategory,
    val name: String,
    val quantityMilli: Long,
    val unit: String,
    val unitCostPesos: Long
) {
    val totalPesos: Long
        get() = PricingCalculator.lineTotal(quantityMilli, unitCostPesos)
}

data class Product(
    val id: Long = 0,
    val name: String,
    val description: String,
    /** Precio de venta por unidad en pesos; null si aún no se ha definido. */
    val salePricePesos: Long?,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class ProductWithCosts(
    val product: Product,
    val items: List<CostItem>
) {
    val totalCostPesos: Long
        get() = PricingCalculator.totalCost(items.map { it.totalPesos })

    /** Utilidad por unidad; null si no hay precio de venta. Puede ser negativa (pérdida). */
    val profitPesos: Long?
        get() = product.salePricePesos?.let { it - totalCostPesos }

    /** Margen sobre precio de venta; null si no hay precio o es 0. */
    val marginPercent: java.math.BigDecimal?
        get() {
            val price = product.salePricePesos ?: return null
            if (price <= 0) return null
            return PricingCalculator.marginPercent(price, totalCostPesos)
        }
}

enum class HistoryType(val label: String) {
    PRODUCT_CREATED("Producto creado"),
    PRODUCT_UPDATED("Producto editado"),
    PRODUCT_DELETED("Producto eliminado"),
    CALCULATION("Cálculo guardado")
}

data class HistoryEntry(
    val id: Long,
    val timestamp: Long,
    val type: HistoryType,
    val title: String,
    val detail: String
)

enum class ThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Oscuro")
}
