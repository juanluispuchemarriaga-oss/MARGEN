package com.margen.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.margen.core.InputParsing
import com.margen.core.MoneyFormat
import com.margen.domain.Feature
import com.margen.domain.calc.PricingCalculator
import com.margen.ui.components.EmptyState
import com.margen.ui.components.InfoNote
import com.margen.ui.components.NumberField
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import java.math.BigDecimal

/** Diferencia de margen en puntos porcentuales: "+5,00 pts". */
private fun signedPoints(value: BigDecimal): String {
    val text = MoneyFormat.percent(value).removeSuffix(" %") + " pts"
    return if (value.signum() > 0) "+$text" else text
}

@Composable
fun CompareTab(state: ToolsState, vm: ToolsViewModel) {
    if (!vm.entitlements.isEnabled(Feature.PRICE_COMPARATOR)) {
        EmptyState(
            title = "Comparador de precios",
            message = "Esta función estará disponible en la versión PRO."
        )
        return
    }

    SectionCard {
        SectionTitle("Precios a comparar")
        state.comparePrices.forEachIndexed { index, text ->
            val parsed = InputParsing.parsePesos(text)
            val error = if (text.isNotBlank() && (parsed == null || parsed <= 0)) {
                "Ingresa un precio válido mayor a cero."
            } else null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                NumberField(
                    value = text,
                    onValueChange = { vm.setComparePrice(index, it) },
                    label = if (index == 0) "Precio 1 (base)" else "Precio ${index + 1}",
                    modifier = Modifier.weight(1f),
                    prefix = "\$",
                    error = error,
                    imeAction = ImeAction.Done
                )
                if (index >= 2) {
                    IconButton(onClick = { vm.removeComparePrice(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar precio ${index + 1}")
                    }
                }
            }
        }
        if (state.comparePrices.size < 5) {
            OutlinedButton(
                onClick = vm::addComparePrice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Agregar precio")
            }
        }
        InfoNote("Las diferencias se calculan frente al primer precio válido de la lista.")
    }

    val hint = costHint(state)
    val cost = state.costPesos
    if (hint != null || cost == null) {
        InfoNote(hint ?: "Escribe el costo total por unidad para ver los resultados.")
        return
    }

    val labeled = state.comparePrices.mapIndexedNotNull { index, text ->
        val price = InputParsing.parsePesos(text)
        if (price != null && price > 0) "Precio ${index + 1}" to price else null
    }
    if (labeled.isEmpty()) {
        InfoNote("Escribe al menos un precio válido.")
        return
    }

    val outcome = runCatching { PricingCalculator.compare(cost, labeled.map { it.second }) }
    val comparisons = outcome.getOrNull()
    if (comparisons == null) {
        Text(
            text = outcome.exceptionOrNull()?.message ?: "No se pudo comparar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    if (labeled.size < 2) {
        InfoNote("Agrega un segundo precio para ver las diferencias.")
    }

    val bestProfit = comparisons.maxOf { it.result.profitPesos }
    comparisons.forEachIndexed { index, comparison ->
        val result = comparison.result
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(labeled[index].first + if (index == 0) " (base)" else "")
                if (labeled.size > 1 && result.profitPesos == bestProfit) {
                    Text(
                        text = "Mayor utilidad",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ResultRow("Precio de venta", MoneyFormat.cop(result.salePricePesos), emphasize = true)
            ResultRow("Utilidad por unidad", MoneyFormat.cop(result.profitPesos))
            ResultRow("Margen sobre el precio", MoneyFormat.percent(result.marginPercent))
            ResultRow("Recargo sobre el costo", MoneyFormat.percentOrDash(result.markupPercent))
            if (index > 0) {
                ResultRow("Diferencia de utilidad", MoneyFormat.copSigned(comparison.profitDiffPesos))
                ResultRow("Diferencia de margen", signedPoints(comparison.marginDiffPoints))
            }
        }
    }
}
