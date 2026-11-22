package com.margen.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.margen.core.InputParsing
import com.margen.data.HistoryRepository
import com.margen.data.ProductRepository
import com.margen.domain.Entitlements
import com.margen.domain.model.HistoryType
import com.margen.domain.model.ProductWithCosts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_COMPARE_PRICES = 5

data class ToolsState(
    val products: List<ProductWithCosts> = emptyList(),
    val selectedProductId: Long? = null,
    val manualCost: String = "",
    val margin: String = "30",
    val comparePrices: List<String> = listOf("", ""),
    val goal: String = "",
    val goalPrice: String = "",
    val message: String? = null
) {
    val selectedProduct: ProductWithCosts?
        get() = products.firstOrNull { it.product.id == selectedProductId }

    /** Costo total usado en los cálculos. Null si falta o es inválido. */
    val costPesos: Long?
        get() {
            val product = selectedProduct
            return if (product != null) product.totalCostPesos else InputParsing.parsePesos(manualCost)
        }
}

class ToolsViewModel(
    private val productRepository: ProductRepository,
    private val historyRepository: HistoryRepository,
    val entitlements: Entitlements,
    private val initialProductId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsState())
    val state: StateFlow<ToolsState> = _state.asStateFlow()

    private var initialApplied = initialProductId <= 0L

    init {
        viewModelScope.launch {
            productRepository.products.collect { list ->
                _state.update { s ->
                    val selected = s.selectedProductId
                    val stillExists = selected != null && list.any { it.product.id == selected }
                    s.copy(
                        products = list,
                        selectedProductId = if (stillExists) selected else null
                    )
                }
                if (!initialApplied && list.any { it.product.id == initialProductId }) {
                    initialApplied = true
                    selectProduct(initialProductId)
                }
            }
        }
    }

    fun selectProduct(id: Long?) = _state.update { s ->
        val product = s.products.firstOrNull { it.product.id == id }
        if (product == null) {
            s.copy(selectedProductId = null)
        } else {
            val priceText = product.product.salePricePesos
                ?.let { InputParsing.pesosToInput(it) } ?: ""
            s.copy(
                selectedProductId = product.product.id,
                goalPrice = priceText,
                comparePrices = listOf(priceText) + s.comparePrices.drop(1)
            )
        }
    }

    fun setManualCost(value: String) = _state.update { it.copy(manualCost = value) }
    fun setMargin(value: String) = _state.update { it.copy(margin = value) }
    fun setGoal(value: String) = _state.update { it.copy(goal = value) }
    fun setGoalPrice(value: String) = _state.update { it.copy(goalPrice = value) }

    fun setComparePrice(index: Int, value: String) = _state.update { s ->
        if (index !in s.comparePrices.indices) s else {
            s.copy(comparePrices = s.comparePrices.mapIndexed { i, old -> if (i == index) value else old })
        }
    }

    fun addComparePrice() = _state.update { s ->
        if (s.comparePrices.size >= MAX_COMPARE_PRICES) s else s.copy(comparePrices = s.comparePrices + "")
    }

    fun removeComparePrice(index: Int) = _state.update { s ->
        if (s.comparePrices.size <= 2 || index !in s.comparePrices.indices) s else {
            s.copy(comparePrices = s.comparePrices.filterIndexed { i, _ -> i != index })
        }
    }

    /** Guarda el precio recomendado como precio de venta del producto seleccionado. */
    fun applyPrice(pricePesos: Long) {
        val product = _state.value.selectedProduct ?: return
        viewModelScope.launch {
            try {
                productRepository.save(product.product.copy(salePricePesos = pricePesos), product.items)
                _state.update { it.copy(message = "Precio de venta actualizado en \"${product.product.name}\".") }
            } catch (e: Exception) {
                _state.update { it.copy(message = "No se pudo actualizar el precio.") }
            }
        }
    }

    fun saveCalculation(title: String, detail: String) {
        viewModelScope.launch {
            try {
                historyRepository.log(HistoryType.CALCULATION, title, detail)
                _state.update { it.copy(message = "Cálculo guardado en el historial.") }
            } catch (e: Exception) {
                _state.update { it.copy(message = "No se pudo guardar el cálculo.") }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
