@file:OptIn(ExperimentalMaterial3Api::class)

package com.margen.ui.products

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.core.InputParsing
import com.margen.core.MoneyFormat
import com.margen.data.ProductRepository
import com.margen.domain.Entitlements
import com.margen.domain.calc.PricingCalculator
import com.margen.domain.model.CostCategory
import com.margen.domain.model.CostItem
import com.margen.domain.model.DEFAULT_UNIT
import com.margen.domain.model.Product
import com.margen.ui.components.ConfirmDialog
import com.margen.ui.components.EmptyState
import com.margen.ui.components.EstimateBadge
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.NumberField
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import com.margen.ui.components.TextInputField
import com.margen.ui.margenViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_ITEMS = 50

data class EditState(
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val items: List<CostItem> = emptyList(),
    val nameError: String? = null,
    val priceError: String? = null,
    val generalError: String? = null,
    val dirty: Boolean = false,
    val saving: Boolean = false
) {
    /** Suma de costos; null solo si la suma desborda (valores absurdos). */
    val totalCostPesos: Long?
        get() = runCatching { PricingCalculator.totalCost(items.map { it.totalPesos }) }.getOrNull()
}

class ProductEditViewModel(
    private val repository: ProductRepository,
    private val entitlements: Entitlements,
    private val productId: Long
) : ViewModel() {

    private var original: Product? = null
    private val _state = MutableStateFlow(EditState(loaded = productId == 0L))
    val state: StateFlow<EditState> = _state.asStateFlow()

    init {
        if (productId != 0L) {
            viewModelScope.launch {
                val data = repository.getProduct(productId)
                if (data == null) {
                    _state.update { it.copy(loaded = true, notFound = true) }
                } else {
                    original = data.product
                    _state.value = EditState(
                        loaded = true,
                        name = data.product.name,
                        description = data.product.description,
                        price = data.product.salePricePesos?.let { InputParsing.pesosToInput(it) } ?: "",
                        items = data.items
                    )
                }
            }
        }
    }

    fun onName(value: String) =
        _state.update { it.copy(name = value, nameError = null, dirty = true) }

    fun onDescription(value: String) =
        _state.update { it.copy(description = value, dirty = true) }

    fun onPrice(value: String) =
        _state.update { it.copy(price = value, priceError = null, dirty = true) }

    /** [index] fuera de rango agrega un costo nuevo; si no, reemplaza el existente. */
    fun upsertItem(index: Int, item: CostItem) = _state.update { s ->
        val list = s.items.toMutableList()
        if (index in list.indices) list[index] = item else list.add(item)
        s.copy(items = list, dirty = true, generalError = null)
    }

    fun removeItem(index: Int) = _state.update { s ->
        if (index !in s.items.indices) s else {
            s.copy(items = s.items.filterIndexed { i, _ -> i != index }, dirty = true, generalError = null)
        }
    }

    fun save(onSaved: (id: Long, isNew: Boolean) -> Unit) {
        val s = _state.value
        if (s.saving) return

        val name = s.name.trim()
        val nameError = if (name.isEmpty()) "Escribe el nombre del producto." else null

        var price: Long? = null
        var priceError: String? = null
        if (s.price.isNotBlank()) {
            val parsed = InputParsing.parsePesos(s.price)
            if (parsed == null || parsed <= 0) {
                priceError = "Ingresa un precio válido mayor a cero."
            } else {
                price = parsed
            }
        }

        val generalError = if (s.totalCostPesos == null) {
            "La suma de costos es demasiado grande. Revisa los valores."
        } else null

        if (nameError != null || priceError != null || generalError != null) {
            _state.update {
                it.copy(nameError = nameError, priceError = priceError, generalError = generalError)
            }
            return
        }

        _state.update { it.copy(saving = true, generalError = null) }
        viewModelScope.launch {
            try {
                val isNew = productId == 0L
                if (isNew && !entitlements.canCreateProduct(repository.count())) {
                    _state.update {
                        it.copy(saving = false, generalError = "Alcanzaste el límite de productos de tu plan.")
                    }
                    return@launch
                }
                val base = original ?: Product(name = "", description = "", salePricePesos = null)
                val product = base.copy(
                    id = productId,
                    name = name,
                    description = s.description.trim(),
                    salePricePesos = price
                )
                val id = repository.save(product, s.items)
                _state.update { it.copy(saving = false, dirty = false) }
                onSaved(id, isNew)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, generalError = "No se pudo guardar. Intenta de nuevo.")
                }
            }
        }
    }
}

@Composable
fun ProductEditScreen(
    productId: Long,
    onBack: () -> Unit,
    onSaved: (id: Long, isNew: Boolean) -> Unit
) {
    val vm = margenViewModel(key = "edit_$productId") { c ->
        ProductEditViewModel(c.productRepository, c.entitlements, productId)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    var showDiscard by remember { mutableStateOf(false) }
    var dialogOpen by remember { mutableStateOf(false) }
    var dialogIndex by remember { mutableIntStateOf(-1) }

    val requestBack: () -> Unit = {
        if (state.dirty) {
            showDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = state.dirty && !state.saving) { showDiscard = true }

    Scaffold(
        topBar = {
            MargenTopBar(
                title = if (productId == 0L) "Nuevo producto" else "Editar producto",
                onBack = requestBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            !state.loaded -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.notFound -> EmptyState(
                title = "Producto no encontrado",
                message = "Es posible que haya sido eliminado.",
                modifier = Modifier.padding(padding),
                actionLabel = "Volver",
                onAction = onBack
            )

            else -> EditForm(
                state = state,
                vm = vm,
                modifier = Modifier.padding(padding),
                onAddItem = {
                    dialogIndex = -1
                    dialogOpen = true
                },
                onEditItem = { index ->
                    dialogIndex = index
                    dialogOpen = true
                },
                onSave = { vm.save(onSaved) }
            )
        }
    }

    if (dialogOpen) {
        CostItemDialog(
            initial = state.items.getOrNull(dialogIndex),
            onDismiss = { dialogOpen = false },
            onConfirm = { item ->
                vm.upsertItem(dialogIndex, item)
                dialogOpen = false
            }
        )
    }

    if (showDiscard) {
        ConfirmDialog(
            title = "¿Descartar cambios?",
            message = "Tienes cambios sin guardar. Si sales ahora, se perderán.",
            confirmLabel = "Descartar",
            onConfirm = {
                showDiscard = false
                onBack()
            },
            onDismiss = { showDiscard = false }
        )
    }
}
