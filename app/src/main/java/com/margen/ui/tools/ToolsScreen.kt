@file:OptIn(ExperimentalMaterial3Api::class)

package com.margen.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.margen.core.InputParsing
import com.margen.core.MoneyFormat
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.NumberField
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import com.margen.ui.margenViewModel

private val TAB_TITLES = listOf("Precio", "Comparar", "Meta")

/** Mensaje cuando todavía no hay un costo utilizable; null si ya se puede calcular. */
internal fun costHint(state: ToolsState): String? {
    val cost = state.costPesos
    return when {
        cost != null && cost > 0 -> null
        state.selectedProduct != null ->
            "Este producto aún no tiene costos. Agrégalos desde su pantalla de edición."
        else -> "Escribe el costo total por unidad para ver los resultados."
    }
}

@Composable
fun ToolsScreen(productId: Long) {
    val vm = margenViewModel(key = "tools_$productId") { c ->
        ToolsViewModel(c.productRepository, c.historyRepository, c.entitlements, productId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = { MargenTopBar("Calculadora") },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                TAB_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SourceCard(state = state, vm = vm)
                when (tab) {
                    0 -> PriceTab(state = state, vm = vm)
                    1 -> CompareTab(state = state, vm = vm)
                    else -> GoalTab(state = state, vm = vm)
                }
            }
        }
    }
}

@Composable
private fun SourceCard(state: ToolsState, vm: ToolsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.selectedProduct

    SectionCard {
        SectionTitle("Costo a usar")
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selected?.product?.name ?: "Escribir costo manualmente",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Escribir costo manualmente") },
                    onClick = {
                        vm.selectProduct(null)
                        expanded = false
                    }
                )
                state.products.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(item.product.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        onClick = {
                            vm.selectProduct(item.product.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (selected == null) {
            val manualError =
                if (state.manualCost.isNotBlank() && InputParsing.parsePesos(state.manualCost) == null) {
                    "Ingresa un valor válido."
                } else null
            NumberField(
                value = state.manualCost,
                onValueChange = vm::setManualCost,
                label = "Costo total por unidad",
                prefix = "\$",
                error = manualError,
                imeAction = ImeAction.Done
            )
        } else {
            ResultRow("Costo total del producto", MoneyFormat.cop(selected.totalCostPesos))
        }
    }
}
