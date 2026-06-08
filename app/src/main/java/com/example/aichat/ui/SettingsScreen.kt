package com.example.aichat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aichat.R
import com.example.aichat.data.AiProvider
import com.example.aichat.data.BuiltInProviders
import com.example.aichat.data.SettingsManager
import com.example.aichat.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    val savedApiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
    val savedProviderId by settingsManager.providerIdFlow.collectAsState(initial = "openai")
    val savedModelName by settingsManager.modelNameFlow.collectAsState(initial = "gpt-4o-mini")
    val savedEndpointUrl by settingsManager.endpointUrlFlow.collectAsState(initial = "https://api.openai.com/v1/")
    val savedSystemPrompt by settingsManager.systemPromptFlow.collectAsState(initial = "You are a helpful assistant.")
    val savedTemperature by settingsManager.temperatureFlow.collectAsState(initial = 0.7f)
    val savedThemeMode by settingsManager.themeModeFlow.collectAsState(initial = "system")
    val savedColorPalette by settingsManager.colorPaletteFlow.collectAsState(initial = "neon")
    val savedFontSize by settingsManager.fontSizeFlow.collectAsState(initial = "medium")
    val savedMemoryEnabled by settingsManager.memoryEnabledFlow.collectAsState(initial = true)
    val savedLanguage by settingsManager.languageFlow.collectAsState(initial = "system")
    val savedCustomColor by settingsManager.customColorFlow.collectAsState(initial = "")
    val savedBackgroundUri by settingsManager.backgroundUriFlow.collectAsState(initial = "")
    val savedTelegramToken by settingsManager.telegramTokenFlow.collectAsState(initial = "")
    val customProviders by settingsManager.customProvidersFlow.collectAsState(initial = emptyList())

    var apiKey by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var selectedProviderId by remember(savedProviderId) { mutableStateOf(savedProviderId) }
    var selectedModel by remember(savedModelName) { mutableStateOf(savedModelName) }
    var endpointUrl by remember(savedEndpointUrl) { mutableStateOf(savedEndpointUrl) }
    var systemPrompt by remember(savedSystemPrompt) { mutableStateOf(savedSystemPrompt) }
    var temperature by remember(savedTemperature) { mutableStateOf(savedTemperature) }
    var themeMode by remember(savedThemeMode) { mutableStateOf(savedThemeMode) }
    var colorPalette by remember(savedColorPalette) { mutableStateOf(savedColorPalette) }
    var fontSize by remember(savedFontSize) { mutableStateOf(savedFontSize) }
    var memoryEnabled by remember(savedMemoryEnabled) { mutableStateOf(savedMemoryEnabled) }
    var language by remember(savedLanguage) { mutableStateOf(savedLanguage) }
    var customColor by remember(savedCustomColor) { mutableStateOf(savedCustomColor) }
    var backgroundUri by remember(savedBackgroundUri) { mutableStateOf(savedBackgroundUri) }
    var telegramToken by remember(savedTelegramToken) { mutableStateOf(savedTelegramToken) }
    var showAddProviderDialog by remember { mutableStateOf(false) }

    val allProviders = BuiltInProviders.providers + customProviders
    val selectedProvider = allProviders.find { it.id == selectedProviderId } ?: BuiltInProviders.providers[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Provider Selection
            item {
                SectionTitle(icon = Icons.Default.Cloud, title = stringResource(R.string.ai_provider))
            }
            items(allProviders) { provider ->
                ProviderCard(
                    provider = provider,
                    isSelected = provider.id == selectedProviderId,
                    onClick = {
                        selectedProviderId = provider.id
                        endpointUrl = provider.baseUrl
                        if (provider.models.isNotEmpty()) {
                            selectedModel = provider.models[0]
                        }
                    },
                    onGetApiKey = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.apiKeyUrl))
                        context.startActivity(intent)
                    }
                )
            }
            item {
                OutlinedButton(
                    onClick = { showAddProviderDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.5f)))
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_custom_provider))
                }
            }

            // Model Selection
            item {
                SectionTitle(icon = Icons.Default.Psychology, title = stringResource(R.string.model))
            }
            item {
                ModelSelector(
                    models = selectedProvider.models,
                    selectedModel = selectedModel,
                    onModelSelected = { selectedModel = it },
                    endpointUrl = endpointUrl,
                    onEndpointUrlChange = { endpointUrl = it }
                )
            }

            // API Key
            item {
                SectionTitle(icon = Icons.Default.Key, title = stringResource(R.string.api_key))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text(stringResource(R.string.api_key)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                cursorColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedProvider.apiKeyUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.get_api_key, selectedProvider.name), color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // System Prompt
            item {
                SectionTitle(icon = Icons.Default.SmartToy, title = stringResource(R.string.system_prompt))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text(stringResource(R.string.system_prompt)) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            // Temperature
            item {
                SectionTitle(icon = Icons.Default.Thermostat, title = stringResource(R.string.temperature, temperature))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0f..2f,
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.precise), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.creative), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Appearance Settings
            item {
                SectionTitle(icon = Icons.Default.Palette, title = stringResource(R.string.appearance))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Language
                        Text(stringResource(R.string.language), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to stringResource(R.string.system_default), "th" to "ไทย", "en" to "English").forEach { (value, label) ->
                                FilterChip(
                                    selected = language == value,
                                    onClick = { language = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("es" to "Español", "ja" to "日本語", "zh" to "中文").forEach { (value, label) ->
                                FilterChip(
                                    selected = language == value,
                                    onClick = { language = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        
                        Text(stringResource(R.string.theme_mode), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to stringResource(R.string.auto), "dark" to stringResource(R.string.dark), "light" to stringResource(R.string.light)).forEach { (value, label) ->
                                FilterChip(
                                    selected = themeMode == value,
                                    onClick = { themeMode = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.color_palette), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("neon" to stringResource(R.string.neon), "ocean" to stringResource(R.string.ocean), "forest" to stringResource(R.string.forest), "sunset" to stringResource(R.string.sunset)).forEach { (value, label) ->
                                FilterChip(
                                    selected = colorPalette == value,
                                    onClick = { colorPalette = value; customColor = "" },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customColor,
                            onValueChange = { customColor = it },
                            placeholder = { Text("Custom Hex (e.g. #FF0000)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(16.dp))
                        Text("Chat Background", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        
                        val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            uri?.let { backgroundUri = it.toString() }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { imagePicker.launch("image/*") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Pick Background")
                            }
                            if (backgroundUri.isNotBlank()) {
                                TextButton(onClick = { backgroundUri = "" }) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.chat_font_size), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("small" to stringResource(R.string.small), "medium" to stringResource(R.string.medium), "large" to stringResource(R.string.large)).forEach { (value, label) ->
                                FilterChip(
                                    selected = fontSize == value,
                                    onClick = { fontSize = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Advanced Memory Settings
            item {
                SectionTitle(icon = Icons.Default.Memory, title = stringResource(R.string.agent_memory))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.long_term_memory), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(R.string.allow_ai_remember), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = memoryEnabled,
                                onCheckedChange = { memoryEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primaryContainer)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        val memoryClearedMsg = stringResource(R.string.memory_cleared)
                        OutlinedButton(
                            onClick = {
                                val memoryManager = com.example.aichat.data.MemoryManager(context)
                                scope.launch {
                                    memoryManager.clearMemory()
                                    android.widget.Toast.makeText(context, memoryClearedMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.clear_all_memories))
                        }
                    }
                }
            }

            // Server & Telegram Mode
            item {
                SectionTitle(icon = Icons.Default.Dns, title = "Server & Telegram Mode")
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Telegram Bot Token", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = telegramToken,
                            onValueChange = { telegramToken = it },
                            placeholder = { Text("123456789:ABCdefGHI...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                cursorColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Web Server (Port 8080)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Starts Ktor Web Server & Telegram Bot", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        
                        var isServerRunning by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, com.example.aichat.server.ServerService::class.java)
                                if (!isServerRunning) {
                                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                                    isServerRunning = true
                                    android.widget.Toast.makeText(context, "Server Started on 8080", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    context.stopService(intent)
                                    isServerRunning = false
                                    android.widget.Toast.makeText(context, "Server Stopped", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isServerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isServerRunning) "Stop Server" else "Start Server Hub")
                        }
                    }
                }
            }
            
            // Contact & Support
            item {
                SectionTitle(icon = Icons.Default.HelpCenter, title = stringResource(R.string.contact_support))
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val telegramUrl = "https://t.me/benzsirirat"
                        
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.donate))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.contact_developer))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.report_bug))
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        scope.launch {
                            settingsManager.saveProvider(selectedProviderId, endpointUrl, selectedModel)
                            settingsManager.saveApiKey(apiKey)
                            settingsManager.saveSystemPrompt(systemPrompt)
                            settingsManager.saveTemperature(temperature)
                            settingsManager.saveAppearanceSettings(themeMode, colorPalette, fontSize, backgroundUri, customColor)
                            settingsManager.saveMemoryEnabled(memoryEnabled)
                            settingsManager.saveLanguage(language)
                            settingsManager.saveTelegramToken(telegramToken)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.save_settings), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showAddProviderDialog) {
        AddCustomProviderDialog(
            onDismiss = { showAddProviderDialog = false },
            onAdd = { provider ->
                scope.launch {
                    settingsManager.saveCustomProviders(customProviders + provider)
                }
                showAddProviderDialog = false
            }
        )
    }
}

@Composable
fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun ProviderCard(
    provider: AiProvider,
    isSelected: Boolean,
    onClick: () -> Unit,
    onGetApiKey: () -> Unit
) {
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "border")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (provider.id) {
                            "openai" -> OpenAIGreen
                            "anthropic" -> AnthropicOrange
                            "google" -> GoogleBlue
                            "openrouter" -> OpenRouterCyan
                            "groq" -> GroqPurple
                            "deepseek" -> DeepSeekBlue
                            "mistral" -> MistralOrange
                            "ollama" -> OllamaWhite
                            else -> NeonPurple
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    provider.name.first().toString(),
                    color = if (provider.id == "ollama") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${provider.models.size} models",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    models: List<String>,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    endpointUrl: String,
    onEndpointUrlChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = endpointUrl,
                onValueChange = onEndpointUrlChange,
                label = { Text(stringResource(R.string.endpoint_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.tertiary
                )
            )
            Spacer(Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { onModelSelected(it) },
                    label = { Text(stringResource(R.string.model)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    readOnly = false,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        cursorColor = MaterialTheme.colorScheme.tertiary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            },
                            leadingIcon = {
                                if (model == selectedModel) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomProviderDialog(
    onDismiss: () -> Unit,
    onAdd: (AiProvider) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKeyUrl by remember { mutableStateOf("") }
    var modelsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(stringResource(R.string.add_custom_provider), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.provider_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.base_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = apiKeyUrl,
                    onValueChange = { apiKeyUrl = it },
                    label = { Text(stringResource(R.string.api_key_url)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = modelsText,
                    onValueChange = { modelsText = it },
                    label = { Text(stringResource(R.string.models_comma)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank()) {
                        onAdd(
                            AiProvider(
                                id = "custom_${System.currentTimeMillis()}",
                                name = name,
                                baseUrl = baseUrl,
                                apiKeyUrl = apiKeyUrl.ifBlank { "" },
                                models = modelsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                isCustom = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
