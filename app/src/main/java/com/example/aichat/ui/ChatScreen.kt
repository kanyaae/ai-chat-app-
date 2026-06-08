package com.example.aichat.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aichat.R
import com.example.aichat.agent.AgentSkills
import com.example.aichat.agent.SkillExecutor
import com.example.aichat.data.BuiltInProviders
import com.example.aichat.data.MemoryManager
import com.example.aichat.data.SettingsManager
import com.example.aichat.network.*
import com.example.aichat.theme.*
import kotlinx.coroutines.launch
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.util.Base64

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val memoryManager = remember { MemoryManager(context) }
    val skillExecutor = remember { SkillExecutor(context) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val apiKey by settingsManager.apiKeyFlow.collectAsState(initial = "")
    val endpointUrl by settingsManager.endpointUrlFlow.collectAsState(initial = "https://api.openai.com/v1/")
    val modelName by settingsManager.modelNameFlow.collectAsState(initial = "gpt-4o-mini")
    val systemPrompt by settingsManager.systemPromptFlow.collectAsState(initial = "You are a helpful assistant.")
    val temperature by settingsManager.temperatureFlow.collectAsState(initial = 0.7f)
    val providerId by settingsManager.providerIdFlow.collectAsState(initial = "openai")
    val fontSizePref by settingsManager.fontSizeFlow.collectAsState(initial = "medium")
    val memoryEnabled by settingsManager.memoryEnabledFlow.collectAsState(initial = true)
    val backgroundUri by settingsManager.backgroundUriFlow.collectAsState(initial = "")

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val t = TextToSpeech(context) {}
        tts = t
        onDispose { t.shutdown() }
    }

    var attachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        attachmentUri = uri
    }
    
    val sttLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!data.isNullOrEmpty()) {
                input = data[0]
            }
        }
    }

    // Display messages (user + assistant text)
    var displayMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    // Full API conversation history (includes tool calls/results)
    var apiMessages by remember { mutableStateOf(listOf<ChatMessageFull>()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var agentStatus by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val providerName = BuiltInProviders.findById(providerId)?.name ?: providerId

    val noResponseStr = stringResource(R.string.no_response)
    val errorStr = stringResource(R.string.error)
    val usingToolsStr = stringResource(R.string.using_tools)
    val runningToolStr = stringResource(R.string.running_tool)
    val processingResultsStr = stringResource(R.string.processing_results)
    val loopLimitStr = stringResource(R.string.loop_limit_reached)

    // Auto-scroll
    LaunchedEffect(displayMessages.size, isLoading) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1 + if (isLoading) 1 else 0)
        }
    }

    // Build system prompt with memory
    fun buildSystemPrompt(): String {
        val sb = StringBuilder(systemPrompt)
        sb.append("\n\nYou are an AI agent running on the user's Android device. You have access to tools/skills that let you interact with the device's file system, apps, and more.")
        
        if (memoryEnabled) {
            sb.append("\nWhen you learn something important about the user (name, preferences, facts), tell them you'll remember it.")
            sb.append("\nIMPORTANT: Always use the 'save_memory' approach by mentioning [REMEMBER: ...] in your response when you learn new facts about the user.")
            sb.append(memoryManager.getMemoryContext())
            sb.append(memoryManager.getRecentConversationContext())
        }
        return sb.toString()
    }

    // Send message and handle tool calling loop
    fun sendMessage() {
        if (input.isBlank() && attachmentUri == null || isLoading) return
        val userText = input
        input = ""
        isLoading = true
        agentStatus = ""

        val b64 = attachmentUri?.let { uri ->
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) { null }
        }

        val finalContent: Any = if (b64 != null) {
            listOf(
                ContentPart(type = "text", text = userText),
                ContentPart(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,$b64"))
            )
        } else {
            userText
        }

        val userMsg = ChatMessage("user", userText, attachmentUri?.toString())
        displayMessages = displayMessages + userMsg

        val userMsgFull = ChatMessageFull("user", finalContent)
        apiMessages = apiMessages + userMsgFull
        
        attachmentUri = null // clear after send

        scope.launch {
            try {
                val api = ApiClient.getApi(endpointUrl)
                val tools = AgentSkills.getAllTools()

                // Build full messages with system prompt
                val fullMessages = mutableListOf<ChatMessageFull>()
                fullMessages.add(ChatMessageFull("system", buildSystemPrompt()))
                fullMessages.addAll(apiMessages)

                var continueLoop = true
                var loopCount = 0
                val maxLoops = 5

                while (continueLoop && loopCount < maxLoops) {
                    loopCount++
                    val request = ChatRequest(
                        model = modelName,
                        messages = fullMessages,
                        temperature = temperature,
                        tools = tools
                    )

                    val response = api.createChatCompletion("Bearer $apiKey", request)
                    val choice = response.choices?.firstOrNull()
                    val respMsg = choice?.message

                    if (respMsg == null) {
                        displayMessages = displayMessages + ChatMessage("assistant", noResponseStr)
                        continueLoop = false
                        continue
                    }

                    // Check if AI wants to call tools
                    if (respMsg.toolCalls != null && respMsg.toolCalls.isNotEmpty()) {
                        fullMessages.add(ChatMessageFull(
                            role = "assistant",
                            content = respMsg.content,
                            toolCalls = respMsg.toolCalls
                        ))
                        apiMessages = apiMessages + ChatMessageFull(
                            role = "assistant",
                            content = respMsg.content,
                            toolCalls = respMsg.toolCalls
                        )

                        val skillNames = respMsg.toolCalls.map { it.function.name }
                        agentStatus = usingToolsStr.format(skillNames.joinToString(", "))

                        if (!respMsg.content.isNullOrBlank()) {
                            displayMessages = displayMessages + ChatMessage("assistant", respMsg.content)
                        }

                        for (tc in respMsg.toolCalls) {
                            agentStatus = runningToolStr.format(tc.function.name)
                            val result = skillExecutor.execute(tc)

                            val toolResultMsg = ChatMessageFull(
                                role = "tool",
                                content = result,
                                toolCallId = tc.id
                            )
                            fullMessages.add(toolResultMsg)
                            apiMessages = apiMessages + toolResultMsg
                        }

                        agentStatus = processingResultsStr
                        continueLoop = true
                    } else {
                        val text = respMsg.content ?: "..."
                        displayMessages = displayMessages + ChatMessage("assistant", text)
                        apiMessages = apiMessages + ChatMessageFull("assistant", text)

                        if (memoryEnabled) {
                            val memoryPattern = Regex("\\[REMEMBER:\\s*(.+?)\\]", RegexOption.IGNORE_CASE)
                            memoryPattern.findAll(text).forEach { match ->
                                memoryManager.addMemory(
                                    content = match.groupValues[1],
                                    category = "fact",
                                    source = modelName
                                )
                            }
                        }

                        continueLoop = false
                        agentStatus = ""
                    }
                }

                if (loopCount >= maxLoops) {
                    displayMessages = displayMessages + ChatMessage("assistant", loopLimitStr)
                }

            } catch (e: Exception) {
                displayMessages = displayMessages + ChatMessage("assistant", errorStr.format(e.message))
            } finally {
                isLoading = false
                agentStatus = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "$providerName • $modelName",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (displayMessages.isNotEmpty()) {
                        IconButton(onClick = {
                            if (memoryEnabled && displayMessages.size >= 2) {
                                val title = displayMessages.first().content.take(50)
                                val lastMsg = displayMessages.last().content.take(100)
                                memoryManager.saveConversationSummary(
                                    title = title,
                                    summary = lastMsg,
                                    modelUsed = modelName,
                                    messageCount = displayMessages.size
                                )
                            }
                            displayMessages = emptyList()
                            apiMessages = emptyList()
                        }) {
                            Icon(Icons.Default.DeleteSweep, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.tertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (backgroundUri.isNotBlank()) {
                AsyncImage(
                    model = backgroundUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            Column(modifier = Modifier.fillMaxSize()) {
                val copiedText = stringResource(R.string.copied)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (displayMessages.isEmpty()) {
                    item { EmptyStateView() }
                }
                items(displayMessages) { msg ->
                    ChatBubble(
                        message = msg,
                        fontSizePref = fontSizePref,
                        onLongPress = {
                            clipboardManager.setText(AnnotatedString(msg.content))
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        },
                        onSpeak = {
                            tts?.speak(msg.content, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
                }
                if (isLoading) {
                    item { TypingIndicator(agentStatus) }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

            if (attachmentUri != null) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(80.dp).clip(RoundedCornerShape(8.dp))) {
                    AsyncImage(model = attachmentUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    IconButton(onClick = { attachmentUri = null }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = { imagePicker.launch("image/*") }) {
                    Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.type_a_message), color = MaterialTheme.colorScheme.outline) },
                    minLines = 1, maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.tertiary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                IconButton(onClick = { 
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                    sttLauncher.launch(intent)
                }) {
                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(4.dp))
                FloatingActionButton(
                    onClick = { sendMessage() },
                    containerColor = if ((input.isNotBlank() || attachmentUri != null) && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.ai_agent_ready), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.agent_description), fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(20.dp))

        val skills = listOf(
            stringResource(R.string.skill_files),
            stringResource(R.string.skill_apps),
            stringResource(R.string.skill_search),
            stringResource(R.string.skill_notes),
            stringResource(R.string.skill_memory)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            skills.forEach { skill ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(skill, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage, fontSizePref: String, onLongPress: () -> Unit, onSpeak: () -> Unit = {}) {
    val isUser = message.role == "user"

    val textSize = when (fontSizePref) {
        "small" -> 13.sp
        "large" -> 18.sp
        else -> 15.sp
    }
    val lineHeight = when (fontSizePref) {
        "small" -> 18.sp
        "large" -> 26.sp
        else -> 22.sp
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp, bottomEnd = 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.widthIn(max = 300.dp).combinedClickable(onClick = {}, onLongClick = onLongPress)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.attachmentUri != null) {
                        AsyncImage(
                            model = message.attachmentUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).padding(bottom = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = message.content,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = textSize, lineHeight = lineHeight
                    )
                    if (!isUser) {
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp).align(Alignment.End).padding(top = 4.dp)) {
                            Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                        }
                    }
                }
            }
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(status: String = "") {
    Column(modifier = Modifier.padding(start = 40.dp, top = 4.dp)) {
        if (status.isNotBlank()) {
            Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 4.dp))
        }
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = when (index) { 0 -> 1f; 1 -> 0.6f; else -> 0.3f }))
                    )
                }
            }
        }
    }
}
