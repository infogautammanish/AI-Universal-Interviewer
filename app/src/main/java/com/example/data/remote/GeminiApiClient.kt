package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiApiClient", "No active Gemini API key configured in BuildConfig. Falling back to internal engine.")
            return@withContext null
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partObj = JSONObject().put("text", prompt)
                    val partsArray = JSONArray().put(partObj)
                    put(JSONObject().put("parts", partsArray))
                }
                put("contents", contentsArray)

                if (!systemInstruction.isNullOrBlank()) {
                    val sysPart = JSONObject().put("text", systemInstruction)
                    val sysParts = JSONArray().put(sysPart)
                    put("systemInstruction", JSONObject().put("parts", sysParts))
                }

                val config = JSONObject().apply {
                    put("temperature", 0.3)
                    put("topP", 0.95)
                    put("topK", 40)
                }
                put("generationConfig", config)
            }

            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiApiClient", "Gemini API error code: ${response.code} message: ${response.message}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text")
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Gemini request failed: ${e.message}", e)
            null
        }
    }
}
