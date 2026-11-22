package com.margen.domain.calc

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Todos los valores de este archivo son DATOS DE PRUEBA ficticios, no información real. */
class PricingCalculatorTest {

    private fun assertPercent(expected: String, actual: BigDecimal?) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    // ---- Costos ----

    @Test fun lineTotal_multiplicaCantidadPorCosto() {
        assertEquals(3_000L, PricingCalculator.lineTotal(1_500, 2_000))
        assertEquals(250L, PricingCalculator.lineTotal(250, 1_000))
    }

    @Test fun lineTotal_redondeaAlPesoHalfUp() {
        assertEquals(3L, PricingCalculator.lineTotal(333, 10))   // 3,33 -> 3
        assertEquals(3L, PricingCalculator.lineTotal(500, 5))    // 2,5  -> 3
    }

    @Test fun lineTotal_rechazaNegativos() {
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.lineTotal(-1, 100) }
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.lineTotal(100, -1) }
    }

    @Test fun totalCost_sumaComponentes() {
        assertEquals(11_400L, PricingCalculator.totalCost(listOf(6_300, 3_600, 1_500)))
        assertEquals(0L, PricingCalculator.totalCost(emptyList()))
    }

    @Test fun totalCost_rechazaNegativosYDesbordamiento() {
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.totalCost(listOf(10, -1)) }
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.totalCost(listOf(Long.MAX_VALUE, 1))
        }
    }

    // ---- Precio recomendado ----

    @Test fun recommendedPrice_margen30() {
        assertEquals(100_000L, PricingCalculator.recommendedPrice(70_000, BigDecimal("30")))
    }

    @Test fun recommendedPrice_margenCeroDevuelveElCosto() {
        assertEquals(10_000L, PricingCalculator.recommendedPrice(10_000, BigDecimal.ZERO))
    }

    @Test fun recommendedPrice_redondeaHaciaArribaYCumpleElMargen() {
        val price = PricingCalculator.recommendedPrice(1_000, BigDecimal("33.33"))
        assertEquals(1_500L, price)
        val margin = PricingCalculator.marginPercent(price, 1_000)
        assertTrue(margin.compareTo(BigDecimal("33.33")) >= 0)
    }

    @Test fun recommendedPrice_validaRangoDelMargen() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.recommendedPrice(1_000, BigDecimal("100"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.recommendedPrice(1_000, BigDecimal("-1"))
        }
        assertEquals(10_000_000L, PricingCalculator.recommendedPrice(1_000, BigDecimal("99.99")))
    }

    @Test fun recommendedPrice_rechazaPreciosGigantes() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.recommendedPrice(9_999_999_999, BigDecimal("50"))
        }
    }

    // ---- Utilidad, margen y recargo ----

    @Test fun profit_gananciaYPerdida() {
        assertEquals(30_000L, PricingCalculator.profit(100_000, 70_000))
        assertEquals(-20_000L, PricingCalculator.profit(50_000, 70_000))
    }

    @Test fun margin_yMarkup_sonDiferentes() {
        assertPercent("30.00", PricingCalculator.marginPercent(100_000, 70_000))
        assertPercent("42.86", PricingCalculator.markupPercent(100_000, 70_000))
    }

    @Test fun margin_negativoCuandoHayPerdida() {
        assertPercent("-40.00", PricingCalculator.marginPercent(50_000, 70_000))
    }

    @Test fun divisionPorCero_seRechaza() {
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.marginPercent(0, 1_000) }
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.markupPercent(1_000, 0) }
    }

    @Test fun analyze_conCostoCeroNoTieneRecargo() {
        val result = PricingCalculator.analyze(0, 5_000)
        assertNull(result.markupPercent)
        assertPercent("100.00", result.marginPercent)
    }

    @Test fun recommend_requiereCostoMayorACero() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.recommend(0, BigDecimal("30"))
        }
    }

    @Test fun recommend_ejemploTortaDePrueba() {
        val result = PricingCalculator.recommend(11_400, BigDecimal("40"))
        assertEquals(19_000L, result.salePricePesos)
        assertEquals(7_600L, result.profitPesos)
        assertPercent("40.00", result.marginPercent)
        assertPercent("66.67", result.markupPercent)
    }

    // ---- Comparador ----

    @Test fun compare_diferenciasContraElPrimerPrecio() {
        val list = PricingCalculator.compare(70_000, listOf(100_000, 120_000))
        assertEquals(0L, list[0].profitDiffPesos)
        assertEquals(20_000L, list[1].profitDiffPesos)
        assertPercent("11.67", list[1].marginDiffPoints)
    }

    @Test fun compare_listaVaciaSeRechaza() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.compare(1_000, emptyList())
        }
    }

    // ---- Objetivos de ventas ----

    @Test fun unitsForGoal_redondeaHaciaArriba() {
        assertEquals(34L, PricingCalculator.unitsForGoal(1_000_000, 30_000))
        assertEquals(30L, PricingCalculator.unitsForGoal(900_000, 30_000))
    }

    @Test fun unitsForGoal_rechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.unitsForGoal(0, 1_000) }
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.unitsForGoal(1_000, 0) }
        assertThrows(IllegalArgumentException::class.java) { PricingCalculator.unitsForGoal(1_000, -5) }
    }

    @Test fun salesGoal_calculaTotales() {
        val result = PricingCalculator.salesGoal(1_000_000, 100_000, 70_000)
        assertEquals(34L, result.unitsNeeded)
        assertEquals(3_400_000L, result.revenuePesos)
        assertEquals(2_380_000L, result.totalCostPesos)
        assertEquals(1_020_000L, result.profitPesos)
    }

    @Test fun salesGoal_conPerdidaSeRechaza() {
        assertThrows(IllegalArgumentException::class.java) {
            PricingCalculator.salesGoal(1_000_000, 50_000, 70_000)
        }
    }
}
