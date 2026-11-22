package com.margen.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.margen.core.InputParsing
import com.margen.core.MoneyFormat
import com.margen.domain.calc.PricingCalculator
import com.margen.ui.components.EstimateBadge
import com.margen.ui.components.InfoNote
import com.margen.ui.components.NumberField
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle

@Composable
fun GoalTab(state: ToolsState, vm: ToolsViewModel) {
    val goalValue = InputParsing.parsePesos(state.goal)
    val priceValue = InputParsing.parsePesos(state.goalPrice)
    val goalError = if (state.goal.isNotBlank() && (goalValue == null || goalValue <= 0)) {
        "Ingresa una meta válida mayor a cero."
    } else null
    val priceError = if (state.goalPrice.isNotBlank() && (priceValue == null || priceValue <= 0)) {
        "Ingresa un precio válido mayor a cero."
    } else null

    SectionCard {
        SectionTitle("Meta de ganancia")
        NumberField(
            value = state.goal,
            onValueChange = vm::setGoal,
            label = "Ganancia que quieres alcanzar",
            prefix = "\$",
            error = goalError
        )
        NumberField(
            value = state.goalPrice,
            onValueChange = vm::setGoalPrice,
            label = "Precio de venta por unidad",
            prefix = "\$",
            error = priceError,
            imeAction = ImeAction.Done
        )
    }

    val hint = costHint(state)
    val cost = state.costPesos
    if (hint != null || cost == null) {
        InfoNote(hint ?: "Escribe el costo total por unidad para ver los resultados.")
        return
    }
    if (goalValue == null || goalValue <= 0 || priceValue == null || priceValue <= 0) {
        InfoNote("Escribe la meta y el precio de venta para calcular las unidades necesarias.")
        return
    }

    val outcome = runCatching { PricingCalculator.salesGoal(goalValue, priceValue, cost) }
    val result = outcome.getOrNull()
    if (result == null) {
        Text(
            text = outcome.exceptionOrNull()?.message ?: "No se pudo calcular.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Unidades necesarias")
            EstimateBadge()
        }
        ResultRow(
            label = "Unidades a vender",
            value = MoneyFormat.number(result.unitsNeeded),
            emphasize = true
        )
        ResultRow("Utilidad por unidad", MoneyFormat.cop(PricingCalculator.profit(priceValue, cost)))
        ResultRow("Ingresos por ventas", MoneyFormat.cop(result.revenuePesos))
        ResultRow("Costos totales", MoneyFormat.cop(result.totalCostPesos))
        ResultRow("Ganancia resultante", MoneyFormat.cop(result.profitPesos))
        InfoNote(
            "Las unidades se redondean hacia arriba, por eso la ganancia resultante puede superar un poco tu meta. " +
                "Es una proyección, no ventas reales."
        )
    }
}
