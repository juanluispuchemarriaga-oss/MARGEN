package com.margen.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.core.MoneyFormat
import com.margen.data.HistoryRepository
import com.margen.data.ProductRepository
import com.margen.domain.model.HistoryEntry
import com.margen.domain.model.ProductWithCosts
import com.margen.ui.components.EstimateBadge
import com.margen.ui.components.InfoNote
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import com.margen.ui.components.StatCard
import com.margen.ui.history.formatTimestamp
import com.margen.ui.margenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardState(
    val loaded: Boolean = false,
    val productCount: Int = 0,
    /** Productos con costos registrados y precio de venta definido. */
    val analyzedCount: Int = 0,
    /** Suma de la utilidad estimada por unidad de cada producto analizado. */
    val estimatedProfitPerUnitSum: Long = 0,
    val topMargin: List<ProductWithCosts> = emptyList(),
    val recent: List<HistoryEntry> = emptyList()
)

class DashboardViewModel(
    productRepository: ProductRepository,
    historyRepository: HistoryRepository
) : ViewModel() {

    val state: StateFlow<DashboardState> = combine(
        productRepository.products,
        historyRepository.observeRecent(3)
    ) { products, recent ->
        val analyzed = products.filter {
            it.items.isNotEmpty() && (it.product.salePricePesos ?: 0L) > 0L
        }
        DashboardState(
            loaded = true,
            productCount = products.size,
            analyzedCount = analyzed.size,
            estimatedProfitPerUnitSum = analyzed.sumOf { it.profitPesos ?: 0L },
            topMargin = analyzed.sortedByDescending { it.marginPercent }.take(3),
            recent = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())
}

@Composable
fun DashboardScreen(
    onNewProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val vm = margenViewModel { c -> DashboardViewModel(c.productRepository, c.historyRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { MargenTopBar("MARGEN") },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Conoce lo que realmente ganas",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.loaded && state.productCount == 0) {
                SectionCard {
                    SectionTitle("Empieza por tu primer producto")
                    Text(
                        text = "Registra un producto con sus costos y MARGEN calculará su precio de venta, tu utilidad y tu margen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onNewProduct) { Text("Crear producto") }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Productos",
                        value = state.productCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Con precio y costos",
                        value = state.analyzedCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Ganancia potencial por unidad")
                        EstimateBadge()
                    }
                    Text(
                        text = if (state.analyzedCount > 0) {
                            MoneyFormat.cop(state.estimatedProfitPerUnitSum)
                        } else "—",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    InfoNote(
                        "Suma de la utilidad estimada por unidad de cada producto con precio y costos. " +
                            "No es dinero recibido ni ventas reales."
                    )
                }

                SectionCard {
                    SectionTitle("Productos con mayor margen")
                    if (state.topMargin.isEmpty()) {
                        Text(
                            text = "Cuando tus productos tengan costos y precio de venta, aquí verás los de mayor margen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.topMargin.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenProduct(item.product.id) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Utilidad por unidad: " +
                                            (item.profitPesos?.let { MoneyFormat.cop(it) } ?: "—"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = MoneyFormat.percentOrDash(item.marginPercent),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    TextButton(onClick = onOpenProducts) { Text("Ver todos los productos") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onNewProduct, modifier = Modifier.weight(1f)) {
                    Text("Nuevo producto")
                }
                FilledTonalButton(onClick = onOpenCalculator, modifier = Modifier.weight(1f)) {
                    Text("Calculadora")
                }
            }

            SectionCard {
                SectionTitle("Actividad reciente")
                if (state.recent.isEmpty()) {
                    Text(
                        text = "Aún no hay actividad registrada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.recent.forEach { entry ->
                        Column {
                            Text(
                                text = "${entry.type.label}: ${entry.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatTimestamp(entry.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(onClick = onOpenHistory) { Text("Ver historial") }
            }
        }
    }
}
