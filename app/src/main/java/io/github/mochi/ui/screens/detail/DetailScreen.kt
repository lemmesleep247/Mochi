package io.github.mochi.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.mochi.data.model.ListStatus
import io.github.mochi.data.model.MediaItem
import io.github.mochi.data.model.MediaType
import io.github.mochi.data.model.label
import io.github.mochi.data.model.listStatuses

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    id: Int,
    type: MediaType,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(id, type) { viewModel.load(id, type) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.item == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Couldn't load this title", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> DetailContent(
                item = uiState.item!!,
                saving = uiState.saving,
                modifier = Modifier.padding(padding),
                onStatusChange = viewModel::updateStatus,
                onProgressChange = viewModel::updateProgress,
                onScoreChange = viewModel::updateScore,
                onSave = viewModel::save,
                onRemove = viewModel::remove,
            )
        }
    }
}

@Composable
private fun DetailContent(
    item: MediaItem,
    saving: Boolean,
    onStatusChange: (ListStatus) -> Unit,
    onProgressChange: (Int) -> Unit,
    onScoreChange: (Int) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(16.dp)) {
            AsyncImage(
                model = item.cover,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 120.dp, height = 170.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                if (item.titleEnglish.isNotBlank() && item.titleEnglish != item.title) {
                    Text(
                        item.titleEnglish,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    listOf(item.format, item.startYear, item.airStatus).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.creators.isNotBlank()) {
                    Text(
                        item.creators,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Stat(label = "Score", value = if (item.score > 0) "%.2f".format(item.score) else "—")
                    Stat(label = "Rank", value = if (item.rank > 0) "#${item.rank}" else "—")
                }
            }
        }

        if (item.genres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(item.genres) { genre -> AssistChip(onClick = {}, label = { Text(genre) }) }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (item.synopsis.isNotBlank()) {
            Text(
                item.synopsis,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(16.dp))

        Text(
            "Your list",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))

        StatusPicker(
            type = item.type,
            selected = item.listStatus,
            onSelect = onStatusChange,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))

        Stepper(
            label = if (item.type == MediaType.Anime) "Episodes" else "Chapters",
            valueLabel = if (item.totalUnits > 0) "${item.progress} / ${item.totalUnits}" else "${item.progress}",
            onDecrement = { onProgressChange(item.progress - 1) },
            onIncrement = { onProgressChange(item.progress + 1) },
            canDecrement = item.progress > 0,
            canIncrement = item.totalUnits == 0 || item.progress < item.totalUnits,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))

        Stepper(
            label = "Your score",
            valueLabel = if (item.myScore > 0) "${item.myScore} / 10" else "Not scored",
            onDecrement = { onScoreChange(item.myScore - 1) },
            onIncrement = { onScoreChange(item.myScore + 1) },
            canDecrement = item.myScore > 0,
            canIncrement = item.myScore < 10,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(24.dp))

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.weight(1f)) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                    )
                } else {
                    Text(if (item.inList) "Save" else "Add to List")
                }
            }
            if (item.inList) {
                OutlinedButton(onClick = onRemove, modifier = Modifier.weight(1f)) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusPicker(
    type: MediaType,
    selected: ListStatus,
    onSelect: (ListStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            type.listStatuses().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean,
    canIncrement: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onDecrement, enabled = canDecrement) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(
                valueLabel,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledTonalIconButton(onClick = onIncrement, enabled = canIncrement) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
