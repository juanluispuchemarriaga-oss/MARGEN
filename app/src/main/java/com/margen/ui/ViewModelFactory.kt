package com.margen.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.margen.AppContainer
import com.margen.MargenApp

/**
 * Crea un ViewModel inyectándole el contenedor de dependencias.
 * Uso: val vm = margenViewModel { c -> MiViewModel(c.productRepository) }
 * [key] permite tener un ViewModel distinto por argumento (por ejemplo, por id de producto).
 */
@Composable
inline fun <reified VM : ViewModel> margenViewModel(
    key: String? = null,
    crossinline builder: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as MargenApp).container
    return viewModel<VM>(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = builder(container) as T
        }
    )
}
