package com.healthtracker.app.coach

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A problem talking to Gemini, with a message that can be shown as is. */
class CoachError(val userMessage: String, val httpCode: Int = 0) : Exception(userMessage)

data class Turn(val role: String, val text: String)

/** Minimal client for Google's Gemini REST API (generateContent). */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    /** Tried in order until one exists for this key. Can be overridden in settings. */
    val fallbackModels = listOf(
        "gemini-flash-latest", "gemini-2.5-flash", "gemini-flash-lite-latest", "gemini-2.5-flash-lite", "gemini-2.0-flash",
    )

    fun buildRequest(
        system: String,
        turns: List<Turn>,
        maxTokens: Int,
        schema: JSONObject? = null,
        noThinking: Boolean = false,
    ): JSONObject {
        val contents = JSONArray()
        turns.forEach { t ->
            contents.put(JSONObject().put("role", if (t.role == "user") "user" else "model").put("parts", JSONArray().put(JSONObject().put("text", t.text))))
        }
        val config = JSONObject().put("maxOutputTokens", maxTokens).put("temperature", 0.6)
        if (schema != null) config.put("responseMimeType", "application/json").put("responseSchema", schema)
        if (noThinking) config.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", config)
    }

    /** Pulls the answer text out of a generateContent response. */
    fun parseText(body: String): String {
        val root = JSONObject(body)
        root.optJSONObject("promptFeedback")?.optString("blockReason")?.takeIf { it.isNotBlank() }?.let {
            throw CoachError("Coach couldn't answer that one. Try asking another way.")
        }
        val cand = root.optJSONArray("candidates")?.optJSONObject(0) ?: throw CoachError("Coach didn't send an answer. Try again.")
        val parts = cand.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                if (!p.optBoolean("thought", false)) append(p.optString("text"))
            }
        }.trim()
        if (text.isEmpty()) {
            throw when (cand.optString("finishReason")) {
                "MAX_TOKENS" -> CoachError("Coach's answer got cut off. Try a shorter question.")
                "SAFETY" -> CoachError("Coach couldn't answer that one. Try asking another way.")
                else -> CoachError("Coach didn't send an answer. Try again.")
            }
        }
        return text
    }

    fun errorFor(code: Int, body: String): CoachError {
        val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message").orEmpty() }.getOrDefault("")
        return when {
            code == 400 && msg.contains("API key", ignoreCase = true) -> CoachError("That API key isn't valid. Copy it again from Google AI Studio.", code)
            code == 401 || code == 403 -> CoachError("This key can't use Gemini. Check it in Google AI Studio.", code)
            code == 404 -> CoachError("Model not found", code)
            code == 429 -> CoachError("Google's free limit for this key is used up for now. Try again later.", code)
            code >= 500 -> CoachError("Google's servers are busy. Try again in a minute.", code)
            else -> CoachError("Coach hit a problem (${code}). ${msg.take(120)}", code)
        }
    }

    suspend fun post(key: String, model: String, request: JSONObject): String = withContext(Dispatchers.IO) {
        val conn = (URL("$BASE$model:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", key)
        }
        try {
            conn.outputStream.use { it.write(request.toString().toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw errorFor(code, body)
            body
        } catch (e: IOException) {
            throw CoachError("No internet connection. Coach needs the internet; everything else works offline.")
        } finally {
            conn.disconnect()
        }
    }
}
