package com.example.aichat.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ===== Function Calling Models =====

data class ToolFunction(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("parameters") val parameters: Map<String, Any>
)

data class Tool(
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: ToolFunction
)

data class ToolCall(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String = "function",
    @SerializedName("function") val function: ToolCallFunction
)

data class ToolCallFunction(
    @SerializedName("name") val name: String,
    @SerializedName("arguments") val arguments: String
)

// ===== Skill Definitions =====

object AgentSkills {

    fun getAllTools(): List<Tool> = listOf(
        // File System Skills
        Tool(function = ToolFunction(
            name = "list_files",
            description = "List files and folders in a directory on the device. Use this to browse the file system.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "Directory path to list. Use '/' for root, '/sdcard/' for internal storage.")
                ),
                "required" to listOf("path")
            )
        )),
        Tool(function = ToolFunction(
            name = "read_file",
            description = "Read the text content of a file on the device.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "Full path to the file to read.")
                ),
                "required" to listOf("path")
            )
        )),
        Tool(function = ToolFunction(
            name = "write_file",
            description = "Write or create a text file on the device storage.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "Full path for the file to write."),
                    "content" to mapOf("type" to "string", "description" to "The text content to write.")
                ),
                "required" to listOf("path", "content")
            )
        )),
        Tool(function = ToolFunction(
            name = "delete_file",
            description = "Delete a file or empty folder on the device.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "Full path to the file or folder to delete.")
                ),
                "required" to listOf("path")
            )
        )),
        Tool(function = ToolFunction(
            name = "search_files",
            description = "Search for files by name pattern in a directory (recursive).",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "directory" to mapOf("type" to "string", "description" to "Directory to search in."),
                    "pattern" to mapOf("type" to "string", "description" to "File name pattern to search for (e.g. '.pdf', 'report').")
                ),
                "required" to listOf("directory", "pattern")
            )
        )),

        // Device Info Skills
        Tool(function = ToolFunction(
            name = "device_info",
            description = "Get device information: model, Android version, storage space, battery level.",
            parameters = mapOf("type" to "object", "properties" to emptyMap<String, Any>())
        )),

        // App Management Skills
        Tool(function = ToolFunction(
            name = "list_apps",
            description = "List installed apps on the device.",
            parameters = mapOf("type" to "object", "properties" to emptyMap<String, Any>())
        )),
        Tool(function = ToolFunction(
            name = "open_app",
            description = "Open an installed app by package name.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "package_name" to mapOf("type" to "string", "description" to "The package name of the app to open (e.g. 'com.google.android.youtube').")
                ),
                "required" to listOf("package_name")
            )
        )),
        Tool(function = ToolFunction(
            name = "open_url",
            description = "Open a URL in the default browser.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "url" to mapOf("type" to "string", "description" to "The URL to open.")
                ),
                "required" to listOf("url")
            )
        )),

        // Utility Skills
        Tool(function = ToolFunction(
            name = "create_note",
            description = "Create a text note/memo and save it to storage.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "title" to mapOf("type" to "string", "description" to "Title/filename for the note."),
                    "content" to mapOf("type" to "string", "description" to "Content of the note.")
                ),
                "required" to listOf("title", "content")
            )
        )),
        Tool(function = ToolFunction(
            name = "get_current_time",
            description = "Get the current date and time.",
            parameters = mapOf("type" to "object", "properties" to emptyMap<String, Any>())
        ))
    )
}

// ===== Skill Executor =====

class SkillExecutor(private val context: Context) {
    private val gson = Gson()

