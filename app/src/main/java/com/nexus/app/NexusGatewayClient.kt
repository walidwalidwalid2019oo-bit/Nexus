package com.nexus.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * NEXUS private AI gateway client.
 *
 * The gateway URL is intentionally blank until a trusted Worker is deployed.
 * Provider secrets belong in the gateway environment, never in this Android app.
 */
internal object NexusGatewayClient {
    const val DEFAULT_ENDPOINT = ""

    fun isConfigured(endpoint: String): Boolean =
        endpoint.startsWith("https://") && endpoint.endsWith("/v1/chat/completions")

    fun send(
        endpoint: String,
        provider: String,
        model: String,
        systemPrompt: String,
        messages: List<ChatMessage>,
        imageDataUrl: String? = null
    ): Pair<String?, String?> {
        var connection: HttpURLConnection? = null
        return try {
            if (!isConfigured(endpoint)) {
                return Pair(null, "Private gateway is not configured yet.")
            }
            if (provider !in setOf("pollinations", "groq")) {
                return Pair(null, "Unknown AI provider.")
            }
            if (model.isBlank()) return Pair(null, "Choose a model in Settings.")

            val requestMessages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
            messages.takeLast(24).forEachIndexed { index, message ->
                val isCurrentPhoto = imageDataUrl != null && index == messages.takeLast(24).lastIndex
                val messageContent: Any = if (isCurrentPhoto) {
                    JSONArray()
                        .put(JSONObject().put("type", "text").put("text", message.content))
                        .put(JSONObject().put("type", "image_url")
                            .put("image_url", JSONObject().put("url", imageDataUrl)))
                } else message.content
                requestMessages.put(JSONObject()
                    .put("role", message.role)
                    .put("content", messageContent))
            }

            val payload = JSONObject()
                .put("provider", provider)
                .put("model", model)
                .put("messages", requestMessages)
                .put("max_tokens", 1200)

            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                Pair(null, "Gateway error " + status + ": " + raw.take(200))
            } else {
                val answer = JSONObject(raw).optJSONArray("choices")
                    ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                if (answer.isNullOrBlank()) Pair(null, "The gateway returned an empty response.")
                else Pair(answer, null)
            }
        } catch (error: Exception) {
            Pair(null, "Gateway connection error: " + (error.message ?: "unknown error").take(140))
        } finally {
            connection?.disconnect()
        }
    }
}
