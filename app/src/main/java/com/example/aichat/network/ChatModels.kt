package com.example.aichat.network

import com.example.aichat.agent.Tool
import com.example.aichat.agent.ToolCall
import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageFull>,
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("tools") val tools: List<Tool>? = null
)

data class ContentPart(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url") val url: String
)

// Simple message for display
data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    val attachmentUri: String? = null // For displaying locally picked image
)

// Full message including tool calls and multimodal content
data class ChatMessageFull(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: Any? = null, // String or List<ContentPart>
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class ChatResponse(
    @SerializedName("choices") val choices: List<Choice>?
)

data class Choice(
    @SerializedName("message") val message: ResponseMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ResponseMessage(
    @SerializedName("role") val role: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>?
)
