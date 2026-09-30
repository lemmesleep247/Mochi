package io.github.mochi.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mochi.R
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.model.label
import io.github.mochi.data.model.listStatuses
import io.github.mochi.ui.components.MediaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onOpenDetail: (Int, MediaType) -> Unit,
    viewModel: ListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The real bottom nav bar (MochiNavHost) already reserves its own
        // system-bar inset; this screen only needs to reserve the top one.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = if (uiState.selectedType == MediaType.Anime) 0 else 1) {
                Tab(
                    selected = uiState.selectedType == MediaType.Anime,
                    onClick = { viewModel.selectTab(MediaType.Anime) },
                    text = { Text("Anime") },
                )
                Tab(
                    selected = uiState.selectedType == MediaType.Manga,
                    onClick = { viewModel.selectTab(MediaType.Manga) },
                    text = { Text("Manga") },
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.statusFilter == null,
                        onClick = { viewModel.selectStatusFilter(null) },
                        label = { Text("All") },
                    )
                }
                items(uiState.selectedType.listStatuses()) { status ->
                    FilterChip(
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.selectStatusFilter(status) },
                        label = { Text(status.label()) },
                    )
                }
            }

            when {
                uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { onOpenDetail(item.id, item.type) },
                            onQuickIncrement = { viewModel.quickIncrement(item) },
                        )
                    }
                }
            }
        }
    }
}
