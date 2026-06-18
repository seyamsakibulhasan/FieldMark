package com.fieldmark.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fieldmark.app.R
import com.fieldmark.app.compass.rememberCompass
import com.fieldmark.app.ui.components.CompassRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = rememberCompass(ctx)
    val heading by repo.heading.collectAsState()
    val pitch by repo.pitch.collectAsState()
    val roll by repo.roll.collectAsState()

    val cardinal = when (heading) {
        in 0f..22.5f, in 337.5f..360f -> stringResource(R.string.north)
        in 22.5f..67.5f -> stringResource(R.string.northeast)
        in 67.5f..112.5f -> stringResource(R.string.east)
        in 112.5f..157.5f -> stringResource(R.string.southeast)
        in 157.5f..202.5f -> stringResource(R.string.south)
        in 202.5f..247.5f -> stringResource(R.string.southwest)
        in 247.5f..292.5f -> stringResource(R.string.west)
        in 292.5f..337.5f -> stringResource(R.string.northwest)
        else -> stringResource(R.string.north)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compass)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                CompassRose(heading = heading, modifier = Modifier.size(300.dp))
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    InfoRow(stringResource(R.string.heading), "${heading.toInt()}°  $cardinal")
                    Spacer(Modifier.size(8.dp))
                    InfoRow(stringResource(R.string.pitch), "${"%.1f".format(pitch)}°")
                    Spacer(Modifier.size(8.dp))
                    InfoRow(stringResource(R.string.roll), "${"%.1f".format(roll)}°")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(16.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
