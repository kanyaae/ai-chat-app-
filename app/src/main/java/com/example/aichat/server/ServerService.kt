package com.example.aichat.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.aichat.MainActivity
import com.example.aichat.R
import com.example.aichat.data.SettingsManager
import com.example.aichat.network.ApiClient
import com.example.aichat.network.ChatMessageFull
import com.example.aichat.network.ChatRequest
import com.example.aichat.system.SystemStatsManager
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ServerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var ktorServer: NettyApplicationEngine? = null
    private val gson = Gson()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        
        startKtorServer()
        startTelegramBot()
        
        return START_STICKY
    }

    private fun startKtorServer() {
        if (ktorServer != null) return
        
        val settingsManager = SettingsManager(applicationContext)
        val statsManager = SystemStatsManager(applicationContext)

        ktorServer = embeddedServer(Netty, port = 8080) {
            install(CORS) {
                anyHost()
            }

            routing {
                get("/") {
                    // Serve a basic Web App UI (Single Page App)
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>OpenClaw AI Server Hub</title>
                            <style>
                                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #121212; color: #fff; margin: 0; padding: 20px; }
                                .container { max-width: 800px; margin: auto; background: #1e1e1e; padding: 20px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }
                                h1 { color: #bb86fc; }
                                .chat-box { height: 400px; overflow-y: scroll; border: 1px solid #333; padding: 10px; margin-bottom: 10px; border-radius: 8px; background: #2d2d2d; }
                                .msg { margin-bottom: 10px; padding: 10px; border-radius: 8px; }
                                .msg.user { background: #bb86fc; color: #000; margin-left: 20%; }
                                .msg.ai { background: #333; margin-right: 20%; }
                                input[type=text] { width: calc(100% - 100px); padding: 10px; border-radius: 8px; border: none; background: #333; color: white; }
                                button { width: 80px; padding: 10px; border-radius: 8px; border: none; background: #03dac6; color: #000; font-weight: bold; cursor: pointer; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h1>🤖 OpenClaw Web Interface</h1>
                                <div id="status">Loading status...</div>
                                <hr/>
                                <div class="chat-box" id="chatbox"></div>
                                <div>
                                    <input type="text" id="userInput" placeholder="Type a message..." onkeypress="if(event.key === 'Enter') sendMsg()"/>
                                    <button onclick="sendMsg()">Send</button>
                                </div>
                            </div>
                            <script>
                                async function updateStatus() {
                                    try {
                                        let res = await fetch('/api/status');
                                        let data = await res.json();
                                        document.getElementById('status').innerText = `Battery: ${'$'}{data.batteryPercentage}% | RAM: ${'$'}{data.availableRamMb}MB free | CPU: ${'$'}{data.cpuUsagePercent.toFixed(1)}%`;
                                    } catch(e) {}
                                }
                                setInterval(updateStatus, 5000);
                                updateStatus();

                                async function sendMsg() {
                                    let input = document.getElementById('userInput');
                                    let text = input.value;
                                    if(!text) return;
                                    input.value = '';
                                    
                                    let box = document.getElementById('chatbox');
                                    box.innerHTML += `<div class="msg user">${'$'}{text}</div>`;
                                    box.scrollTop = box.scrollHeight;
                                    
                                    try {
                                        let res = await fetch('/api/chat', {
                                            method: 'POST',
                                            headers: {'Content-Type': 'application/json'},
                                            body: JSON.stringify({ message: text })
                                        });
                                        let data = await res.json();
                                        box.innerHTML += `<div class="msg ai">${'$'}{data.reply}</div>`;
                                        box.scrollTop = box.scrollHeight;
                                    } catch(e) {
                                        box.innerHTML += `<div class="msg ai" style="color:red">Error: ${'$'}{e.message}</div>`;
                                    }
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    call.respondText(html, ContentType.Text.Html)
                }

                get("/api/status") {
                    val stats = statsManager.getStats()
                    call.respondText(gson.toJson(stats), ContentType.Application.Json)
                }

                post("/api/chat") {
                    val reqText = call.receiveText()
                    val map = gson.fromJson(reqText, Map::class.java)
                    val userMessage = map["message"].toString()

                    val apiKey = settingsManager.apiKeyFlow.first()
                    val endpointUrl = settingsManager.endpointUrlFlow.first()
                    val modelName = settingsManager.modelNameFlow.first()
                    val systemPrompt = settingsManager.systemPromptFlow.first()
                    val temperature = settingsManager.temperatureFlow.first()

                    try {
                        val api = ApiClient.getApi(endpointUrl)
                        val messages = listOf(
                            ChatMessageFull("system", systemPrompt),
                            ChatMessageFull("user", userMessage)
                        )
                        val request = ChatRequest(modelName, messages, temperature)
                        val response = api.createChatCompletion("Bearer $apiKey", request)
                        
                        val reply = response.choices?.firstOrNull()?.message?.content ?: "No response"
                        
                        val resMap = mapOf("reply" to reply)
                        call.respondText(gson.toJson(resMap), ContentType.Application.Json)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
                    }
                }
                
                // Stub for MCP (Model Context Protocol) 
                get("/mcp") {
                    val mcpData = mapOf("version" to "1.0", "supported" to listOf("prompts", "tools", "resources"))
                    call.respondText(gson.toJson(mcpData), ContentType.Application.Json)
                }
            }
        }.start(wait = false)
    }

    private fun startTelegramBot() {
        serviceScope.launch {
            val settingsManager = SettingsManager(applicationContext)
            // Telegram polling loop
            var offset = 0L
            val client = OkHttpClient()
            
            while (true) {
                // In a real scenario, the user provides a Telegram Bot Token in Settings
                // For now, this is the architectural loop ready for the token.
                delay(3000)
                // val token = settingsManager.telegramTokenFlow.first()
                // if (token.isBlank()) continue
                // ... fetch updates ...
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ktorServer?.stop(1000, 2000)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "server_channel",
            "AI Server",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        return NotificationCompat.Builder(this, "server_channel")
            .setContentTitle("OpenClaw Server Hub is running")
            .setContentText("Listening on port 8080")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
