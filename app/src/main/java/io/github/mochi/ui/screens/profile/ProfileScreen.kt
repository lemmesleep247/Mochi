package io.github.mochi.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.mochi.data.model.MalProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onConnectForStats: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
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
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
    ) { padding ->
        when {
            uiState.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.profile == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Couldn't load your profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> ProfileContent(
                profile = uiState.profile!!,
                hasScrapeSession = uiState.hasScrapeSession,
                onConnectForStats = onConnectForStats,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: MalProfile,
    hasScrapeSession: Boolean,
    onConnectForStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = profile.picture,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                if (profile.joinedAt.isNotBlank()) {
                    Text(
                        "Joined ${profile.joinedAt.take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("Anime", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        StatsGrid(
            listOf(
                "Days" to "%.1f".format(profile.animeDaysWatched),
                "Mean score" to "%.2f".format(profile.animeMeanScore),
                "Episodes" to profile.animeEpisodesWatched.toString(),
                "Watching" to profile.animeWatching.toString(),
                "Completed" to profile.animeCompleted.toString(),
                "On hold" to profile.animeOnHold.toString(),
                "Dropped" to profile.animeDropped.toString(),
                "Plan to watch" to profile.animePlanToWatch.toString(),
            ),
        )
        Spacer(Modifier.height(24.dp))

        Text("Manga", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        when {
            !hasScrapeSession -> Card(onClick = onConnectForStats, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Connect your MyAnimeList account", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Manga stats aren't available through MyAnimeList's official API — sign in on the website to unlock them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            !profile.hasMangaStats -> Text(
                "Couldn't load manga stats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> StatsGrid(
                listOf(
                    "Days" to "%.1f".format(profile.mangaDaysRead),
                    "Mean score" to "%.2f".format(profile.mangaMeanScore),
                    "Chapters" to profile.mangaChaptersRead.toString(),
                    "Reading" to profile.mangaReading.toString(),
                    "Completed" to profile.mangaCompleted.toString(),
                    "On hold" to profile.mangaOnHold.toString(),
                    "Dropped" to profile.mangaDropped.toString(),
                    "Plan to read" to profile.mangaPlanToRead.toString(),
                ),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatsGrid(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier) {
        stats.chunked(2).forEach { rowPair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowPair.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(value, style = MaterialTheme.typography.titleMedium)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
