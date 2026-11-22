package com.margen.ui.tools

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.margen.core.InputParsing
import com.margen.core.MoneyFormat
import com.margen.domain.calc.PricingCalculator
import com.margen.ui.components.InfoNote
import com.margen.ui.components.NumberField
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle

@Composable
fun PriceTab(state: ToolsState, vm: ToolsViewModel) {
    val marginValue = InputParsing.parsePercent(state.margin)
    val marginError = when {
        state.margin.isBlank() -> "Escribe el margen deseado."
        marginValue == null -> "Ingresa un número válido."
        !PricingCalculator.isValidMargin(marginValue) ->
            "El margen debe ser mayor o igual a 0 y menor a 100."
        else -> null
    }

    SectionCard {
        SectionTitle("Margen deseado")
        NumberField(
            value = state.margin,
            onValueChange = vm::setMargin,
            label = "Margen de ganancia",
            suffix = "%",
            decimal = true,
            error = marginError,
            imeAction = ImeAction.Done
        )
        InfoNote(
            "El margen se calcula sobre el precio de venta (utilidad ÷ precio). " +
                "El recargo se calcula sobre el costo (utilidad ÷ costo). No son lo mismo."
        )
    }

    val hint = costHint(state)
    val cost = state.costPesos
    if (hint != null || cost == null) {
        InfoNote(hint ?: "Escribe el costo total por unidad para ver los resultados.")
        return
    }

    val outcome = if (marginError == null && marginValue != null) {
        runCatching { PricingCalculator.recommend(cost, marginValue) }
    } else null
    val result = outcome?.getOrNull()
    val failure = outcome?.exceptionOrNull()?.message

    if (failure != null) {
        Text(
            text = failure,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    if (result != null && marginValue != null) {
        SectionCard {
            SectionTitle("Precio recomendado")
            ResultRow("Precio de venta", MoneyFormat.cop(result.salePricePesos), emphasize = true)
            ResultRow("Costo total", MoneyFormat.cop(result.totalCostPesos))
            ResultRow("Utilidad por unidad", MoneyFormat.cop(result.profitPesos))
            ResultRow("Margen real sobre el precio", MoneyFormat.percent(result.marginPercent))
            ResultRow("Recargo sobre el costo", MoneyFormat.percentOrDash(result.markupPercent))
            InfoNote("El precio se redondea hacia arriba al peso para no quedar por debajo del margen deseado.")
        }

        Button(
            onClick = {
                val title = state.selectedProduct?.product?.name ?: "Cálculo manual"
                val detail = "Costo ${MoneyFormat.cop(result.totalCostPesos)} · " +
                    "Margen deseado ${MoneyFormat.percent(marginValue)} · " +
                    "Precio ${MoneyFormat.cop(result.salePricePesos)} · " +
                    "Utilidad ${MoneyFormat.cop(result.profitPesos)} · " +
                    "Recargo ${MoneyFormat.percentOrDash(result.markupPercent)}"
                vm.saveCalculation(title, detail)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar en el historial") }

        if (state.selectedProduct != null) {
            OutlinedButton(
                onClick = { vm.applyPrice(result.salePricePesos) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Usar como precio de venta del producto") }
        }
    }

    val currentPrice = state.selectedProduct?.product?.salePricePesos
    if (currentPrice != null && currentPrice > 0) {
        val current = runCatching { PricingCalculator.analyze(cost, currentPrice) }.getOrNull()
        if (current != null) {
            SectionCard {
                SectionTitle("Tu precio actual")
                ResultRow("Precio de venta", MoneyFormat.cop(current.salePricePesos))
                ResultRow("Utilidad por unidad", MoneyFormat.cop(current.profitPesos))
                ResultRow("Margen sobre el precio", MoneyFormat.percent(current.marginPercent))
                ResultRow("Recargo sobre el costo", MoneyFormat.percentOrDash(current.markupPercent))
            }
        }
    }
}
