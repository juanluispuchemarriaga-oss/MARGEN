package com.margen.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Datos de prueba ficticios. */
class CoreUtilsTest {

    // ---- MoneyFormat ----

    @Test fun cop_usaPuntosDeMiles() {
        assertEquals("\$ 1.234.567", MoneyFormat.cop(1_234_567))
        assertEquals("\$ 0", MoneyFormat.cop(0))
        assertEquals("-\$ 5.000", MoneyFormat.cop(-5_000))
    }

    @Test fun copSigned_muestraSigno() {
        assertEquals("+\$ 500", MoneyFormat.copSigned(500))
        assertEquals("-\$ 500", MoneyFormat.copSigned(-500))
    }

    @Test fun quantity_usaComaDecimal() {
        assertEquals("1,5", MoneyFormat.quantity(1_500))
        assertEquals("2", MoneyFormat.quantity(2_000))
        assertEquals("0,25", MoneyFormat.quantity(250))
    }

    @Test fun percent_dosDecimalesConComa() {
        assertEquals("12,50 %", MoneyFormat.percent(BigDecimal("12.5")))
        assertEquals("-3,46 %", MoneyFormat.percent(BigDecimal("-3.456")))
        assertEquals("—", MoneyFormat.percentOrDash(null))
    }

    // ---- InputParsing ----

    @Test fun parsePesos_formatosComunes() {
        assertEquals(1_500L, InputParsing.parsePesos("1500"))
        assertEquals(1_500L, InputParsing.parsePesos("1.500"))
        assertEquals(1_234_567L, InputParsing.parsePesos("1.234.567"))
        assertEquals(1_501L, InputParsing.parsePesos("1.500,50"))
    }

    @Test fun parseDecimal_aceptaPuntoYComaDecimal() {
        assertEquals(0, BigDecimal("1.5").compareTo(InputParsing.parseDecimal("1.5")))
        assertEquals(0, BigDecimal("0.5").compareTo(InputParsing.parseDecimal("0,5")))
    }

    @Test fun parse_rechazaEntradasInvalidas() {
        assertNull(InputParsing.parseDecimal(""))
        assertNull(InputParsing.parseDecimal("abc"))
        assertNull(InputParsing.parseDecimal("-5"))
        assertNull(InputParsing.parseDecimal("1,2,3"))
        assertNull(InputParsing.parseDecimal("12.34.5"))
    }

    @Test fun parsePesos_respetaElLimite() {
        assertNull(InputParsing.parsePesos("99999999999"))
        assertEquals(9_999_999_999L, InputParsing.parsePesos("9999999999"))
    }

    @Test fun parseQuantityMilli_convierteAMilesimas() {
        assertEquals(1_500L, InputParsing.parseQuantityMilli("1,5"))
        assertEquals(250L, InputParsing.parseQuantityMilli("0,25"))
        assertEquals("1,5", InputParsing.quantityToInput(1_500))
    }
}
