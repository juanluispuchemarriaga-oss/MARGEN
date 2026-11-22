package com.margen.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.core.MoneyFormat
import com.margen.data.ProductRepository
import com.margen.domain.model.ProductWithCosts
import com.margen.ui.components.EmptyState
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.SectionCard
import com.margen.ui.margenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProductListState(
    val loaded: Boolean = false,
    val totalCount: Int = 0,
    val products: List<ProductWithCosts> = emptyList()
)

class ProductListViewModel(productRepository: ProductRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<ProductListState> = combine(productRepository.products, query) { all, q ->
        val text = q.trim()
        val filtered = if (text.isEmpty()) {
            all
        } else {
            all.filter {
                it.product.name.contains(text, ignoreCase = true) ||
                    it.product.description.contains(text, ignoreCase = true)
            }
        }
        ProductListState(loaded = true, totalCount = all.size, products = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListState())

    fun setQuery(value: String) {
        query.value = value
    }
}

@Composable
fun ProductListScreen(
    onNewProduct: () -> Unit,
    onOpenProduct: (Long) -> Unit
) {
    val vm = margenViewModel { c -> ProductListViewModel(c.productRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { vm.setQuery(query) }

    Scaffold(
        topBar = { MargenTopBar("Productos") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewProduct,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (state.totalCount > 0 || query.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Buscar producto") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                            }
                        }
                    } else null
                )
            }

            when {
                !state.loaded -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.totalCount == 0 -> EmptyState(
                    title = "Aún no tienes productos",
                    message = "Crea el primero para calcular su costo, su precio y tu ganancia.",
                    actionLabel = "Crear producto",
                    onAction = onNewProduct
                )

                state.products.isEmpty() -> EmptyState(
                    title = "Sin resultados",
                    message = "Ningún producto coincide con \"${query.trim()}\"."
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.products, key = { it.product.id }) { item ->
                        ProductCard(item = item, onClick = { onOpenProduct(item.product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(item: ProductWithCosts, onClick: () -> Unit) {
    val price = item.product.salePricePesos
    val profit = item.profitPesos
    SectionCard(onClick = onClick) {
        Text(
            text = item.product.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (item.product.description.isNotBlank()) {
            Text(
                text = item.product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LabeledValue(
                label = "Costo",
                value = if (item.items.isEmpty()) "—" else MoneyFormat.cop(item.totalCostPesos)
            )
            LabeledValue(
                label = "Precio de venta",
                value = if (price != null) MoneyFormat.cop(price) else "—"
            )
        }
        if (price != null && price > 0 && item.items.isNotEmpty() && profit != null) {
            Text(
                text = "Utilidad por unidad: ${MoneyFormat.cop(profit)} · Margen ${MoneyFormat.percentOrDash(item.marginPercent)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (profit < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = if (price == null) "Sin precio de venta" else "Sin costos registrados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}
