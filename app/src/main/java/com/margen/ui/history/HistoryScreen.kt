package com.margen.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.margen.data.HistoryRepository
import com.margen.domain.model.HistoryEntry
import com.margen.ui.components.EmptyState
import com.margen.ui.components.MargenTopBar
import com.margen.ui.components.SectionCard
import com.margen.ui.margenViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Fecha y hora local con formato colombiano: 21/11/2026 14:30. */
fun formatTimestamp(millis: Long): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

data class HistoryState(
    val loaded: Boolean = false,
    val entries: List<HistoryEntry> = emptyList()
)

class HistoryViewModel(historyRepository: HistoryRepository) : ViewModel() {
    val state: StateFlow<HistoryState> = historyRepository.entries
        .map { HistoryState(loaded = true, entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())
}

@Composable
fun HistoryScreen() {
    val vm = margenViewModel { c -> HistoryViewModel(c.historyRepository) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { MargenTopBar("Historial") },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        when {
            !state.loaded -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.entries.isEmpty() -> EmptyState(
                title = "Sin actividad todavía",
                message = "Aquí aparecerán los productos que crees, edites o elimines y los cálculos que guardes.",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    SectionCard {
                        Text(
                            text = entry.type.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                        if (entry.detail.isNotBlank()) {
                            Text(
                                text = entry.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatTimestamp(entry.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
