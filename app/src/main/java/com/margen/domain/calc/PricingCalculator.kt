package com.margen.domain.calc

import com.margen.core.Limits
import java.math.BigDecimal
import java.math.RoundingMode

data class PriceResult(
    val totalCostPesos: Long,
    val salePricePesos: Long,
    val profitPesos: Long,
    /** Utilidad / precio de venta × 100 (2 decimales). */
    val marginPercent: BigDecimal,
    /** Utilidad / costo × 100 (2 decimales). Null si el costo es 0. */
    val markupPercent: BigDecimal?
)

data class PriceComparison(
    val result: PriceResult,
    /** Diferencia de utilidad frente al primer precio de la lista. */
    val profitDiffPesos: Long,
    /** Diferencia de margen, en puntos porcentuales, frente al primer precio. */
    val marginDiffPoints: BigDecimal
)

data class SalesGoalResult(
    val unitsNeeded: Long,
    val revenuePesos: Long,
    val totalCostPesos: Long,
    val profitPesos: Long
)

/**
 * Cálculos de precios. Funciones puras, sin Android.
 * Dinero en pesos enteros (Long); operaciones intermedias con BigDecimal.
 * Los datos inválidos lanzan IllegalArgumentException con mensaje en español.
 */
object PricingCalculator {

    private val HUNDRED = BigDecimal(100)
    private val THOUSAND = BigDecimal(1000)

    /** Total de un componente: cantidad (milésimas) × costo unitario, redondeado al peso (HALF_UP). */
    fun lineTotal(quantityMilli: Long, unitCostPesos: Long): Long {
        require(quantityMilli >= 0) { "La cantidad no puede ser negativa." }
        require(unitCostPesos >= 0) { "El costo unitario no puede ser negativo." }
        return BigDecimal.valueOf(quantityMilli)
            .multiply(BigDecimal.valueOf(unitCostPesos))
            .divide(THOUSAND, 0, RoundingMode.HALF_UP)
            .toLong()
    }

    /** Costo total = suma de los totales de cada componente. */
    fun totalCost(lineTotals: List<Long>): Long {
        var total = 0L
        for (value in lineTotals) {
            require(value >= 0) { "Un costo no puede ser negativo." }
            total = try {
                Math.addExact(total, value)
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException("La suma de costos es demasiado grande.")
            }
        }
        return total
    }

    fun isValidMargin(marginPercent: BigDecimal): Boolean =
        marginPercent.signum() >= 0 && marginPercent.compareTo(HUNDRED) < 0

    /**
     * Precio recomendado = costo / (1 - margen/100), redondeado HACIA ARRIBA al peso
     * para que el margen deseado nunca quede incumplido por redondeo.
     */
    fun recommendedPrice(totalCostPesos: Long, desiredMarginPercent: BigDecimal): Long {
        require(totalCostPesos >= 0) { "El costo total no puede ser negativo." }
        require(isValidMargin(desiredMarginPercent)) {
            "El margen debe ser mayor o igual a 0 y menor a 100."
        }
        val divisor = BigDecimal.ONE.subtract(desiredMarginPercent.divide(HUNDRED))
        val price = try {
            BigDecimal.valueOf(totalCostPesos)
                .divide(divisor, 0, RoundingMode.CEILING)
                .longValueExact()
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("El precio resultante es demasiado grande.")
        }
        require(price <= Limits.MAX_PESOS) {
            "El precio resultante es demasiado grande. Reduce el margen o los costos."
        }
        return price
    }

    /** Utilidad = precio de venta - costo total. Puede ser negativa (pérdida). */
    fun profit(salePricePesos: Long, totalCostPesos: Long): Long {
        require(salePricePesos >= 0) { "El precio de venta no puede ser negativo." }
        require(totalCostPesos >= 0) { "El costo total no puede ser negativo." }
        return salePricePesos - totalCostPesos
    }

