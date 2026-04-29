package com.sindromradiospb.ttsprototypev2.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialStatus
import com.sindromradiospb.ttsprototypev2.BuildConfig
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicDisclosurePanel
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicHebrewTableFont
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicGeneratedRowUi
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeUiState
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicNoteEditorState
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicSaveMetadataDraft
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicSourceLanguage
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicTableColumn
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicTranslitProfile
import com.sindromradiospb.ttsprototypev2.feature.classic.transliterateHebrewWithProfile
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryTextMetadataDraft
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryUiState
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryViewModel
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsUiState
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsViewModel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AppBackground = Color(0xFFF4F6F8)
private val PanelBackground = Color(0xFFFFFFFF)
private val SoftPanelBackground = Color(0xFFF8FAFB)
private val NeutralButton = Color(0xFFF1F3F5)
private val BorderColor = Color(0xFFE0E5EA)
private val ClassicBlue = Color(0xFF3498DB)
private val ClassicGreen = Color(0xFF2ECC71)
private val ClassicDanger = Color(0xFFE85D75)
private val MutedText = Color(0xFF68737D)
private const val DeveloperPhoneDisplay = "+972535536175"
private const val DeveloperWhatsappUrl = "https://wa.me/972535536175?text=%D0%97%D0%B4%D1%80%D0%B0%D0%B2%D1%81%D1%82%D0%B2%D1%83%D0%B9%D1%82%D0%B5%2C%20%D1%83%20%D0%BC%D0%B5%D0%BD%D1%8F%20%D0%B2%D0%BE%D0%BF%D1%80%D0%BE%D1%81%20%D0%BF%D0%BE%20TTS%20Prototype%20Android"
private const val DeveloperFacebookUrl = "https://www.facebook.com/Like.Egor.Blog"
private const val DeveloperInstagramUrl = "https://www.instagram.com/like.egor.blog/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    classicModeViewModel: ClassicModeViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isLibraryOpen by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf("Classic", "Settings", "IDE", "Связь\nс разработчиком")
    val classicState by classicModeViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                Column(Modifier.background(PanelBackground)) {
                    Text(
                        text = "Hebrew/Russian TTS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, textAlign = TextAlign.Center) },
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
                    onClearSource = { classicModeViewModel.onSourceTextChanged("") },
                    onSourceLanguageChanged = classicModeViewModel::onSourceLanguageChanged,
                    onTranslationProviderChanged = classicModeViewModel::onTranslationProviderChanged,
                    onTtsProviderChanged = classicModeViewModel::onTtsProviderChanged,
                    onTtsVoiceNameChanged = classicModeViewModel::onTtsVoiceNameChanged,
                    onSpeakingRateChanged = classicModeViewModel::onSpeakingRateChanged,
                    onPitchChanged = classicModeViewModel::onPitchChanged,
                    onTranslitProfileChanged = classicModeViewModel::onTranslitProfileChanged,
                    onHebrewTableFontChanged = classicModeViewModel::onHebrewTableFontChanged,
                    onGenerate = classicModeViewModel::generateTable,
                    onSave = classicModeViewModel::saveCurrent,
                    onSaveMetadataDraftChanged = classicModeViewModel::updateSaveMetadataDraft,
                    onCommitSaveMetadata = classicModeViewModel::commitSaveMetadata,
                    onCancelSaveMetadata = classicModeViewModel::cancelSaveMetadata,
                    onSpeak = classicModeViewModel::speakSource,
                    onPlayRow = classicModeViewModel::playRow,
                    onToggleAutoNext = classicModeViewModel::toggleAutoNextPlayback,
                    onOpenRowNote = classicModeViewModel::openRowNote,
                    onSelectRow = classicModeViewModel::selectRow,
                    onToggleTableColumnsPanel = classicModeViewModel::toggleTableColumnsPanel,
                    onToggleTableColumn = classicModeViewModel::toggleTableColumn,
                    onResetTableDisplay = classicModeViewModel::resetTableDisplay,
                    onAdjustTableColumnWidth = classicModeViewModel::adjustTableColumnWidth,
                    onToggleDisclosure = classicModeViewModel::toggleDisclosure,
                    onRowNoteDraftChanged = classicModeViewModel::updateRowNoteDraft,
                    onSaveRowNote = classicModeViewModel::saveRowNote,
                    onDeleteRowNote = classicModeViewModel::deleteRowNote,
                    onCloseRowNote = classicModeViewModel::closeRowNote,
                    onOpenLibrary = { isLibraryOpen = true },
                    onOpenSettings = { selectedTab = 1 },
                    onOpenIde = { selectedTab = 2 },
                    onDismissMessage = classicModeViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                1 -> SettingsScreen(
                    state = settingsState,
                    onDraftChanged = settingsViewModel::onCredentialDraftChanged,
                    onSaveCredential = settingsViewModel::saveCredential,
                    onAttachJsonCredential = settingsViewModel::attachJsonCredential,
                    onValidateCredential = settingsViewModel::validateCredential,
                    onDeleteCredential = settingsViewModel::deleteCredential,
                    onDismissMessage = settingsViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                2 -> IdeModeScreen(Modifier.padding(innerPadding))

                else -> DeveloperContactScreen(Modifier.padding(innerPadding))
            }
        }

        if (isLibraryOpen) {
            LibraryV3Modal(
                state = libraryState,
                classicState = classicState,
                onClose = { isLibraryOpen = false },
                onSaveCurrent = classicModeViewModel::saveCurrent,
                onOpenInClassic = { textId, resume ->
                    classicModeViewModel.openLibraryText(textId, resume)
                    isLibraryOpen = false
                    selectedTab = 0
                },
                onArchiveText = libraryViewModel::archiveText,
                onRequestDeleteText = libraryViewModel::requestDeleteText,
                onConfirmDeleteText = libraryViewModel::deleteText,
                onCancelDeleteText = libraryViewModel::cancelDeleteText,
                onStartMetadataEdit = libraryViewModel::startEditingMetadata,
                onMetadataDraftChanged = libraryViewModel::updateMetadataDraft,
                onSaveMetadata = libraryViewModel::saveMetadata,
                onCancelMetadataEdit = libraryViewModel::cancelMetadataEdit,
                onImportLibraryJson = libraryViewModel::importLegacyWebLibraryJson,
                onImportZipBundle = libraryViewModel::importZipBundle,
                onDismissMessage = libraryViewModel::clearMessage,
            )
        }

        if (selectedTab == 0 && !isLibraryOpen) {
            ClassicQuickControlsOverlay(
                isLandscape = isLandscape,
                onToggleOrientation = {
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                },
                onOpenEditMode = { selectedTab = 2 },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassicModeScreen(
    state: ClassicModeUiState,
    onSourceTextChanged: (String) -> Unit,
    onClearSource: () -> Unit,
    onSourceLanguageChanged: (ClassicSourceLanguage) -> Unit,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
    onTtsVoiceNameChanged: (String?) -> Unit,
    onSpeakingRateChanged: (Double) -> Unit,
    onPitchChanged: (Double) -> Unit,
    onTranslitProfileChanged: (ClassicTranslitProfile) -> Unit,
    onHebrewTableFontChanged: (ClassicHebrewTableFont) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onSaveMetadataDraftChanged: (ClassicSaveMetadataDraft) -> Unit,
    onCommitSaveMetadata: () -> Unit,
    onCancelSaveMetadata: () -> Unit,
    onSpeak: () -> Unit,
    onPlayRow: (Int) -> Unit,
    onToggleAutoNext: () -> Unit,
    onOpenRowNote: (Int) -> Unit,
    onSelectRow: (Int) -> Unit,
    onToggleTableColumnsPanel: () -> Unit,
    onToggleTableColumn: (ClassicTableColumn) -> Unit,
    onResetTableDisplay: () -> Unit,
    onAdjustTableColumnWidth: (ClassicTableColumn, Float) -> Unit,
    onToggleDisclosure: (ClassicDisclosurePanel) -> Unit,
    onRowNoteDraftChanged: (String) -> Unit,
    onSaveRowNote: () -> Unit,
    onDeleteRowNote: () -> Unit,
    onCloseRowNote: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIde: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenScrollState = rememberScrollState()
    val tableScenarioRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(state.generatedAt, state.rows.size, state.isGenerating, state.disclosure.tableOpen) {
        if (state.rows.isNotEmpty() && !state.isGenerating && state.disclosure.tableOpen) {
            delay(180)
            tableScenarioRequester.bringIntoView()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(screenScrollState)
            .background(AppBackground)
            .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ClassicStatusStrip(
            state = state,
            isOpen = state.disclosure.limitsOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Limits) },
        )

        SurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Classic Mode", style = MaterialTheme.typography.labelLarge, color = MutedText)
                    Text(
                        "Текст для озвучки и перевода",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Сценарий: исходный текст -> обработка -> результат -> экспорт и работа по строкам.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton("📚 Библиотека", onOpenLibrary)
                SecondaryActionButton("Ключи", onOpenSettings)
                SecondaryActionButton("🖥️ IDE режим", onOpenIde)
            }
        }

        SourceComposerCard(
            state = state,
            isOpen = state.disclosure.sourceOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Source) },
            onSourceTextChanged = onSourceTextChanged,
            onClearSource = onClearSource,
            onGenerate = onGenerate,
            onSpeak = onSpeak,
        )

        VoiceSettingsCard(
            state = state,
            isOpen = state.disclosure.voiceOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Voice) },
            onSourceLanguageChanged = onSourceLanguageChanged,
            onTtsProviderChanged = onTtsProviderChanged,
            onTtsVoiceNameChanged = onTtsVoiceNameChanged,
            onSpeakingRateChanged = onSpeakingRateChanged,
            onPitchChanged = onPitchChanged,
            onOpenSettings = onOpenSettings,
        )

        TranslationTableSettingsCard(
            state = state,
            isOpen = state.disclosure.translationOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Translation) },
            onTranslationProviderChanged = onTranslationProviderChanged,
            onTranslitProfileChanged = onTranslitProfileChanged,
            onHebrewTableFontChanged = onHebrewTableFontChanged,
            onOpenSettings = onOpenSettings,
        )

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        ClassicResultCard(
            isOpen = state.disclosure.resultOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Result) },
            rows = state.rows,
            generatedAt = state.generatedAt,
            generationLabel = state.generationLabel,
            onSave = onSave,
            isSaveEnabled = state.rows.isNotEmpty() && !state.isSaving && !state.isGenerating,
            isSaving = state.isSaving,
        )

        ClassicTableCard(
            state = state,
            isOpen = state.disclosure.tableOpen,
            onToggle = { onToggleDisclosure(ClassicDisclosurePanel.Table) },
            tableScenarioModifier = Modifier.bringIntoViewRequester(tableScenarioRequester),
            rows = state.rows,
            onPlayRow = onPlayRow,
            onToggleAutoNext = onToggleAutoNext,
            onOpenRowNote = onOpenRowNote,
            onSelectRow = onSelectRow,
            onToggleTableColumnsPanel = onToggleTableColumnsPanel,
            onToggleTableColumn = onToggleTableColumn,
            onResetTableDisplay = onResetTableDisplay,
            onAdjustTableColumnWidth = onAdjustTableColumnWidth,
        )

        LocalLibrarySummaryCard(
            summaries = state.libraryTexts,
            savedTextId = state.savedTextId,
            onOpenLibrary = onOpenLibrary,
        )

        state.saveMetadataDraft?.let { draft ->
            ClassicSaveMetadataDialog(
                draft = draft,
                isSaving = state.isSaving,
                onDraftChanged = onSaveMetadataDraftChanged,
                onSave = onCommitSaveMetadata,
                onCancel = onCancelSaveMetadata,
            )
        }

        state.noteEditor?.let { editor ->
            RowNoteDialog(
                editor = editor,
                onDraftChanged = onRowNoteDraftChanged,
                onSave = onSaveRowNote,
                onDelete = onDeleteRowNote,
                onClose = onCloseRowNote,
            )
        }
    }
}

