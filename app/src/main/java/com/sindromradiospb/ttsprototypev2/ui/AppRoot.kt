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
import com.sindromradiospb.ttsprototypev2.core.model.LibraryRow
import com.sindromradiospb.ttsprototypev2.core.model.LibraryText
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialStatus
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicGeneratedRowUi
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeUiState
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryRowDraft
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryUiState
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryViewModel
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsUiState
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsViewModel

@Composable
fun AppRoot(
    classicModeViewModel: ClassicModeViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Classic", "Library", "Settings", "IDE")
    val classicState by classicModeViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

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
            1 -> LibraryScreen(
                state = libraryState,
                onIncludeArchivedChanged = libraryViewModel::setIncludeArchived,
                onOpenText = libraryViewModel::openText,
                onArchiveSelected = { libraryViewModel.archiveSelected(archived = true) },
                onRestoreSelected = { libraryViewModel.archiveSelected(archived = false) },
                onDeleteSelected = libraryViewModel::deleteSelected,
                onStartEditingRow = libraryViewModel::startEditingRow,
                onStartAddingRow = libraryViewModel::startAddingRow,
                onRowDraftChanged = libraryViewModel::updateRowDraft,
                onSaveEditingRow = libraryViewModel::saveEditingRow,
                onSaveNewRow = libraryViewModel::saveNewRow,
                onResetEditingRow = libraryViewModel::resetEditingRow,
                onResetRow = libraryViewModel::resetRow,
                onCancelRowEdit = libraryViewModel::cancelRowEdit,
                onMoveRow = libraryViewModel::moveRow,
                onDeleteRow = libraryViewModel::deleteRow,
                onDismissMessage = libraryViewModel::clearMessage,
                modifier = Modifier.padding(innerPadding),
            )
            2 -> SettingsScreen(
                state = settingsState,
                onDraftChanged = settingsViewModel::onCredentialDraftChanged,
                onSaveCredential = settingsViewModel::saveCredential,
                onDeleteCredential = settingsViewModel::deleteCredential,
                onDismissMessage = settingsViewModel::clearMessage,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onIncludeArchivedChanged: (Boolean) -> Unit,
    onOpenText: (String) -> Unit,
    onArchiveSelected: () -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onStartEditingRow: (String) -> Unit,
    onStartAddingRow: (String?) -> Unit,
    onRowDraftChanged: (LibraryRowDraft) -> Unit,
    onSaveEditingRow: () -> Unit,
    onSaveNewRow: () -> Unit,
    onResetEditingRow: () -> Unit,
    onResetRow: (String) -> Unit,
    onCancelRowEdit: () -> Unit,
    onMoveRow: (String, Int) -> Unit,
    onDeleteRow: (String) -> Unit,
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
        Text("Local library", style = MaterialTheme.typography.titleLarge)
        Text(
            "Saved texts are stored on this device. Archive hides a text from the default list; delete removes it from Room.",
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = !state.includeArchived,
                onClick = { onIncludeArchivedChanged(false) },
                label = { Text("Active") },
            )
            FilterChip(
                selected = state.includeArchived,
                onClick = { onIncludeArchivedChanged(true) },
                label = { Text("Include archived") },
            )
            AssistChip(onClick = { }, label = { Text("${state.summaries.size} shown") })
        }

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        if (state.summaries.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No saved texts", style = MaterialTheme.typography.titleMedium)
                    Text("Generate and save a Classic Mode result to populate the local library.")
                }
            }
        } else {
            state.summaries.forEach { summary ->
                LibrarySummaryCard(
                    summary = summary,
                    isSelected = state.selectedText?.id == summary.textId,
                    onOpen = { onOpenText(summary.textId) },
                )
            }
        }

        state.selectedText?.let { selected ->
            SelectedLibraryTextCard(
                state = state,
                text = selected,
                isLoading = state.isLoading,
                onArchive = onArchiveSelected,
                onRestore = onRestoreSelected,
                onDelete = onDeleteSelected,
                onStartEditingRow = onStartEditingRow,
                onStartAddingRow = onStartAddingRow,
                onRowDraftChanged = onRowDraftChanged,
                onSaveEditingRow = onSaveEditingRow,
                onSaveNewRow = onSaveNewRow,
                onResetEditingRow = onResetEditingRow,
                onResetRow = onResetRow,
                onCancelRowEdit = onCancelRowEdit,
                onMoveRow = onMoveRow,
                onDeleteRow = onDeleteRow,
            )
        }
    }
}

