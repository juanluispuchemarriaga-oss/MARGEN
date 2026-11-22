package com.margen.core

import java.math.BigDecimal
import java.math.RoundingMode

/** Límites de seguridad para evitar desbordamientos. */
object Limits {
    /** Máximo en pesos para cualquier valor monetario (casi 10 mil millones). */
    const val MAX_PESOS = 9_999_999_999L

    /** Máximo de cantidad en milésimas (1.000.000 de unidades). */
    const val MAX_QUANTITY_MILLI = 1_000_000_000L
}

/**
 * Lectura de números escritos por el usuario.
 * Acepta formato colombiano ("1.500,50") y simple ("1500", "1.5", "0,5").
 * Un punto seguido de exactamente 3 dígitos (y primer grupo sin cero inicial) se
 * interpreta como separador de miles: "1.500" = 1500.
 */
object InputParsing {

    private val GROUPED = Regex("^[1-9]\\d{0,2}(\\.\\d{3})+$")

    /** Devuelve un decimal no negativo, o null si el texto no es válido. */
    fun parseDecimal(text: String): BigDecimal? {
        val cleaned = text.trim().replace(" ", "").removePrefix("$")
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' || it == ',' }) return null
        if (cleaned.count { it == ',' } > 1) return null

        val normalized: String = when {
            cleaned.contains(',') -> {
                val intPart = cleaned.substringBefore(',')
                val decPart = cleaned.substringAfter(',')
                if (decPart.contains('.')) return null
                if (intPart.contains('.') && !GROUPED.matches(intPart)) return null
                val whole = intPart.replace(".", "").ifEmpty { "0" }
                if (decPart.isEmpty()) whole else "$whole.$decPart"
            }
            GROUPED.matches(cleaned) -> cleaned.replace(".", "")
            cleaned.count { it == '.' } > 1 -> return null
            else -> cleaned
        }
        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    /** Pesos enteros (redondea al peso más cercano). Null si es inválido o supera el límite. */
    fun parsePesos(text: String): Long? {
        val d = parseDecimal(text) ?: return null
        val rounded = d.setScale(0, RoundingMode.HALF_UP)
        if (rounded > BigDecimal.valueOf(Limits.MAX_PESOS)) return null
        return rounded.toLong()
    }

    /** Cantidad en milésimas (hasta 3 decimales). Null si es inválida o supera el límite. */
    fun parseQuantityMilli(text: String): Long? {
        val d = parseDecimal(text) ?: return null
        val milli = d.multiply(BigDecimal(1000)).setScale(0, RoundingMode.HALF_UP)
        if (milli > BigDecimal.valueOf(Limits.MAX_QUANTITY_MILLI)) return null
        return milli.toLong()
    }

    /** Porcentaje como decimal. El rango se valida en el calculador. */
    fun parsePercent(text: String): BigDecimal? = parseDecimal(text)

    /** Para rellenar campos al editar: 1500 -> "1500". */
    fun pesosToInput(pesos: Long): String = pesos.toString()

    /** Para rellenar campos al editar: 1500 -> "1,5"; 2000 -> "2". */
    fun quantityToInput(milli: Long): String {
        val whole = milli / 1000
        val frac = (milli % 1000).toString().padStart(3, '0').trimEnd('0')
        return if (frac.isEmpty()) whole.toString() else "$whole,$frac"
    }

    /** Para rellenar campos al editar: 35.50 -> "35,5". */
    fun percentToInput(value: BigDecimal): String =
        value.stripTrailingZeros().toPlainString().replace('.', ',')
}
