package com.sindromradiospb.ttsprototypev2.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sindromradiospb.ttsprototypev2.core.model.TtsProviderId
import com.sindromradiospb.ttsprototypev2.core.model.TranslationProviderId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialId
import com.sindromradiospb.ttsprototypev2.core.settings.ProviderCredentialStatus
import com.sindromradiospb.ttsprototypev2.data.repository.LibraryTextSummary
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicGeneratedRowUi
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeUiState
import com.sindromradiospb.ttsprototypev2.feature.classic.ClassicModeViewModel
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryTextMetadataDraft
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryUiState
import com.sindromradiospb.ttsprototypev2.feature.library.LibraryViewModel
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsUiState
import com.sindromradiospb.ttsprototypev2.feature.settings.SettingsViewModel
import java.util.Locale

private val AppBackground = Color(0xFFF4F6F8)
private val PanelBackground = Color(0xFFFFFFFF)
private val SoftPanelBackground = Color(0xFFF8FAFB)
private val NeutralButton = Color(0xFFF1F3F5)
private val BorderColor = Color(0xFFE0E5EA)
private val ClassicBlue = Color(0xFF3498DB)
private val ClassicGreen = Color(0xFF2ECC71)
private val ClassicDanger = Color(0xFFE85D75)
private val MutedText = Color(0xFF68737D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    classicModeViewModel: ClassicModeViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isLibraryOpen by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf("Classic", "Settings", "IDE")
    val classicState by classicModeViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()

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
                    onClearSource = { classicModeViewModel.onSourceTextChanged("") },
                    onTranslationProviderChanged = classicModeViewModel::onTranslationProviderChanged,
                    onTtsProviderChanged = classicModeViewModel::onTtsProviderChanged,
                    onGenerate = classicModeViewModel::generateTable,
                    onSave = classicModeViewModel::saveCurrent,
                    onSpeak = classicModeViewModel::speakSource,
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
                    onDeleteCredential = settingsViewModel::deleteCredential,
                    onDismissMessage = settingsViewModel::clearMessage,
                    modifier = Modifier.padding(innerPadding),
                )

                else -> IdeModeScreen(Modifier.padding(innerPadding))
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
                onDismissMessage = libraryViewModel::clearMessage,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassicModeScreen(
    state: ClassicModeUiState,
    onSourceTextChanged: (String) -> Unit,
    onClearSource: () -> Unit,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
    onGenerate: () -> Unit,
    onSave: () -> Unit,
    onSpeak: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIde: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ClassicStatusStrip(state)

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
            onSourceTextChanged = onSourceTextChanged,
            onClearSource = onClearSource,
            onGenerate = onGenerate,
            onSpeak = onSpeak,
        )

        ProviderSettingsCard(
            state = state,
            onTranslationProviderChanged = onTranslationProviderChanged,
            onTtsProviderChanged = onTtsProviderChanged,
        )

        state.message?.let {
            MessageCard(message = it, onDismiss = onDismissMessage)
        }

        ClassicResultCard(
            rows = state.rows,
            generatedAt = state.generatedAt,
            generationLabel = state.generationLabel,
            onSave = onSave,
            isSaveEnabled = state.rows.isNotEmpty() && !state.isSaving && !state.isGenerating,
            isSaving = state.isSaving,
        )

        LocalLibrarySummaryCard(
            summaries = state.libraryTexts,
            savedTextId = state.savedTextId,
            onOpenLibrary = onOpenLibrary,
        )
    }
}