@Composable
private fun ClassicStatusStrip(
    state: ClassicModeUiState,
    isOpen: Boolean,
    onToggle: () -> Unit,
) {
    DisclosureSurfaceCard(
        title = "Лимиты и квоты",
        subtitle = "Локальные счётчики символов, строк и будущих provider quotas.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuotaTile(
                value = state.sourceText.length.toString(),
                label = "TTS Символы",
                subLabel = "Через это приложение",
                modifier = Modifier.weight(1f),
            )
            QuotaTile(
                value = state.rows.size.toString(),
                label = "Запросы AI",
                subLabel = "Табличные строки",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Provider quotas are shown as local app counters until provider billing telemetry is implemented.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
        )
    }
}

@Composable
private fun ClassicQuickControlsOverlay(
    isLandscape: Boolean,
    onToggleOrientation: () -> Unit,
    onOpenEditMode: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        OutlinedButton(
            onClick = onToggleOrientation,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PanelBackground.copy(alpha = 0.96f),
                contentColor = Color(0xFF1F2D3A),
            ),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .widthIn(min = 58.dp),
        ) {
            Text(
                text = if (isLandscape) "↕ Портрет" else "↔ Альбом",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onOpenEditMode,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClassicBlue, contentColor = Color.White),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .widthIn(min = 58.dp),
        ) {
            Text(
                text = "✎ Редактор",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuotaTile(value: String, label: String, subLabel: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SoftPanelBackground),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE5EEF2), RoundedCornerShape(99.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.08f)
                        .height(6.dp)
                        .background(ClassicGreen, RoundedCornerShape(99.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceComposerCard(
    state: ClassicModeUiState,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onSourceTextChanged: (String) -> Unit,
    onClearSource: () -> Unit,
    onGenerate: () -> Unit,
    onSpeak: () -> Unit,
) {
    DisclosureSurfaceCard(
        title = "Исходный текст",
        subtitle = "Текст, сборка таблицы и быстрое озвучивание источника.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Здесь начинается основной сценарий. Состояние текста и результата должно читаться мгновенно.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText,
                )
            }
            SecondaryActionButton("Очистить", onClearSource, enabled = state.sourceText.isNotEmpty())
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(if (state.sourceText.isBlank()) "Текст: пусто" else "Текст: ${state.sourceText.length} симв.")
            StatusPill(if (state.rows.isEmpty()) "Результат: отсутствует" else "Результат: ${state.rows.size} строк")
            StatusPill("Источник: локальный ввод")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.sourceText,
            onValueChange = onSourceTextChanged,
            placeholder = { Text("Введите текст здесь... (Иврит, Английский, Русский)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 7,
            enabled = !state.isGenerating && !state.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryGreenButton(
                text = if (state.isGenerating) "Сборка..." else if (state.rows.isEmpty()) "Собрать таблицу" else "Пересобрать таблицу",
                onClick = onGenerate,
                enabled = !state.isGenerating && !state.isSaving,
            )
            PrimaryBlueButton(
                text = if (state.isSpeaking) "Озвучивание..." else "🔊 Озвучить",
                onClick = onSpeak,
                enabled = !state.isGenerating && !state.isSaving && !state.isSpeaking,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Ctrl shortcuts from the web version are intentionally not primary on Android; touch actions are first-class.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedText,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceSettingsCard(
    state: ClassicModeUiState,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onSourceLanguageChanged: (ClassicSourceLanguage) -> Unit,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
    onTtsVoiceNameChanged: (String?) -> Unit,
    onSpeakingRateChanged: (Double) -> Unit,
    onPitchChanged: (Double) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isSystemFallback = state.ttsProvider == TtsProviderId.SystemFallbackLowQuality
    DisclosureSurfaceCard(
        title = "Настройки озвучки",
        subtitle = "Язык, голос, темп и ключи провайдера.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        Spacer(Modifier.height(10.dp))
        LabeledChoiceRow(
            label = "Язык исходного текста",
            options = ClassicSourceLanguage.entries,
            selected = state.sourceLanguage,
            optionLabel = { it.label },
            onSelected = onSourceLanguageChanged,
        )
        LabeledChoiceRow(
            label = "Провайдер озвучки",
            options = TtsProviderId.entries,
            selected = state.ttsProvider,
            optionLabel = { ttsProviderLabel(it) },
            onSelected = onTtsProviderChanged,
        )
        if (isSystemFallback) {
            Text(
                "System fallback использует установленный TTS-движок Android. Выбор голоса, скорости и тона недоступен в этом режиме; если в эмуляторе нет системного TTS-движка или языковых данных, воспроизведение завершится ошибкой ProviderUnavailable/UnsupportedLanguage.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
            )
        } else {
            LabeledChoiceRow(
                label = "Голос TTS",
                options = TtsVoiceOptions,
                selected = state.ttsVoiceName.orEmpty(),
                optionLabel = { if (it.isBlank()) "Авто (по умолчанию)" else it },
                onSelected = { onTtsVoiceNameChanged(it.ifBlank { null }) },
            )
            LabeledSlider(
                label = "Скорость речи",
                valueText = String.format(Locale.US, "%.2f×", state.speakingRate),
                value = state.speakingRate.toFloat(),
                valueRange = 0.5f..2.0f,
                steps = 14,
                onValueChange = { onSpeakingRateChanged(it.toDouble()) },
            )
            LabeledSlider(
                label = "Тон (pitch)",
                valueText = String.format(Locale.US, "%.1f", state.pitch),
                value = state.pitch.toFloat(),
                valueRange = -5f..5f,
                steps = 19,
                onValueChange = { onPitchChanged(it.toDouble()) },
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton("🔑 Ключи провайдера", onOpenSettings)
            StatusPill("TTS: ${ttsProviderLabel(state.ttsProvider)}")
            StatusPill("Голос: ${if (isSystemFallback) "системный" else state.ttsVoiceName ?: "авто"}")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranslationTableSettingsCard(
    state: ClassicModeUiState,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    onTranslitProfileChanged: (ClassicTranslitProfile) -> Unit,
    onHebrewTableFontChanged: (ClassicHebrewTableFont) -> Unit,
    onOpenSettings: () -> Unit,
) {
    DisclosureSurfaceCard(
        title = "Настройки перевода и таблицы",
        subtitle = "Переводчик, транслит, шрифт таблицы и сохранение результата.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        Spacer(Modifier.height(10.dp))
        LabeledChoiceRow(
            label = "Провайдер перевода",
            options = TranslationProviderId.entries,
            selected = state.translationProvider,
            optionLabel = { translationProviderLabel(it) },
            onSelected = onTranslationProviderChanged,
        )
        LabeledChoiceRow(
            label = "Профиль транслитерации",
            options = ClassicTranslitProfile.entries,
            selected = state.translitProfile,
            optionLabel = { it.label },
            onSelected = onTranslitProfileChanged,
        )
        LabeledChoiceRow(
            label = "Шрифт иврита табличной части",
            options = ClassicHebrewTableFont.entries,
            selected = state.hebrewTableFont,
            optionLabel = { it.label },
            onSelected = onHebrewTableFontChanged,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton("🔑 Ключи перевода", onOpenSettings)
            StatusPill("Перевод: ${translationProviderLabel(state.translationProvider)}")
            StatusPill(state.translitProfile.label)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> LabeledChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(optionLabel(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MutedText)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

private val TtsVoiceOptions = listOf(
    "",
    "he-IL-Standard-A",
    "he-IL-Standard-B",
    "ru-RU-Standard-A",
    "ru-RU-Standard-B",
    "en-US-Standard-C",
    "en-US-Standard-D",
)

private fun ttsProviderLabel(provider: TtsProviderId): String =
    when (provider) {
        TtsProviderId.GoogleOnlineTts -> "Online TTS"
        TtsProviderId.SystemFallbackLowQuality -> "System fallback"
    }

private fun translationProviderLabel(provider: TranslationProviderId): String =
    when (provider) {
        TranslationProviderId.GoogleTranslateFree -> "Google Translate"
        TranslationProviderId.GcpTranslate -> "GCP Translate"
        TranslationProviderId.GeminiLegacy -> "Gemini (legacy)"
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassicResultCard(
    isOpen: Boolean,
    onToggle: () -> Unit,
    rows: List<ClassicGeneratedRowUi>,
    generatedAt: String?,
    generationLabel: String,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
) {
    DisclosureSurfaceCard(
        title = "Результат",
        subtitle = "Статус формирования, дата генерации и сохранение карточки текста.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(generationLabel)
            StatusPill("Строк: ${rows.size}")
            StatusPill("Audio: ${if (rows.isEmpty()) "missing" else "pending"}")
            if (generatedAt != null) StatusPill("Generated: ${formatDate(generatedAt)}")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "После формирования таблицы сохраните или обновите карточку текста в библиотеке.",
                    color = MutedText,
                )
            }
            PrimaryGreenButton(
                text = if (isSaving) "Сохранение..." else "💾 Обновить",
                onClick = onSave,
                enabled = isSaveEnabled,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassicTableCard(
    state: ClassicModeUiState,
    isOpen: Boolean,
    onToggle: () -> Unit,
    tableScenarioModifier: Modifier = Modifier,
    rows: List<ClassicGeneratedRowUi>,
    onPlayRow: (Int) -> Unit,
    onToggleAutoNext: () -> Unit,
    onOpenRowNote: (Int) -> Unit,
    onSelectRow: (Int) -> Unit,
    onToggleTableColumnsPanel: () -> Unit,
    onToggleTableColumn: (ClassicTableColumn) -> Unit,
    onResetTableDisplay: () -> Unit,
    onAdjustTableColumnWidth: (ClassicTableColumn, Float) -> Unit,
) {
    DisclosureSurfaceCard(
        title = "Таблица",
        subtitle = "Карточка текста, источник, колонки, построчное воспроизведение и заметки.",
        isOpen = isOpen,
        onToggle = onToggle,
    ) {
        LibraryLoadedTableMetadataHeader(
            title = state.loadedTextTitle,
            sourceLabel = state.loadedTextSourceLabel,
            ttsProfileLabel = state.loadedTextTtsProfileLabel,
            translationLabel = state.loadedTextTranslationLabel,
            audioStatusLabel = state.loadedTextAudioStatusLabel,
        )
        if (state.loadedTextTitle != null ||
            !state.loadedTextSourceLabel.isNullOrBlank() ||
            !state.loadedTextTtsProfileLabel.isNullOrBlank() ||
            !state.loadedTextTranslationLabel.isNullOrBlank() ||
            !state.loadedTextAudioStatusLabel.isNullOrBlank()
        ) {
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Таблица с колонками как в Classic Mode: действие, иврит, огласовки, транслит и перевод.", color = MutedText)
            }
        }
        Spacer(Modifier.height(10.dp))
        TableDisplayScenarioPanel(
            state = state,
            modifier = tableScenarioModifier,
            onToggleColumnsPanel = onToggleTableColumnsPanel,
            onToggleColumn = onToggleTableColumn,
            onReset = onResetTableDisplay,
            onToggleAutoNext = onToggleAutoNext,
        )
        Spacer(Modifier.height(10.dp))
        if (rows.isEmpty()) {
            EmptyState("Пока нет строк. Введите Hebrew text и нажмите `Собрать таблицу`.")
        } else {
            GeneratedRowsTable(
                state = state,
                onPlayRow = onPlayRow,
                onOpenRowNote = onOpenRowNote,
                onSelectRow = onSelectRow,
                onAdjustColumnWidth = onAdjustTableColumnWidth,
            )
        }
    }
}

@Composable
private fun LibraryLoadedTableMetadataHeader(
    title: String?,
    sourceLabel: String?,
    ttsProfileLabel: String?,
    translationLabel: String?,
    audioStatusLabel: String?,
) {
    val cleanTitle = title?.trim().orEmpty()
    val cleanSource = sourceLabel?.trim().orEmpty()
    val cleanTts = ttsProfileLabel?.trim().orEmpty()
    val cleanTranslation = translationLabel?.trim().orEmpty()
    val cleanAudio = audioStatusLabel?.trim().orEmpty()
    if (cleanTitle.isBlank() && cleanSource.isBlank() && cleanTts.isBlank() && cleanTranslation.isBlank() && cleanAudio.isBlank()) return

    Surface(
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cleanTitle.isNotBlank()) {
                Text(
                    cleanTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SourceValueRow(
                label = "SOURCE:",
                source = cleanSource,
                showEmptyValue = false,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cleanAudio.isNotBlank()) StatusPill(cleanAudio)
                if (cleanTts.isNotBlank()) StatusPill(cleanTts)
                if (cleanTranslation.isNotBlank()) StatusPill(cleanTranslation)
            }
            if (cleanTts.isNotBlank()) {
                Text(
                    "Настройки озвучки экрана синхронизированы с профилем карточки. Импортированное локальное аудио воспроизводится без повторного синтеза; пересинтез нужен только после явного изменения настроек.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableDisplayScenarioPanel(
    state: ClassicModeUiState,
    modifier: Modifier = Modifier,
    onToggleColumnsPanel: () -> Unit,
    onToggleColumn: (ClassicTableColumn) -> Unit,
    onReset: () -> Unit,
    onToggleAutoNext: () -> Unit,
) {
    Surface(
        color = SoftPanelBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🧩 Таблица: отображение и сценарии", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton(
                    text = if (state.tableDisplay.isColumnsPanelOpen) "Скрыть колонки" else "Колонки",
                    onClick = onToggleColumnsPanel,
                )
                PrimaryBlueButton(
                    text = if (state.tableDisplay.isAutoNextActive) "■ Остановить" else "▶▶ Построчное воспроизведение",
                    onClick = onToggleAutoNext,
                    enabled = state.rows.isNotEmpty(),
                )
            }
            if (state.tableDisplay.isColumnsPanelOpen) {
                HorizontalDivider()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClassicTableColumn.entries.forEach { column ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(end = 10.dp),
                        ) {
                            Checkbox(
                                checked = column in state.tableDisplay.visibleColumns,
                                onCheckedChange = { onToggleColumn(column) },
                            )
                            Text(column.columnControlLabel())
                        }
                    }
                }
                SecondaryActionButton("Сбросить настройки", onReset)
                Text("Настройки таблицы сохраняются в текущем экране. Потяните правый край заголовка колонки, чтобы изменить ширину.", color = MutedText)
            }
        }
    }
}

@Composable
private fun GeneratedRowsTable(
    state: ClassicModeUiState,
    onPlayRow: (Int) -> Unit,
    onOpenRowNote: (Int) -> Unit,
    onSelectRow: (Int) -> Unit,
    onAdjustColumnWidth: (ClassicTableColumn, Float) -> Unit,
) {
    val visibleColumns = ClassicTableColumn.entries.filter { it in state.tableDisplay.visibleColumns }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
    ) {
        Row {
            visibleColumns.forEachIndexed { index, column ->
                ClassicTableHeaderCell(
                    text = column.columnHeaderLabel(state),
                    widthDp = state.tableDisplay.columnWidthsDp[column] ?: column.defaultWidthDp(),
                    canResize = true,
                    onResize = { dragPx ->
                        onAdjustColumnWidth(column, with(density) { dragPx.toDp().value })
                    },
                )
            }
        }
        state.rows.forEach { row ->
            val selected = state.tableDisplay.selectedRowIndex == row.orderIndex
            val playing = state.playingRowIndex == row.orderIndex
            Row(
                modifier = Modifier
                    .clickable { onSelectRow(row.orderIndex) }
                    .background(
                        when {
                            playing -> Color(0xFFE3F2FD)
                            selected -> Color(0xFFFFF3B0)
                            else -> Color.White
                        },
                    ),
            ) {
                visibleColumns.forEach { column ->
                    ClassicTableCell(
                        column = column,
                        row = row,
                        state = state,
                        isPlaying = playing,
                        onPlayRow = { onPlayRow(row.orderIndex) },
                        onOpenRowNote = { onOpenRowNote(row.orderIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassicTableHeaderCell(
    text: String,
    widthDp: Int,
    canResize: Boolean,
    onResize: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(58.dp)
            .background(Color(0xFFF1F4F7))
            .border(1.dp, BorderColor)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (canResize) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(14.dp)
                    .pointerInput(text) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            onResize(dragAmount)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("⋮", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClassicTableCell(
    column: ClassicTableColumn,
    row: ClassicGeneratedRowUi,
    state: ClassicModeUiState,
    isPlaying: Boolean,
    onPlayRow: () -> Unit,
    onOpenRowNote: () -> Unit,
) {
    val widthDp = state.tableDisplay.columnWidthsDp[column] ?: column.defaultWidthDp()
    val isRtlText = column == ClassicTableColumn.Hebrew || column == ClassicTableColumn.Niqqud
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(124.dp)
            .border(1.dp, BorderColor)
            .padding(8.dp),
    ) {
        when (column) {
            ClassicTableColumn.Action -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
                ) {
                    Text("${row.orderIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    AudioCacheBadge(row.audioAssetKey)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecondaryActionButton(
                            text = if (isPlaying) "▶..." else "▶",
                            onClick = onPlayRow,
                            modifier = Modifier.width(48.dp),
                        )
                        SecondaryActionButton(
                            text = if (row.note.isNullOrBlank()) "✎" else "✎✓",
                            onClick = onOpenRowNote,
                            modifier = Modifier.width(52.dp),
                        )
                    }
                }
            }
            else -> {
                val cellText = row.tableCellText(column, state)
                SelectionContainer {
                    Text(
                        text = cellText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (column == ClassicTableColumn.Hebrew) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (cellText.isBlank()) MutedText else Color(0xFF1F2933),
                        textAlign = if (isRtlText) TextAlign.End else TextAlign.Start,
                        modifier = Modifier.fillMaxSize(),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun ClassicTableColumn.columnControlLabel(): String =
    when (this) {
        ClassicTableColumn.Action -> "Действие"
        ClassicTableColumn.Hebrew -> "Иврит"
        ClassicTableColumn.Niqqud -> "Огласовки"
        ClassicTableColumn.Translit -> "Транслит"
        ClassicTableColumn.Translation -> "Перевод"
    }

private fun ClassicTableColumn.columnHeaderLabel(state: ClassicModeUiState): String =
    when (this) {
        ClassicTableColumn.Action -> "▶✎"
        ClassicTableColumn.Hebrew -> "Иврит"
        ClassicTableColumn.Niqqud -> "Огласовки"
        ClassicTableColumn.Translit -> when (state.translitProfile) {
            ClassicTranslitProfile.Sbl -> "Транслит (SBL)"
            ClassicTranslitProfile.RuPhonetic -> "Транслит (рус.)"
        }
        ClassicTableColumn.Translation -> "Перевод"
    }

private fun ClassicTableColumn.defaultWidthDp(): Int =
    when (this) {
        ClassicTableColumn.Action -> 92
        ClassicTableColumn.Hebrew -> 170
        ClassicTableColumn.Niqqud -> 190
        ClassicTableColumn.Translit -> 190
        ClassicTableColumn.Translation -> 230
    }

private fun ClassicGeneratedRowUi.tableCellText(column: ClassicTableColumn, state: ClassicModeUiState): String =
    when (column) {
        ClassicTableColumn.Action -> ""
        ClassicTableColumn.Hebrew -> hebrewPlain
        ClassicTableColumn.Niqqud -> hebrewNiqqud.ifBlank { "—" }
        ClassicTableColumn.Translit -> when (state.translitProfile) {
            ClassicTranslitProfile.Sbl -> translit.ifBlank {
                transliterateHebrewWithProfile(hebrewNiqqud, ClassicTranslitProfile.Sbl)
            }
            ClassicTranslitProfile.RuPhonetic -> translitRu.ifBlank {
                transliterateHebrewWithProfile(hebrewNiqqud, ClassicTranslitProfile.RuPhonetic)
            }.ifBlank { translit }
        }.ifBlank { "—" }
        ClassicTableColumn.Translation -> russian.ifBlank { "—" }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratedRowCard(
    row: ClassicGeneratedRowUi,
    onPlayRow: () -> Unit,
    onOpenRowNote: () -> Unit,
    isPlaying: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftPanelBackground),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Строка ${row.orderIndex + 1}", style = MaterialTheme.typography.labelLarge, color = MutedText)
                    Text("Действие", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioCacheBadge(row.audioAssetKey)
                    SecondaryActionButton(if (isPlaying) "▶..." else "▶", onPlayRow, modifier = Modifier.widthIn(min = 48.dp))
                    SecondaryActionButton(if (row.note.isNullOrBlank()) "📝" else "📝 ✓", onOpenRowNote, modifier = Modifier.widthIn(min = 48.dp))
                }
            }
            Text(row.hebrewPlain, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
            if (row.hebrewNiqqud.isNotBlank()) {
                Text(row.hebrewNiqqud, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Niqqud: пока недоступно", color = MutedText)
            }
            if (row.translit.isNotBlank()) Text("SBL: ${row.translit}")
            if (row.translitRu.isNotBlank()) Text("Russian phonetic: ${row.translitRu}")
            if (row.russian.isNotBlank()) Text("Russian: ${row.russian}")
            if (!row.note.isNullOrBlank()) {
                Text("Заметка: ${row.note.lineSequence().firstOrNull().orEmpty().take(120)}", style = MaterialTheme.typography.bodySmall, color = ClassicBlue)
            }
        }
    }
}

@Composable
private fun AudioCacheBadge(assetKey: String?) {
    val (label, color) = if (assetKey.isNullOrBlank()) {
        "○" to Color(0xFFADB5BD)
    } else {
        "●" to ClassicGreen
    }
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Text(
            label,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun ClassicSaveMetadataDialog(
    draft: ClassicSaveMetadataDraft,
    isSaving: Boolean,
    onDraftChanged: (ClassicSaveMetadataDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onCancel()
        },
        title = { Text("Метаданные текста", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { onDraftChanged(draft.copy(title = it)) },
                    label = { Text("TITLE*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = draft.level,
                    onValueChange = { onDraftChanged(draft.copy(level = it)) },
                    label = { Text("LEVEL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = draft.tagsCsv,
                    onValueChange = { onDraftChanged(draft.copy(tagsCsv = it)) },
                    label = { Text("TAGS") },
                    supportingText = { Text("Tags вводите через запятую. Пустое поле = очистка значения.") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = draft.source,
                    onValueChange = { onDraftChanged(draft.copy(source = it)) },
                    label = { Text("SOURCE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = draft.topic,
                    onValueChange = { onDraftChanged(draft.copy(topic = it)) },
                    label = { Text("TEMA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                )
            }
        },
        confirmButton = {
            PrimaryGreenButton(if (isSaving) "Save..." else "Save", onSave, enabled = !isSaving && draft.title.isNotBlank())
        },
        dismissButton = {
            SecondaryActionButton("Cancel", onCancel, enabled = !isSaving)
        },
    )
}

@Composable
private fun RowNoteDialog(
    editor: ClassicNoteEditorState,
    onDraftChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!editor.isSaving) onClose()
        },
        title = { Text("Заметка к строке", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = SoftPanelBackground,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderColor),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(editor.row.hebrewPlain, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                        if (editor.row.russian.isNotBlank()) Text(editor.row.russian, color = MutedText)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoteTemplateButton("B", "**", editor.draft, onDraftChanged)
                    NoteTemplateButton("I", "*", editor.draft, onDraftChanged)
                    NoteAppendButton("• List", "\n- ", editor.draft, onDraftChanged)
                    NoteAppendButton("> Quote", "\n> ", editor.draft, onDraftChanged)
                }
                OutlinedTextField(
                    value = editor.draft,
                    onValueChange = onDraftChanged,
                    label = { Text("Note") },
                    placeholder = { Text("Напишите заметку к этой строке. Поддерживаются Markdown-паттерны.") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !editor.isSaving,
                )
                Text(
                    "Пустая заметка не хранится: очистите поле и нажмите Save или используйте Delete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText,
                )
            }
        },
        confirmButton = {
            PrimaryGreenButton(if (editor.isSaving) "Save..." else "Save", onSave, enabled = !editor.isSaving)
        },
        dismissButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton("Delete", onDelete, enabled = !editor.isSaving && !editor.row.note.isNullOrBlank())
                SecondaryActionButton("Close", onClose, enabled = !editor.isSaving)
            }
        },
    )
}

@Composable
private fun NoteTemplateButton(label: String, marker: String, draft: String, onDraftChanged: (String) -> Unit) {
    SecondaryActionButton(label, onClick = { onDraftChanged(draft + marker + "text" + marker) })
}

@Composable
private fun NoteAppendButton(label: String, value: String, draft: String, onDraftChanged: (String) -> Unit) {
    SecondaryActionButton(label, onClick = { onDraftChanged(draft + value) })
}

@Composable
private fun LocalLibrarySummaryCard(
    summaries: List<LibraryTextSummary>,
    savedTextId: String?,
    onOpenLibrary: () -> Unit,
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Локальная библиотека", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Сохранённые тексты находятся на устройстве и экспортируются отдельно от ключей.", color = MutedText)
            }
            SecondaryActionButton("📚 Открыть", onOpenLibrary)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Текстов: ${summaries.size}")
            if (savedTextId != null) StatusPill("Текущий текст сохранён")
        }
        summaries.take(3).forEach { summary ->
            Text("${summary.title} · ${formatDate(summary.updatedAt)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryV3Modal(
    state: LibraryUiState,
    classicState: ClassicModeUiState,
    onClose: () -> Unit,
    onSaveCurrent: () -> Unit,
    onOpenInClassic: (String, Boolean) -> Unit,
    onArchiveText: (String, Boolean) -> Unit,
    onRequestDeleteText: (String) -> Unit,
    onConfirmDeleteText: (String) -> Unit,
    onCancelDeleteText: () -> Unit,
    onStartMetadataEdit: (String) -> Unit,
    onMetadataDraftChanged: (LibraryTextMetadataDraft) -> Unit,
    onSaveMetadata: () -> Unit,
    onCancelMetadataEdit: () -> Unit,
    onImportLibraryJson: (String) -> Unit,
    onImportZipBundle: (java.io.InputStream) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var tagsMode by rememberSaveable { mutableStateOf(LibraryTagsMode.ALL) }
    var searchScope by rememberSaveable { mutableStateOf(LibrarySearchScope.TEXTS) }
    var sortMode by rememberSaveable { mutableStateOf(LibrarySortMode.LAST_OPENED) }
    var selectedTags by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var localNotice by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val importReadScope = rememberCoroutineScope()
    val importJsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importReadScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            .orEmpty()
                    }
                }
                result.fold(
                    onSuccess = { text ->
                        if (text.isBlank()) {
                            localNotice = "Выбранный JSON пуст."
                        } else {
                            onImportLibraryJson(text)
                        }
                    },
                    onFailure = { error ->
                        localNotice = "Не удалось прочитать JSON: ${error.message.orEmpty()}"
                    },
                )
            }
        }
    }

    val importZipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importReadScope.launch {
                runCatching {
                    val stream = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                    }
                    if (stream != null) {
                        onImportZipBundle(stream)
                    } else {
                        localNotice = "Не удалось открыть ZIP файл."
                    }
                }.onFailure { error ->
                    localNotice = "Ошибка ZIP импорта: ${error.message.orEmpty()}"
                }
            }
        }
    }

    val tagCounts = remember(state.summaries) { buildTagCounts(state.summaries) }
    val filtered = remember(state.summaries, query, level, selectedTags, tagsMode, searchScope, sortMode) {
        filterAndSortSummaries(
            summaries = state.summaries,
            query = query,
            level = level,
            selectedTags = selectedTags,
            tagsMode = tagsMode,
            searchScope = searchScope,
            sortMode = sortMode,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(10.dp)
            .imePadding(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Библиотека (v3)",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryActionButton("Экспорт Библиотеки", onClick = { localNotice = "ZIP export UI будет подключён в M6/M7." })
                    SecondaryActionButton(
                        "Импорт JSON",
                        onClick = { importJsonPicker.launch(arrayOf("application/json", "text/*")) },
                    )
                    SecondaryActionButton(
                        "Импорт ZIP (с аудио)",
                        onClick = { importZipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    )
                    SecondaryActionButton("Обновить", onClick = { localNotice = "Список синхронизирован с локальной Room Flow." })
                    SecondaryActionButton("Закрыть", onClick = onClose)
                }
                HorizontalDivider(color = BorderColor)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text("Поиск (PRO): название / тема / ссылка / уровень / теги (#tag)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            )
                            LibrarySelector(
                                label = level.ifBlank { "Все уровни" },
                                options = libraryLevelOptions(state.summaries),
                                selected = level,
                                onSelected = { level = it },
                            )
                            LibrarySelector(
                                label = tagsMode.label,
                                options = LibraryTagsMode.entries.map { it.label to it },
                                selected = tagsMode,
                                onSelected = { tagsMode = it },
                            )
                            LibrarySelector(
                                label = searchScope.label,
                                options = LibrarySearchScope.entries.map { it.label to it },
                                selected = searchScope,
                                onSelected = { searchScope = it },
                            )
                            LibrarySelector(
                                label = sortMode.label,
                                options = LibrarySortMode.entries.map { it.label to it },
                                selected = sortMode,
                                onSelected = { sortMode = it },
                            )
                            PrimaryGreenButton(
                                text = if (classicState.isSaving) "Сохранение..." else "Сохранить текущую таблицу",
                                onClick = onSaveCurrent,
                                enabled = classicState.rows.isNotEmpty() && !classicState.isSaving && !classicState.isGenerating,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TagCloud(
                                tagCounts = tagCounts,
                                selectedTags = selectedTags,
                                onToggleTag = { tag ->
                                    selectedTags = if (tag in selectedTags) {
                                        selectedTags - tag
                                    } else {
                                        selectedTags + tag
                                    }
                                },
                            )
                            SecondaryActionButton(
                                "Сбросить",
                                onClick = {
                                    query = ""
                                    level = ""
                                    tagsMode = LibraryTagsMode.ALL
                                    searchScope = LibrarySearchScope.TEXTS
                                    sortMode = LibrarySortMode.LAST_OPENED
                                    selectedTags = emptyList()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    item {
                        state.message?.let { MessageCard(message = it, onDismiss = onDismissMessage) }
                        localNotice?.let {
                            MessageCard(message = it, onDismiss = { localNotice = null })
                        }
                        if (searchScope != LibrarySearchScope.TEXTS) {
                            MessageCard(
                                message = "${searchScope.label}: repository query for rows/notes is pending; visible text metadata is still filtered.",
                                onDismiss = { searchScope = LibrarySearchScope.TEXTS },
                            )
                        }
                    }

                    item {
                        Text(
                            "Загружено: ${filtered.size} · ${libraryLoadedTimestamp(state.summaries)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText,
                        )
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyState("Пока нет сохранённых текстов или фильтр ничего не нашёл.")
                        }
                    } else {
                        items(filtered, key = { it.textId }) { summary ->
                            LibraryV3TextCard(
                                summary = summary,
                                position = filtered.indexOf(summary) + 1,
                                selectedTags = selectedTags,
                                onOpen = { onOpenInClassic(summary.textId, false) },
                                onContinue = { onOpenInClassic(summary.textId, true) },
                                onEdit = { onStartMetadataEdit(summary.textId) },
                                onArchive = { onArchiveText(summary.textId, true) },
                                onDelete = { onRequestDeleteText(summary.textId) },
                            )
                        }
                    }
                }
            }
        }

        state.pendingDeleteTextId?.let { textId ->
            val title = state.summaries.firstOrNull { it.textId == textId }?.title ?: "selected text"
            DeleteTextDialog(
                title = title,
                onConfirm = { onConfirmDeleteText(textId) },
                onCancel = onCancelDeleteText,
            )
        }

        state.metadataDraft?.let { draft ->
            MetadataEditorDialog(
                draft = draft,
                isSaving = state.isLoading,
                onDraftChanged = onMetadataDraftChanged,
                onSave = onSaveMetadata,
                onCancel = onCancelMetadataEdit,
            )
        }
    }
}

@Composable
private fun TagCloud(
    tagCounts: List<Pair<String, Int>>,
    selectedTags: List<String>,
    onToggleTag: (String) -> Unit,
) {
    if (tagCounts.isEmpty()) {
        Text("Теги пока отсутствуют", style = MaterialTheme.typography.bodySmall, color = MutedText)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tagCounts.take(18).forEach { (tag, count) ->
            val selected = tag in selectedTags
            FilterChip(
                selected = selected,
                onClick = { onToggleTag(tag) },
                label = { Text("#$tag $count") },
            )
        }
    }
}

@Composable
private fun SourceValueRow(
    label: String,
    source: String,
    showEmptyValue: Boolean = true,
) {
    val cleanSource = source.trim()
    if (cleanSource.isBlank() && !showEmptyValue) return

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }
    val canOpen = cleanSource.isHttpUrl()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText)
        if (cleanSource.isBlank()) {
            Text("—", style = MaterialTheme.typography.bodySmall, color = MutedText, modifier = Modifier.weight(1f))
        } else {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    cleanSource,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (canOpen) ClassicBlue else Color(0xFF1F2D3A),
                        textDecoration = if (canOpen) TextDecoration.Underline else TextDecoration.None,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = canOpen) {
                        runCatching { uriHandler.openUri(cleanSource) }
                            .onFailure {
                                Toast.makeText(context, "Не удалось открыть источник.", Toast.LENGTH_SHORT).show()
                            }
                    },
                )
            }
            TextButton(
                onClick = {
                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("SOURCE", cleanSource))
                    Toast.makeText(context, "Источник скопирован.", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("⧉")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryV3TextCard(
    summary: LibraryTextSummary,
    position: Int,
    selectedTags: List<String>,
    onOpen: () -> Unit,
    onContinue: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (summary.isArchived) Color(0xFFF5F5F5) else SoftPanelBackground),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$position.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MutedText)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            summary.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        summary.topic?.let { TopicBadge(it) }
                    }
                    Text("Прогресс: строка № —", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    SourceValueRow(label = "Источник:", source = summary.sourceLabel.orEmpty())
                    Text(
                        "Последнее открытие: ${formatDate(summary.lastOpenedAt)} · Создан: ${formatDate(summary.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                summary.level?.let { StatusPill("Уровень: $it") }
                StatusPill(summary.libraryAudioStatusLabel())
                summary.tags.take(8).forEach { tag ->
                    val selected = tag in selectedTags
                    Box(
                        Modifier
                            .border(1.dp, if (selected) ClassicBlue else BorderColor, RoundedCornerShape(99.dp))
                            .background(if (selected) Color(0xFFE8F4FD) else PanelBackground, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("#$tag", style = MaterialTheme.typography.bodySmall, color = ClassicBlue)
                    }
                }
                if (summary.isArchived) StatusPill("Архив")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryBlueButton("Открыть", onOpen, modifier = Modifier.widthIn(min = 128.dp))
                PrimaryGreenButton("Продолжить", onContinue, modifier = Modifier.widthIn(min = 128.dp))
                SecondaryActionButton("Изменить", onEdit, modifier = Modifier.widthIn(min = 128.dp))
                SecondaryActionButton("В архив", onArchive, enabled = !summary.isArchived, modifier = Modifier.widthIn(min = 128.dp))
                DangerActionButton("Удалить", onDelete, modifier = Modifier.widthIn(min = 128.dp))
            }
        }
    }
}

@Composable
private fun TopicBadge(topic: String) {
    Box(
        Modifier
            .background(Color(0xFFEAF7EF), RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(topic, style = MaterialTheme.typography.labelSmall, color = Color(0xFF207245))
    }
}

@Composable
private fun DeleteTextDialog(
    title: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Удалить текст?") },
        text = { Text("`$title` будет удалён вместе со строками. Shared audio assets are not removed outside their Room cascade rules.") },
        confirmButton = {
            DangerActionButton("Удалить", onConfirm)
        },
        dismissButton = {
            SecondaryActionButton("Cancel", onCancel)
        },
    )
}

@Composable
private fun MetadataEditorDialog(
    draft: LibraryTextMetadataDraft,
    isSaving: Boolean,
    onDraftChanged: (LibraryTextMetadataDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Метаданные текста", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                TextButton(onClick = onCancel) { Text("Закрыть") }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                draft.validationMessage?.let { Text(it, color = ClassicDanger, fontWeight = FontWeight.Bold) }
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { onDraftChanged(draft.copy(title = it)) },
                    label = { Text("TITLE*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                LibrarySelector(
                    label = draft.level.ifBlank { "—" },
                    options = listOf("—" to "") + BaseLevelOptions.filter { it.isNotBlank() }.map { it to it },
                    selected = draft.level,
                    onSelected = { onDraftChanged(draft.copy(level = it)) },
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = draft.tagsCsv,
                    onValueChange = { onDraftChanged(draft.copy(tagsCsv = it)) },
                    label = { Text("TAGS") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    value = draft.source,
                    onValueChange = { onDraftChanged(draft.copy(source = it)) },
                    label = { Text("SOURCE") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    value = draft.topic,
                    onValueChange = { onDraftChanged(draft.copy(topic = it)) },
                    label = { Text("TEMA") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Text("Tags вводите через запятую. Пустое поле = очистка значения.", color = MutedText)
            }
        },
        confirmButton = {
            PrimaryBlueButton(if (isSaving) "Saving..." else "Save", onSave, enabled = !isSaving)
        },
        dismissButton = {
            SecondaryActionButton("Cancel", onCancel, enabled = !isSaving)
        },
    )
}

@Composable
private fun <T> LibrarySelector(
    label: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, BorderColor),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = PanelBackground),
        ) {
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Start, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("⌄")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 260.dp, max = 360.dp),
        ) {
            options.forEach { (title, value) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (value == selected) "✓" else "", modifier = Modifier.width(24.dp))
                            Text(title)
                        }
                    },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDraftChanged: (ProviderCredentialId, String) -> Unit,
    onSaveCredential: (ProviderCredentialId) -> Unit,
    onAttachJsonCredential: (ProviderCredentialId, String) -> Unit,
    onValidateCredential: (ProviderCredentialId) -> Unit,
    onDeleteCredential: (ProviderCredentialId) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SurfaceCard {
            Text("Provider credentials", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Target UX: Прикрепить JSON -> Проверить -> Store securely. Current input below remains an interim smoke-check path and must not be exported or logged.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText,
            )
        }

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        state.credentialStatuses.forEach { status ->
            ProviderCredentialCard(
                status = status,
                draft = state.drafts[status.id].orEmpty(),
                onDraftChanged = { onDraftChanged(status.id, it) },
                onSave = { onSaveCredential(status.id) },
                onAttachJson = { onAttachJsonCredential(status.id, it) },
                onValidate = { onValidateCredential(status.id) },
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
    onAttachJson: (String) -> Unit,
    onValidate: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
            }.getOrElse { "" }
            onAttachJson(text)
        }
    }
    val isGemini = status.id == ProviderCredentialId.GeminiLegacy

    SurfaceCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(status.id.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StatusPill(if (status.isConfigured) "valid/local" else "not configured")
        }
        Text(status.id.usage, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Text("Provider ID: ${status.id.wireId}", style = MaterialTheme.typography.bodySmall)
        Text("Stored value: ${status.maskedValue ?: "Not configured"}", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isGemini) {
                SecondaryActionButton("Прикрепить JSON", onClick = { jsonPicker.launch(arrayOf("application/json", "text/*")) })
            }
            SecondaryActionButton("Проверить", onClick = onValidate, enabled = status.isConfigured)
            SecondaryActionButton("Удалить ключ", onDelete, enabled = status.isConfigured)
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            label = { Text(if (isGemini) "Gemini API Key" else "Interim single-line credential for smoke checks") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(16.dp),
        )
        PrimaryBlueButton(if (isGemini) "Сохранить API Key" else "Save credential", onSave, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun IdeModeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SurfaceCard {
            StatusPill("Experimental / under development")
            Spacer(Modifier.height(8.dp))
            Text("IDE Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("This mode remains separate from Classic Mode. It must not own Library v3 navigation.")
        }
    }
}

@Composable
fun DeveloperContactScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(ClipboardManager::class.java)
    }
    val supportInfo = remember { buildSupportInfoText() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SurfaceCard {
            Text("Связь с разработчиком", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Основной канал поддержки — WhatsApp. Соцсети доступны как вторичные ссылки и не используются для отправки данных приложения.",
                color = MutedText,
            )
            PrimaryGreenButton(
                text = "Написать в WhatsApp",
                onClick = {
                    runCatching { uriHandler.openUri(DeveloperWhatsappUrl) }
                        .onFailure {
                            Toast.makeText(context, "Не удалось открыть WhatsApp или браузер.", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryActionButton(
                text = "Скопировать номер $DeveloperPhoneDisplay",
                onClick = {
                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("WhatsApp", DeveloperPhoneDisplay))
                    Toast.makeText(context, "Номер скопирован.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SurfaceCard {
            Text("Социальные ссылки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SourceValueRow(label = "Facebook:", source = DeveloperFacebookUrl)
            SourceValueRow(label = "Instagram:", source = DeveloperInstagramUrl)
        }

        SurfaceCard {
            Text("Сведения для поддержки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Эти сведения не содержат ключи, тексты, библиотеку или аудио. Они помогают описать среду при обращении.",
                color = MutedText,
            )
            SelectionContainer {
                Text(supportInfo, style = MaterialTheme.typography.bodySmall)
            }
            SecondaryActionButton(
                text = "Скопировать сведения",
                onClick = {
                    clipboardManager?.setPrimaryClip(ClipData.newPlainText("App support info", supportInfo))
                    Toast.makeText(context, "Сведения скопированы.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SurfaceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBackground),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun DisclosureSurfaceCard(
    title: String,
    subtitle: String,
    isOpen: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText)
            }
            SecondaryActionButton(if (isOpen) "Скрыть" else "Показать", onToggle)
        }
        if (isOpen) {
            content()
        }
    }
}

@Composable
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionContainer {
                Text(message)
            }
            SecondaryActionButton("Dismiss", onDismiss)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftPanelBackground), shape = RoundedCornerShape(18.dp)) {
        Text(message, modifier = Modifier.padding(14.dp), color = MutedText)
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        Modifier
            .background(Color(0xFFEFF3F6), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color(0xFF3D4A54))
    }
}

@Composable
private fun PrimaryGreenButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ClassicGreen, contentColor = Color.White),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryBlueButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ClassicBlue, contentColor = Color.White),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = NeutralButton),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DangerActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ClassicDanger),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFEEF2), contentColor = ClassicDanger),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private enum class LibraryTagsMode(val label: String) {
    ALL("Теги: ALL"),
    ANY("Теги: ANY"),
}

private enum class LibrarySearchScope(val label: String) {
    TEXTS("Поиск: Тексты"),
    TEXTS_NOTES_ROWS("Поиск: Тексты + заметки + строки"),
    ROWS_ONLY("Поиск: Только строки"),
    NOTES_ONLY("Поиск: Только заметки"),
}

private enum class LibrarySortMode(val label: String) {
    LAST_OPENED("Сорт: Последние открытые"),
    LAST_MODIFIED("Сорт: Последние изменённые"),
    TITLE_ASC("Сорт: Название A→Я"),
    LEVEL_ASC("Сорт: Уровень A→Я"),
    THEME_ASC("Сорт: Тема A→Я"),
}

private val BaseLevelOptions = listOf("", "alef", "alef+", "bet", "bet+", "gimel", "gimel+", "dalet", "dalet+", "he", "he+", "vav")

private fun libraryLevelOptions(summaries: List<LibraryTextSummary>): List<Pair<String, String>> {
    val dynamic = summaries.mapNotNull { it.level }.filter { it.isNotBlank() }
    val values = (BaseLevelOptions + dynamic).distinct()
    return values.map { if (it.isBlank()) "Все уровни" to "" else it to it }
}

private fun buildTagCounts(summaries: List<LibraryTextSummary>): List<Pair<String, Int>> =
    summaries
        .flatMap { it.tags }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase(Locale.getDefault()) })

private fun filterAndSortSummaries(
    summaries: List<LibraryTextSummary>,
    query: String,
    level: String,
    selectedTags: List<String>,
    tagsMode: LibraryTagsMode,
    searchScope: LibrarySearchScope,
    sortMode: LibrarySortMode,
): List<LibraryTextSummary> {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault()).removePrefix("#")
    val filtered = summaries.filter { summary ->
        val matchesLevel = level.isBlank() || summary.level == level
        val tagSet = summary.tags.toSet()
        val matchesSelectedTags = selectedTags.isEmpty() || when (tagsMode) {
            LibraryTagsMode.ALL -> selectedTags.all { it in tagSet }
            LibraryTagsMode.ANY -> selectedTags.any { it in tagSet }
        }
        val haystack = when (searchScope) {
            LibrarySearchScope.TEXTS -> listOf(summary.title, summary.topic.orEmpty(), summary.sourceLabel.orEmpty(), summary.level.orEmpty()) + summary.tags
            LibrarySearchScope.TEXTS_NOTES_ROWS,
            LibrarySearchScope.ROWS_ONLY,
            LibrarySearchScope.NOTES_ONLY -> listOf(summary.title, summary.topic.orEmpty(), summary.sourceLabel.orEmpty(), summary.level.orEmpty()) + summary.tags
        }.joinToString(" ").lowercase(Locale.getDefault())
        val matchesQuery = normalizedQuery.isBlank() || haystack.contains(normalizedQuery)
        matchesLevel && matchesSelectedTags && matchesQuery
    }
    return when (sortMode) {
        LibrarySortMode.LAST_OPENED -> filtered.sortedWith(compareByDescending<LibraryTextSummary> { it.lastOpenedAt ?: it.updatedAt }.thenBy { it.title })
        LibrarySortMode.LAST_MODIFIED -> filtered.sortedWith(compareByDescending<LibraryTextSummary> { it.updatedAt }.thenBy { it.title })
        LibrarySortMode.TITLE_ASC -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        LibrarySortMode.LEVEL_ASC -> filtered.sortedWith(compareBy<LibraryTextSummary> { levelRank(it.level) }.thenBy { it.title })
        LibrarySortMode.THEME_ASC -> filtered.sortedWith(compareBy<LibraryTextSummary> { it.topic.orEmpty().lowercase(Locale.getDefault()) }.thenBy { it.title })
    }
}

private fun levelRank(level: String?): Int {
    val index = BaseLevelOptions.indexOf(level.orEmpty())
    return if (index >= 0) index else Int.MAX_VALUE
}

private fun formatDate(value: String?): String =
    value?.replace("T", " ")
        ?.removeSuffix("Z")
        ?.substringBefore(".")
        ?: "—"

private fun LibraryTextSummary.libraryAudioStatusLabel(): String =
    when {
        hasTextAudio && rowCount == 0 -> "Аудио: полный текст локально"
        rowCount > 0 && linkedAudioCount >= rowCount -> "Аудио: локально $linkedAudioCount/$rowCount"
        rowCount > 0 && linkedAudioCount > 0 -> "Аудио: частично $linkedAudioCount/$rowCount"
        hasTextAudio -> "Аудио: текст локально"
        else -> "Аудио: нет"
    }

private fun String.isHttpUrl(): Boolean =
    runCatching {
        val uri = Uri.parse(trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

private fun buildSupportInfoText(): String =
    listOf(
        "App: ${BuildConfig.APPLICATION_ID}",
        "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        "Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}",
        "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
    ).joinToString(separator = "\n")

private fun libraryLoadedTimestamp(summaries: List<LibraryTextSummary>): String =
    summaries
        .flatMap { listOfNotNull(it.lastOpenedAt, it.updatedAt) }
        .maxOrNull()
        ?.let(::formatDate)
        ?: "—"