    fun execute(toolCall: ToolCall): String {
        return try {
            val args = if (toolCall.function.arguments.isNotBlank()) {
                gson.fromJson(toolCall.function.arguments, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } else emptyMap()

            when (toolCall.function.name) {
                "list_files" -> listFiles(args["path"] as? String ?: "/sdcard/")
                "read_file" -> readFile(args["path"] as? String ?: "")
                "write_file" -> writeFile(args["path"] as? String ?: "", args["content"] as? String ?: "")
                "delete_file" -> deleteFile(args["path"] as? String ?: "")
                "search_files" -> searchFiles(args["directory"] as? String ?: "/sdcard/", args["pattern"] as? String ?: "")
                "device_info" -> getDeviceInfo()
                "list_apps" -> listApps()
                "open_app" -> openApp(args["package_name"] as? String ?: "")
                "open_url" -> openUrl(args["url"] as? String ?: "")
                "create_note" -> createNote(args["title"] as? String ?: "note", args["content"] as? String ?: "")
                "get_current_time" -> getCurrentTime()
                else -> "Unknown skill: ${toolCall.function.name}"
            }
        } catch (e: Exception) {
            "Error executing ${toolCall.function.name}: ${e.message}"
        }
    }

    private fun listFiles(path: String): String {
        val dir = File(path)
        if (!dir.exists()) return "Directory not found: $path"
        if (!dir.isDirectory) return "$path is not a directory"

        val files = dir.listFiles() ?: return "Cannot access: $path (permission denied)"
        if (files.isEmpty()) return "Directory is empty: $path"

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder("📁 Contents of $path:\n\n")
        
        val sorted = files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        for (f in sorted.take(50)) {
            val icon = if (f.isDirectory) "📁" else "📄"
            val size = if (f.isFile) formatSize(f.length()) else "${f.listFiles()?.size ?: 0} items"
            val date = sdf.format(Date(f.lastModified()))
            sb.appendLine("$icon ${f.name}  ($size, $date)")
        }
        if (files.size > 50) sb.appendLine("\n... and ${files.size - 50} more items")
        return sb.toString()
    }

    private fun readFile(path: String): String {
        val file = File(path)
        if (!file.exists()) return "File not found: $path"
        if (!file.isFile) return "$path is not a file"
        if (file.length() > 100_000) return "File too large to read (${formatSize(file.length())}). Max 100KB for text files."
        return try {
            "📄 Content of $path:\n\n${file.readText()}"
        } catch (e: Exception) {
            "Cannot read file (may be binary): ${e.message}"
        }
    }

    private fun writeFile(path: String, content: String): String {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            "✅ File written successfully: $path (${formatSize(file.length())})"
        } catch (e: Exception) {
            "❌ Failed to write file: ${e.message}"
        }
    }

    private fun deleteFile(path: String): String {
        val file = File(path)
        if (!file.exists()) return "File not found: $path"
        return if (file.delete()) "✅ Deleted: $path" else "❌ Failed to delete: $path"
    }

    private fun searchFiles(directory: String, pattern: String): String {
        val dir = File(directory)
        if (!dir.exists()) return "Directory not found: $directory"

        val results = mutableListOf<String>()
        searchRecursive(dir, pattern.lowercase(), results, 0, 5)

        return if (results.isEmpty()) {
            "No files matching '$pattern' found in $directory"
        } else {
            "🔍 Found ${results.size} file(s) matching '$pattern':\n\n${results.joinToString("\n")}"
        }
    }

    private fun searchRecursive(dir: File, pattern: String, results: MutableList<String>, depth: Int, maxDepth: Int) {
        if (depth > maxDepth || results.size >= 30) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (results.size >= 30) break
            if (f.name.lowercase().contains(pattern)) {
                val icon = if (f.isDirectory) "📁" else "📄"
                results.add("$icon ${f.absolutePath}")
            }
            if (f.isDirectory && !f.name.startsWith(".")) {
                searchRecursive(f, pattern, results, depth + 1, maxDepth)
            }
        }
    }

    private fun getDeviceInfo(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong

        return """📱 Device Information:
• Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
• Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
• Storage Total: ${formatSize(totalBytes)}
• Storage Free: ${formatSize(freeBytes)}
• Device Name: ${Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL}"""
    }

    private fun listApps(): String {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val sb = StringBuilder("📱 Installed Apps (${apps.size}):\n\n")
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString()
            sb.appendLine("• $label  (${app.packageName})")
        }
        return sb.toString()
    }

    private fun openApp(packageName: String): String {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "✅ Opened app: $packageName"
        } else {
            "❌ App not found: $packageName"
        }
    }

    private fun openUrl(url: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "✅ Opened URL: $url"
        } catch (e: Exception) {
            "❌ Failed to open URL: ${e.message}"
        }
    }

    private fun createNote(title: String, content: String): String {
        val notesDir = File(Environment.getExternalStorageDirectory(), "AIChatNotes")
        notesDir.mkdirs()
        val safeName = title.replace(Regex("[^a-zA-Z0-9ก-๛._\\- ]"), "_")
        val file = File(notesDir, "${safeName}.txt")
        file.writeText("$title\n${"=".repeat(title.length)}\n\n$content\n\nCreated: ${getCurrentTime()}")
        return "✅ Note saved: ${file.absolutePath}"
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (EEEE)", Locale.getDefault())
        return "🕐 ${sdf.format(Date())}"
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} GB"
        }
    }
}