@Composable
private fun LibrarySummaryCard(
    summary: LibraryTextSummary,
    isSelected: Boolean,
    onOpen: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(summary.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (summary.isArchived) {
                    AssistChip(onClick = { }, label = { Text("Archived") })
                }
            }
            Text("Updated: ${summary.updatedAt}", style = MaterialTheme.typography.bodySmall)
            summary.level?.let { Text("Level: $it", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = onOpen) {
                Text(if (isSelected) "Refresh" else "Open")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedLibraryTextCard(
    state: LibraryUiState,
    text: LibraryText,
    isLoading: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onStartEditingRow: (String) -> Unit,
    onStartAddingRow: (String?) -> Unit,
    onRowDraftChanged: (LibraryRowDraft) -> Unit,
    onSaveEditingRow: () -> Unit,
    onSaveNewRow: () -> Unit,
    onResetEditingRow: () -> Unit,
    onResetRow: (String) -> Unit,
    onCancelRowEdit: () -> Unit,
    onMoveRow: (String, Int) -> Unit,
    onDeleteRow: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text.title, style = MaterialTheme.typography.titleLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = { }, label = { Text("${text.rows.size} rows") })
                AssistChip(onClick = { }, label = { Text(if (text.isArchived) "Archived" else "Active") })
                text.lastOpenedAt?.let { AssistChip(onClick = { }, label = { Text("Opened $it") }) }
            }
            Text(text.sourceText, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (text.isArchived) {
                    Button(onClick = onRestore, enabled = !isLoading) { Text("Restore") }
                } else {
                    OutlinedButton(onClick = onArchive, enabled = !isLoading) { Text("Archive") }
                }
                OutlinedButton(onClick = onDelete, enabled = !isLoading) { Text("Delete") }
                OutlinedButton(
                    onClick = { onStartAddingRow(text.rows.lastOrNull()?.id) },
                    enabled = !isLoading,
                ) {
                    Text("Add row")
                }
                OutlinedButton(onClick = { }, enabled = false) { Text("Export ZIP") }
            }
            text.rows.forEachIndexed { index, row ->
                LibraryRowCard(
                    row = row,
                    isEditing = state.editingRowId == row.id,
                    draft = state.rowDraft,
                    isLoading = isLoading,
                    canMoveUp = index > 0,
                    canMoveDown = index < text.rows.lastIndex,
                    onStartEditing = { onStartEditingRow(row.id) },
                    onStartAddingAfter = { onStartAddingRow(row.id) },
                    onDraftChanged = onRowDraftChanged,
                    onSave = onSaveEditingRow,
                    onReset = onResetEditingRow,
                    onResetWithoutEditor = { onResetRow(row.id) },
                    onCancel = onCancelRowEdit,
                    onMoveUp = { onMoveRow(row.id, -1) },
                    onMoveDown = { onMoveRow(row.id, 1) },
                    onDelete = { onDeleteRow(row.id) },
                )
                if (state.addingAfterRowId == row.id) {
                    RowDraftEditor(
                        title = "New row after ${index + 1}",
                        draft = state.rowDraft,
                        isLoading = isLoading,
                        onDraftChanged = onRowDraftChanged,
                        onSave = onSaveNewRow,
                        onReset = null,
                        onCancel = onCancelRowEdit,
                    )
                }
            }
            if (state.isAddingRow && text.rows.none { it.id == state.addingAfterRowId }) {
                RowDraftEditor(
                    title = "New first row",
                    draft = state.rowDraft,
                    isLoading = isLoading,
                    onDraftChanged = onRowDraftChanged,
                    onSave = onSaveNewRow,
                    onReset = null,
                    onCancel = onCancelRowEdit,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryRowCard(
    row: LibraryRow,
    isEditing: Boolean,
    draft: LibraryRowDraft,
    isLoading: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onStartEditing: () -> Unit,
    onStartAddingAfter: () -> Unit,
    onDraftChanged: (LibraryRowDraft) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onResetWithoutEditor: () -> Unit,
    onCancel: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Row ${row.orderIndex + 1}", style = MaterialTheme.typography.labelLarge)
            if (isEditing) {
                RowDraftEditor(
                    title = "Edit row ${row.orderIndex + 1}",
                    draft = draft,
                    isLoading = isLoading,
                    onDraftChanged = onDraftChanged,
                    onSave = onSave,
                    onReset = onReset,
                    onCancel = onCancel,
                )
            } else {
                Text(row.hebrewPlain, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                if (row.hebrewNiqqud.isNotBlank()) {
                    Text(row.hebrewNiqqud, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
                if (row.translit.isNotBlank()) Text("SBL: ${row.translit}")
                if (row.translitRu.isNotBlank()) Text("Russian phonetic: ${row.translitRu}")
                if (row.russian.isNotBlank()) Text("Russian: ${row.russian}")
                row.audioAssetKey?.let { AssistChip(onClick = { }, label = { Text("Audio linked") }) }
                if (row.editMeta?.edited?.values?.any { it } == true) {
                    AssistChip(onClick = { }, label = { Text("Edited") })
                }
                if (row.editMeta?.added == true) {
                    AssistChip(onClick = { }, label = { Text("Added") })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onStartEditing, enabled = !isLoading) { Text("Edit") }
                    OutlinedButton(onClick = onResetWithoutEditor, enabled = !isLoading) { Text("Reset") }
                    OutlinedButton(onClick = onMoveUp, enabled = !isLoading && canMoveUp) { Text("Up") }
                    OutlinedButton(onClick = onMoveDown, enabled = !isLoading && canMoveDown) { Text("Down") }
                    OutlinedButton(onClick = onStartAddingAfter, enabled = !isLoading) { Text("Add after") }
                    OutlinedButton(onClick = onDelete, enabled = !isLoading) { Text("Delete row") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RowDraftEditor(
    title: String,
    draft: LibraryRowDraft,
    isLoading: Boolean,
    onDraftChanged: (LibraryRowDraft) -> Unit,
    onSave: () -> Unit,
    onReset: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = draft.hebrewPlain,
                onValueChange = { onDraftChanged(draft.copy(hebrewPlain = it)) },
                label = { Text("Hebrew original") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                enabled = !isLoading,
            )
            OutlinedTextField(
                value = draft.hebrewNiqqud,
                onValueChange = { onDraftChanged(draft.copy(hebrewNiqqud = it)) },
                label = { Text("Hebrew with niqqud") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                enabled = !isLoading,
            )
            OutlinedTextField(
                value = draft.translit,
                onValueChange = { onDraftChanged(draft.copy(translit = it)) },
                label = { Text("SBL transliteration") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            )
            OutlinedTextField(
                value = draft.translitRu,
                onValueChange = { onDraftChanged(draft.copy(translitRu = it)) },
                label = { Text("Russian phonetic") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            )
            OutlinedTextField(
                value = draft.russian,
                onValueChange = { onDraftChanged(draft.copy(russian = it)) },
                label = { Text("Russian translation") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave, enabled = !isLoading) { Text("Save row") }
                if (onReset != null) {
                    OutlinedButton(onClick = onReset, enabled = !isLoading) { Text("Reset row") }
                }
                OutlinedButton(onClick = onCancel, enabled = !isLoading) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDraftChanged: (ProviderCredentialId, String) -> Unit,
    onSaveCredential: (ProviderCredentialId) -> Unit,
    onDeleteCredential: (ProviderCredentialId) -> Unit,
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
        Text("Provider settings", style = MaterialTheme.typography.titleLarge)
        Text(
            "Credentials are stored locally through Android Keystore encrypted storage. They are not exported, logged, or committed.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Do not paste service account JSON. Android v2 accepts only single-line restricted keys until brokered auth is designed.",
            style = MaterialTheme.typography.bodyMedium,
        )

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        state.credentialStatuses.forEach { status ->
            ProviderCredentialCard(
                status = status,
                draft = state.drafts[status.id].orEmpty(),
                onDraftChanged = { onDraftChanged(status.id, it) },
                onSave = { onSaveCredential(status.id) },
                onDelete = { onDeleteCredential(status.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProviderCredentialCard(
    status: ProviderCredentialStatus,
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(status.id.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = { },
                    label = { Text(if (status.isConfigured) "Configured" else "Missing") },
                )
            }
            Text(status.id.usage, style = MaterialTheme.typography.bodySmall)
            Text("Provider ID: ${status.id.wireId}", style = MaterialTheme.typography.bodySmall)
            Text("Stored value: ${status.maskedValue ?: "Not configured"}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                label = { Text("New single-line credential") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSave) { Text("Save credential") }
                OutlinedButton(onClick = onDelete, enabled = status.isConfigured) { Text("Delete credential") }
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