@Composable
private fun ClassicStatusStrip(state: ClassicModeUiState) {
    SurfaceCard {
        Text("Лимиты и квоты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
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
    onSourceTextChanged: (String) -> Unit,
    onClearSource: () -> Unit,
    onGenerate: () -> Unit,
    onSpeak: () -> Unit,
) {
    SurfaceCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Исходный текст", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
private fun ProviderSettingsCard(
    state: ClassicModeUiState,
    onTranslationProviderChanged: (TranslationProviderId) -> Unit,
    onTtsProviderChanged: (TtsProviderId) -> Unit,
) {
    SurfaceCard {
        Text("Настройки перевода и озвучки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Выбор провайдеров явный. Скрытого fallback для quota/key/billing ошибок нет.", color = MutedText)
        Spacer(Modifier.height(10.dp))
        Text("Перевод", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TranslationProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = state.translationProvider == provider,
                    onClick = { onTranslationProviderChanged(provider) },
                    label = { Text(provider.wireId) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("TTS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TtsProviderId.entries.forEach { provider ->
                FilterChip(
                    selected = state.ttsProvider == provider,
                    onClick = { onTtsProviderChanged(provider) },
                    label = { Text(provider.wireId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassicResultCard(
    rows: List<ClassicGeneratedRowUi>,
    generatedAt: String?,
    generationLabel: String,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
) {
    SurfaceCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(generationLabel)
            StatusPill("Audio: ${if (rows.isEmpty()) "missing" else "pending"}")
            if (generatedAt != null) StatusPill("Generated: ${formatDate(generatedAt)}")
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Результат", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Таблица адаптирована в карточки строк для телефона.", color = MutedText)
            }
            PrimaryGreenButton(
                text = if (isSaving) "Сохранение..." else "💾 Сохранить",
                onClick = onSave,
                enabled = isSaveEnabled,
            )
        }
        Spacer(Modifier.height(10.dp))
        if (rows.isEmpty()) {
            EmptyState("Пока нет строк. Введите Hebrew text и нажмите `Собрать таблицу`.")
        } else {
            rows.forEach { row ->
                GeneratedRowCard(row)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GeneratedRowCard(row: ClassicGeneratedRowUi) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftPanelBackground),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Строка ${row.orderIndex + 1}", style = MaterialTheme.typography.labelLarge, color = MutedText)
            Text(row.hebrewPlain, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
            if (row.hebrewNiqqud.isNotBlank()) {
                Text(row.hebrewNiqqud, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Niqqud: пока недоступно", color = MutedText)
            }
            if (row.translit.isNotBlank()) Text("SBL: ${row.translit}")
            if (row.translitRu.isNotBlank()) Text("Russian phonetic: ${row.translitRu}")
            if (row.russian.isNotBlank()) Text("Russian: ${row.russian}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton("▶ Озвучить строку", onClick = {}, enabled = false)
                SecondaryActionButton("Изменить", onClick = {}, enabled = false)
            }
        }
    }
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
    onDismissMessage: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf("") }
    var tagsMode by rememberSaveable { mutableStateOf(LibraryTagsMode.ALL) }
    var searchScope by rememberSaveable { mutableStateOf(LibrarySearchScope.TEXTS) }
    var sortMode by rememberSaveable { mutableStateOf(LibrarySortMode.LAST_OPENED) }
    var selectedTags by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var localNotice by rememberSaveable { mutableStateOf<String?>(null) }

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
                    SecondaryActionButton("Импорт Библиотеки", onClick = { localNotice = "Import пока не реализован; silent fail запрещён." })
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
                                onCopySource = {
                                    localNotice = "Copy source action требует ClipboardManager wiring в следующем UI-hardening patch."
                                },
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
    onCopySource: () -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Источник: ", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Text(
                            summary.sourceLabel ?: "—",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onCopySource, enabled = !summary.sourceLabel.isNullOrBlank()) {
                            Text("⧉")
                        }
                    }
                    Text(
                        "Последнее открытие: ${formatDate(summary.lastOpenedAt)} · Создан: ${formatDate(summary.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                summary.level?.let { StatusPill("Уровень: $it") }
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
    SurfaceCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(status.id.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StatusPill(if (status.isConfigured) "valid/local" else "not configured")
        }
        Text(status.id.usage, style = MaterialTheme.typography.bodySmall, color = MutedText)
        Text("Provider ID: ${status.id.wireId}", style = MaterialTheme.typography.bodySmall)
        Text("Stored value: ${status.maskedValue ?: "Not configured"}", style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton("Прикрепить JSON", onClick = {}, enabled = false)
            SecondaryActionButton("Проверить", onClick = {}, enabled = false)
            SecondaryActionButton("Удалить ключ", onDelete, enabled = status.isConfigured)
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            label = { Text("Interim single-line credential for smoke checks") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(16.dp),
        )
        PrimaryBlueButton("Save credential", onSave, modifier = Modifier.fillMaxWidth())
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
private fun MessageCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message)
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

private fun libraryLoadedTimestamp(summaries: List<LibraryTextSummary>): String =
    summaries
        .flatMap { listOfNotNull(it.lastOpenedAt, it.updatedAt) }
        .maxOrNull()
        ?.let(::formatDate)
        ?: "—"
