package com.margen.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.core.MoneyFormat
import com.margen.data.ProductRepository
import com.margen.domain.calc.PricingCalculator
import com.margen.domain.model.ProductWithCosts
import com.margen.ui.components.ConfirmDialog
import com.margen.ui.components.EmptyState
import com.margen.ui.components.InfoNote
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import com.margen.ui.history.formatTimestamp
import com.margen.ui.margenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailState(
    val loaded: Boolean = false,
    val data: ProductWithCosts? = null
)

class ProductDetailViewModel(
    private val repository: ProductRepository,
    private val productId: Long
) : ViewModel() {

    val state: StateFlow<DetailState> = repository.observeProduct(productId)
        .map { DetailState(loaded = true, data = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.delete(productId)
            onDone()
        }
    }
}

@Composable
fun ProductDetailScreen(
    productId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenCalculator: () -> Unit,
    onDeleted: () -> Unit
) {
    val vm = margenViewModel(key = "detail_$productId") { c ->
        ProductDetailViewModel(c.productRepository, productId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val data = state.data

    Scaffold(
        topBar = {
            MargenTopBar(
                title = "Detalle del producto",
                onBack = onBack,
                actions = {
                    if (data != null && !deleting) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar producto")
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar producto")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            deleting || !state.loaded -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { if (!deleting) CircularProgressIndicator() }

            data == null -> EmptyState(
                title = "Producto no encontrado",
                message = "Es posible que haya sido eliminado.",
                modifier = Modifier.padding(padding),
                actionLabel = "Volver",
                onAction = onBack
            )

            else -> DetailContent(
                data = data,
                modifier = Modifier.padding(padding),
                onEdit = onEdit,
                onOpenCalculator = onOpenCalculator
            )
        }
    }

    if (showDelete && data != null) {
        ConfirmDialog(
            title = "¿Eliminar producto?",
            message = "Se eliminará \"${data.product.name}\" con todos sus costos. Esta acción no se puede deshacer.",
            confirmLabel = "Eliminar",
            onConfirm = {
                showDelete = false
                deleting = true
                vm.delete(onDone = onDeleted)
            },
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun DetailContent(
    data: ProductWithCosts,
    modifier: Modifier,
    onEdit: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val product = data.product
    val total = data.totalCostPesos
    val price = product.salePricePesos

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = product.name, style = MaterialTheme.typography.titleLarge)
        if (product.description.isNotBlank()) {
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard {
            SectionTitle("Costos")
            if (data.items.isEmpty()) {
                Text(
                    text = "Este producto aún no tiene costos. Toca Editar para agregarlos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                data.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${item.category.label} · ${MoneyFormat.quantity(item.quantityMilli)} " +
                                    "${item.unit} × ${MoneyFormat.cop(item.unitCostPesos)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = MoneyFormat.cop(item.totalPesos),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                HorizontalDivider()
                ResultRow("Costo total", MoneyFormat.cop(total), emphasize = true)
            }
        }

        SectionCard {
            SectionTitle("Precio y ganancia")
            if (price != null && price > 0 && data.items.isNotEmpty()) {
                val analysis = PricingCalculator.analyze(total, price)
                ResultRow("Precio de venta", MoneyFormat.cop(analysis.salePricePesos))
                ResultRow("Utilidad por unidad", MoneyFormat.cop(analysis.profitPesos), emphasize = true)
                ResultRow("Margen sobre el precio", MoneyFormat.percent(analysis.marginPercent))
                ResultRow("Recargo sobre el costo", MoneyFormat.percentOrDash(analysis.markupPercent))
                InfoNote("Valores por unidad y estimados con los costos y el precio que registraste.")
            } else if (price == null) {
                Text(
                    text = "Aún no definiste un precio de venta. Usa la calculadora para obtener uno recomendado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ResultRow("Precio de venta", MoneyFormat.cop(price))
                Text(
                    text = "Agrega costos para calcular tu utilidad y tu margen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(onClick = onOpenCalculator, modifier = Modifier.fillMaxWidth()) {
            Text("Calcular precio")
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Text("Editar producto")
        }

        Text(
            text = "Actualizado: ${formatTimestamp(product.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
