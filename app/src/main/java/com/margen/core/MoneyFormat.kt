package com.margen.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Formato numérico colombiano: punto para miles y coma para decimales.
 * No depende del idioma configurado en el teléfono.
 */
object MoneyFormat {

    /** 1234567 -> "$ 1.234.567" */
    fun cop(pesos: Long): String {
        val sign = if (pesos < 0) "-" else ""
        return sign + "\$ " + groupThousands(pesos.toString().removePrefix("-"))
    }

    /** Con signo explícito para diferencias: "+$ 500", "-$ 500", "$ 0". */
    fun copSigned(pesos: Long): String =
        if (pesos > 0) "+" + cop(pesos) else cop(pesos)

    /** 1234567 -> "1.234.567" */
    fun number(value: Long): String {
        val sign = if (value < 0) "-" else ""
        return sign + groupThousands(value.toString().removePrefix("-"))
    }

    /** Cantidad en milésimas: 1500 -> "1,5"; 2000 -> "2"; 250 -> "0,25". */
    fun quantity(milli: Long): String {
        val sign = if (milli < 0) "-" else ""
        val abs = milli.toString().removePrefix("-").toLong()
        val whole = groupThousands((abs / 1000).toString())
        val frac = (abs % 1000).toString().padStart(3, '0').trimEnd('0')
        return sign + if (frac.isEmpty()) whole else "$whole,$frac"
    }

    /** 35.5 -> "35,50 %" (2 decimales, redondeo HALF_UP). */
    fun percent(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        val sign = if (scaled.signum() < 0) "-" else ""
        val parts = scaled.abs().toPlainString().split('.')
        val intPart = groupThousands(parts[0])
        val decPart = parts.getOrElse(1) { "00" }
        return "$sign$intPart,$decPart %"
    }

    /** Igual que [percent], pero muestra "—" si no hay valor. */
    fun percentOrDash(value: BigDecimal?): String =
        if (value == null) "—" else percent(value)

    /** Inserta puntos cada tres dígitos desde la derecha. */
    fun groupThousands(digits: String): String {
        val sb = StringBuilder()
        digits.forEachIndexed { index, c ->
            if (index > 0 && (digits.length - index) % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return sb.toString()
    }
}
