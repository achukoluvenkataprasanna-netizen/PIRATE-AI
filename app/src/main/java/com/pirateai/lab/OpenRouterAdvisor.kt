package com.pirateai.lab

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Optional remote strategy explainer. The caller supplies a key at runtime;
 * this class never stores it. Send only user-approved, non-sensitive game summaries.
 * Remote advice is text-only and cannot dispatch device gestures.
 */
class OpenRouterAdvisor(
    private val model: String = "openai/gpt-4o-mini"
) {
    suspend fun advise(apiKey: String, gameSummary: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.startsWith("sk-or-")) { "Enter a valid OpenRouter API key." }
            require(gameSummary.isNotBlank() && gameSummary.length <= 4000) { "Game summary must be 1–4000 characters." }
            val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/achukoluvenkataprasanna-netizen/PIRATE-AI")
                setRequestProperty("X-Title", "PIRATE-AI Universal Game Lab")
            }
            try {
                val body = JSONObject()
                    .put("model", model)
                    .put("temperature", 0.3)
                    .put("max_tokens", 350)
                    .put("messages", JSONArray().put(JSONObject()
                        .put("role", "system")
                        .put("content", "You are a practice coach. Give concise, legal, non-cheating strategy suggestions for user-owned or authorised test games. Never propose bypassing anti-cheat, exploiting services, purchases, messaging, or unrelated device actions. Advice only; do not claim to control the device."))
                        .put(JSONObject().put("role", "user").put("content", gameSummary)))
                    .toString()
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (code !in 200..299) error("OpenRouter HTTP $code: ${response.take(300)}")
                JSONObject(response).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content").take(3000)
            } finally {
                connection.disconnect()
            }
        }
    }
}
