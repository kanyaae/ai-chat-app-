package com.example.aichat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val category: String = "general",  // general, preference, fact, instruction
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = ""  // which model created this memory
)

data class ConversationSummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val modelUsed: String,
    val messageCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)

class MemoryManager(private val context: Context) {
    private val gson = Gson()
    private val memoryFile = File(context.filesDir, "agent_memory.json")
    private val historyFile = File(context.filesDir, "conversation_history.json")

    // ===== Long-term Memory =====

    fun getMemories(): List<MemoryEntry> {
        if (!memoryFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<MemoryEntry>>() {}.type
            gson.fromJson(memoryFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addMemory(content: String, category: String = "general", source: String = "") {
        val memories = getMemories().toMutableList()
        memories.add(MemoryEntry(content = content, category = category, source = source))
        // Keep max 100 memories
        val trimmed = if (memories.size > 100) memories.takeLast(100) else memories
        memoryFile.writeText(gson.toJson(trimmed))
    }

    fun deleteMemory(id: String) {
        val memories = getMemories().filter { it.id != id }
        memoryFile.writeText(gson.toJson(memories))
    }

    fun clearMemory() {
        if (memoryFile.exists()) memoryFile.writeText("[]")
        if (historyFile.exists()) historyFile.writeText("[]")
    }

    fun getMemoryContext(): String {
        val memories = getMemories()
        if (memories.isEmpty()) return ""

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder("\n\n[LONG-TERM MEMORY - Things you remember about the user:]\n")
        for (m in memories.takeLast(30)) {
            sb.appendLine("- [${m.category}] ${m.content} (${sdf.format(Date(m.createdAt))})")
        }
        return sb.toString()
    }

    // ===== Conversation History =====

    fun getConversationHistory(): List<ConversationSummary> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ConversationSummary>>() {}.type
            gson.fromJson(historyFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveConversationSummary(title: String, summary: String, modelUsed: String, messageCount: Int) {
        val history = getConversationHistory().toMutableList()
        history.add(ConversationSummary(
            title = title,
            summary = summary,
            modelUsed = modelUsed,
            messageCount = messageCount
        ))
        // Keep max 50 conversations
        val trimmed = if (history.size > 50) history.takeLast(50) else history
        historyFile.writeText(gson.toJson(trimmed))
    }

    fun getRecentConversationContext(limit: Int = 5): String {
        val history = getConversationHistory()
        if (history.isEmpty()) return ""

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder("\n\n[PAST CONVERSATIONS - Recent chat summaries:]\n")
        for (conv in history.takeLast(limit)) {
            sb.appendLine("- ${sdf.format(Date(conv.createdAt))} [${conv.modelUsed}]: ${conv.title} - ${conv.summary}")
        }
        return sb.toString()
    }
}