    /** Margen = utilidad / precio de venta × 100. El precio debe ser mayor a cero. */
    fun marginPercent(salePricePesos: Long, totalCostPesos: Long): BigDecimal {
        require(salePricePesos > 0) { "El precio de venta debe ser mayor a cero." }
        val profit = profit(salePricePesos, totalCostPesos)
        return BigDecimal.valueOf(profit)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(salePricePesos), 2, RoundingMode.HALF_UP)
    }

    /** Recargo = utilidad / costo total × 100. El costo debe ser mayor a cero. */
    fun markupPercent(salePricePesos: Long, totalCostPesos: Long): BigDecimal {
        require(totalCostPesos > 0) { "El costo total debe ser mayor a cero." }
        val profit = profit(salePricePesos, totalCostPesos)
        return BigDecimal.valueOf(profit)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(totalCostPesos), 2, RoundingMode.HALF_UP)
    }

    /** Análisis completo de un precio de venta frente a un costo. */
    fun analyze(totalCostPesos: Long, salePricePesos: Long): PriceResult {
        require(totalCostPesos >= 0) { "El costo total no puede ser negativo." }
        require(salePricePesos > 0) { "El precio de venta debe ser mayor a cero." }
        return PriceResult(
            totalCostPesos = totalCostPesos,
            salePricePesos = salePricePesos,
            profitPesos = profit(salePricePesos, totalCostPesos),
            marginPercent = marginPercent(salePricePesos, totalCostPesos),
            markupPercent = if (totalCostPesos > 0) markupPercent(salePricePesos, totalCostPesos) else null
        )
    }

    /** Precio recomendado y su análisis completo. El costo debe ser mayor a cero. */
    fun recommend(totalCostPesos: Long, desiredMarginPercent: BigDecimal): PriceResult {
        require(totalCostPesos > 0) { "El costo total debe ser mayor a cero." }
        val price = recommendedPrice(totalCostPesos, desiredMarginPercent)
        return analyze(totalCostPesos, price)
    }

    /** Compara varios precios; las diferencias se calculan contra el primero. */
    fun compare(totalCostPesos: Long, prices: List<Long>): List<PriceComparison> {
        require(prices.isNotEmpty()) { "Agrega al menos un precio para comparar." }
        val results = prices.map { analyze(totalCostPesos, it) }
        val base = results.first()
        return results.map {
            PriceComparison(
                result = it,
                profitDiffPesos = it.profitPesos - base.profitPesos,
                marginDiffPoints = it.marginPercent.subtract(base.marginPercent)
            )
        }
    }

    /** Unidades necesarias = meta / utilidad por unidad, redondeado HACIA ARRIBA. */
    fun unitsForGoal(goalProfitPesos: Long, profitPerUnitPesos: Long): Long {
        require(goalProfitPesos > 0) { "La meta de ganancia debe ser mayor a cero." }
        require(profitPerUnitPesos > 0) {
            "La utilidad por unidad debe ser mayor a cero para calcular las unidades."
        }
        val quotient = goalProfitPesos / profitPerUnitPesos
        return if (goalProfitPesos % profitPerUnitPesos == 0L) quotient else quotient + 1
    }

    /** Objetivo de ventas completo: unidades, ingresos, costos y ganancia resultante. */
    fun salesGoal(goalProfitPesos: Long, salePricePesos: Long, totalCostPesos: Long): SalesGoalResult {
        require(salePricePesos > 0) { "El precio de venta debe ser mayor a cero." }
        val profitPerUnit = profit(salePricePesos, totalCostPesos)
        val units = unitsForGoal(goalProfitPesos, profitPerUnit)
        return try {
            SalesGoalResult(
                unitsNeeded = units,
                revenuePesos = Math.multiplyExact(units, salePricePesos),
                totalCostPesos = Math.multiplyExact(units, totalCostPesos),
                profitPesos = Math.multiplyExact(units, profitPerUnit)
            )
        } catch (e: ArithmeticException) {
            throw IllegalArgumentException("Los valores son demasiado grandes.")
        }
    }
}
