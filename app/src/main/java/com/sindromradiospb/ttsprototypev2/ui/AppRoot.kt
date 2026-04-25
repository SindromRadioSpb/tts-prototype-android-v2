package com.sindromradiospb.ttsprototypev2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicGeneratedRowUi
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeUiState
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel

@Composable
fun AppRoot(classicModeViewModel: ClassicModeViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Classic", "IDE")
    val classicState by classicModeViewModel.uiState.collectAsState()

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
            0 -> ClassicModeScreen(
                state = classicState,
                onSourceTextChanged = classicModeViewModel::onSourceTextChanged,
                onTranslationProviderChanged = classicModeViewModel::onTranslationProviderChanged,
                onTtsProviderChanged = classicModeViewModel::onTtsProviderChanged,
                onGenerate = classicModeViewModel::generateTable,
                onSave = classicModeViewModel::saveCurrent,
                onDismissMessage = classicModeViewModel::clearMessage,
                modifier = Modifier.padding(innerPadding),
            )
            else -> IdeModeScreen(Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicModeScreen(
    state: ClassicModeUiState,
    onSourceTextChanged: (String) -> Unit,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.sourceText,
            onValueChange = onSourceTextChanged,
            label = { Text("Hebrew source text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
            enabled = !state.isGenerating && !state.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
        )

        ProviderChips(
            translationProvider = state.translationProvider,
            onTranslationProviderChanged = onTranslationProviderChanged,
            ttsProvider = state.ttsProvider,
            onTtsProviderChanged = onTtsProviderChanged,
        )

        ActionButtons(
            state = state,
            onGenerate = onGenerate,
            onSave = onSave,
        )

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        ResultPreviewCard(
            rows = state.rows,
            generatedAt = state.generatedAt,
            generationLabel = state.generationLabel,
        )
        LibraryActionCard(
            summaries = state.libraryTexts,
            savedTextId = state.savedTextId,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderChips(
    translationProvider: TranslationProviderId,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    ttsProvider: TtsProviderId,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Translation provider", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TranslationProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = translationProvider == provider,
                    onClick = { onTranslationProviderChanged(provider) },
                    label = { Text(provider.wireId) },
                )
            }
        }

        Text("TTS provider", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionButtons(
    state: ClassicModeUiState,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onGenerate,
            enabled = !state.isGenerating && !state.isSaving,
        ) {
            Text(if (state.isGenerating) "Generating..." else "Generate table")
        }
        Button(
            onClick = onSave,
            enabled = state.rows.isNotEmpty() && !state.isGenerating && !state.isSaving,
        ) {
            Text(if (state.isSaving) "Saving..." else "Save")
        }
        OutlinedButton(
            onClick = { },
            enabled = false,
        ) {
            Text("Speak")
        }
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message)
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ResultPreviewCard(
    rows: List<ClassicGeneratedRowUi>,
    generatedAt: String?,
    generationLabel: String,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text(generationLabel) })
                AssistChip(onClick = { }, label = { Text("Audio: missing") })
            }
            Text("Generated result", style = MaterialTheme.typography.titleMedium)
            if (generatedAt != null) {
                Text("Generated at: $generatedAt", style = MaterialTheme.typography.bodySmall)
            }
            if (rows.isEmpty()) {
                Text("No rows yet. Enter Hebrew text and generate the table.")
            } else {
                rows.forEach { row ->
                    GeneratedRowCard(row)
                }
            }
        }
    }
}

@Composable
private fun GeneratedRowCard(row: ClassicGeneratedRowUi) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Row ${row.orderIndex + 1}", style = MaterialTheme.typography.labelLarge)
            Text("Hebrew: ${row.hebrewPlain}", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Text("Niqqud: not generated in M2", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Text("Translit: ${row.translit}")
            Text("Russian phonetic: ${row.translitRu}")
            Text("Russian: ${row.russian}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { }, enabled = false) { Text("Play row") }
                OutlinedButton(onClick = { }, enabled = false) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun LibraryActionCard(
    summaries: List<LibraryTextSummary>,
    savedTextId: String?,
) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Local library", style = MaterialTheme.typography.titleMedium)
            Text("Saved texts are persisted locally with Room/SQLite. Cloud storage is out of runtime scope.")
            Text("Saved texts: ${summaries.size}")
            if (savedTextId != null) {
                AssistChip(onClick = { }, label = { Text("Current text saved") })
            }
            summaries.take(3).forEach { summary ->
                Text("${summary.title} - updated ${summary.updatedAt}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { }, enabled = false) {
                Text("Export ZIP")
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
