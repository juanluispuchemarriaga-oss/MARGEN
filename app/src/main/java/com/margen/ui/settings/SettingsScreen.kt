package com.margen.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.core.MoneyFormat
import com.margen.data.ProductRepository
import com.margen.data.SettingsStore
import com.margen.domain.model.ThemeMode
import com.margen.ui.components.ConfirmDialog
import com.margen.ui.components.InfoNote
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.ResultRow
import com.margen.ui.components.SectionCard
import com.margen.ui.components.SectionTitle
import com.margen.ui.margenViewModel
import java.math.BigDecimal
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val productRepository: ProductRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode

    fun setTheme(mode: ThemeMode) = settingsStore.setThemeMode(mode)

    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            productRepository.clearAllData()
            onDone()
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun SettingsScreen() {
    val vm = margenViewModel { c -> SettingsViewModel(c.settingsStore, c.productRepository) }
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClear by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    Scaffold(
        topBar = { MargenTopBar("Configuración") },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard {
                SectionTitle("Apariencia")
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = themeMode == mode,
                                onClick = { vm.setTheme(mode) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = null)
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            SectionCard {
                SectionTitle("Moneda y formato")
                ResultRow("Moneda", "Peso colombiano (COP)")
                ResultRow(
                    "Ejemplo",
                    MoneyFormat.cop(1234567) + " · " + MoneyFormat.percent(BigDecimal("12.5"))
                )
                InfoNote("Los números usan el formato colombiano: punto para miles y coma para decimales.")
            }

            SectionCard {
                SectionTitle("Tus datos")
                InfoNote(
                    "Todo se guarda solo en este teléfono. MARGEN no usa Internet, cuentas ni servidores."
                )
                OutlinedButton(
                    onClick = { showClear = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Borrar todos los datos", color = MaterialTheme.colorScheme.error)
                }
            }

            SectionCard {
                SectionTitle("Acerca de MARGEN")
                ResultRow("Versión", version)
                Text(
                    text = "Conoce lo que realmente ganas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoNote("Funciona sin conexión y no solicita permisos. Los resultados son estimaciones basadas en los datos que ingresas.")
            }
        }
    }

    if (showClear) {
        ConfirmDialog(
            title = "¿Borrar todos los datos?",
            message = "Se eliminarán todos tus productos, costos e historial de este teléfono. " +
                "Esta acción no se puede deshacer.",
            confirmLabel = "Borrar todo",
            onConfirm = {
                showClear = false
                vm.clearAll {
                    scope.launch { snackbar.showSnackbar("Todos los datos fueron eliminados.") }
                }
            },
            onDismiss = { showClear = false }
        )
    }
}
