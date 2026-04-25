package com.sindromradiospb.ttsprototypev2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId

@Composable
fun AppRoot() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Classic", "IDE")

    Scaffold(
        topBar = {
            Column {
                Text(
                    text = "Hebrew/Russian TTS",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ClassicModeScreen(Modifier.padding(innerPadding))
            else -> IdeModeScreen(Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicModeScreen(modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("שלום, זהו אבטיפוס פשוט של המערכת") }
    var translationProvider by remember { mutableStateOf(TranslationProviderId.GoogleTranslateFree) }
    var ttsProvider by remember { mutableStateOf(TtsProviderId.GoogleOnlineTts) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Hebrew source text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
        )

        ProviderChips(
            translationProvider = translationProvider,
            onTranslationProviderChanged = { translationProvider = it },
            ttsProvider = ttsProvider,
            onTtsProviderChanged = { ttsProvider = it },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { }) {
                Text("Generate table")
            }
            Button(onClick = { }) {
                Text("Speak")
            }
        }

        ResultPreviewCard()
        LibraryActionCard()
    }
}

@Composable
private fun ProviderChips(
    translationProvider: TranslationProviderId,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    ttsProvider: TtsProviderId,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Translation provider", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TranslationProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = translationProvider == provider,
                    onClick = { onTranslationProviderChanged(provider) },
                    label = { Text(provider.wireId) },
                )
            }
        }

        Text("TTS provider", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TtsProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = ttsProvider == provider,
                    onClick = { onTtsProviderChanged(provider) },
                    label = { Text(provider.wireId) },
                )
            }
        }
    }
}

@Composable
private fun ResultPreviewCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("Library: draft") })
                AssistChip(onClick = { }, label = { Text("Audio: missing") })
            }
            Text("Generated result", style = MaterialTheme.typography.titleMedium)
            Text("Hebrew: שלום", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Text("Niqqud: שָׁלוֹם", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Text("Translit: shalom")
            Text("Russian: привет")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }) { Text("Play row") }
                Button(onClick = { }) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun LibraryActionCard() {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local library", style = MaterialTheme.typography.titleMedium)
            Text("Room/SQLite and ZIP export are the required storage path. Cloud storage is out of runtime scope.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }) { Text("Save") }
                Button(onClick = { }) { Text("Export ZIP") }
            }
        }
    }
}

@Composable
fun IdeModeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AssistChip(onClick = { }, label = { Text("Experimental / under development") })
        Text("IDE Mode", style = MaterialTheme.typography.titleLarge)
        Text("This screen stays available for migration continuity, but Classic Mode remains the production workflow.")
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Planned IDE shell")
                Text("Library search, selected text table, notes, SRS, audio metadata, and export panels will reuse the same typed models as Classic Mode.")
            }
        }
    }
}
