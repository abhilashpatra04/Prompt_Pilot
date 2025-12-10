package com.example.promptpilot.data.remote

import android.util.Log
import com.example.promptpilot.data.api.BackendApi
import com.example.promptpilot.data.api.BackendChatRequest
import com.example.promptpilot.models.TextCompletionsParam
import com.google.firebase.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class OpenAIRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi,
    @com.example.promptpilot.di.StreamingClient private val okHttpClient: OkHttpClient
) : OpenAIRepository {

    companion object {
        private const val TIMEOUT_SECONDS = 120L // 2 minutes timeout
        private const val BASE_URL = "https://promptpilot-backend-o5fj.onrender.com"
    }

    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> = flow {
        throw UnsupportedOperationException("Direct OpenAI streaming is deprecated. Use getStreamingAIResponse instead.")
    }

    // Regular non-streaming response
    override suspend fun getAIResponseFromBackend(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        title: String,
        image_urls: List<String>?
    ): String {
        Log.d("PromptPilot", "getAIResponseFromBackend called with: $prompt, $model")
        return try {
            val backendRequest = BackendChatRequest(
                uid = uid,
                prompt = prompt,
                model = model,
                chat_id = chatId,
                title = title,
                image_urls = image_urls,
                stream = false // Explicitly set to false for non-streaming
            )

            val response = withTimeoutOrNull(TIMEOUT_SECONDS * 1000) {
                backendApi.getAIResponse(backendRequest)
            }

            if (response != null) {
                Log.d("PromptPilot", "AI reply received: ${response.reply}")
                response.reply
            } else {
                "Request timed out. Please try again with a shorter message."
            }
        } catch (e: Exception) {
            Log.e("PromptPilot", "Error getting AI response", e)
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try again."
                e.message?.contains("network", ignoreCase = true) == true -> "Network error. Please check your connection."
                else -> "Unable to connect to server. Please try again."
            }
        }
    }

    override suspend fun getStreamingAIResponse(
        uid: String,
        prompt: String,
        model: String,
        chatId: String?,
        imageUrls: List<String>?,
        webSearch: Boolean,
        agentType: String?,
        onChunkReceived: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        Log.d("PromptPilot", "getStreamingAIResponse called with: $prompt, webSearch: $webSearch, agent: $agentType")

        try {
            // Create JSON request body with proper null handling
            val requestBody = JSONObject().apply {
                put("uid", uid)
                put("prompt", prompt)
                put("model", model)
                put("chat_id", chatId ?: JSONObject.NULL)
                put("title", "Untitled")
                put("image_urls", if (!imageUrls.isNullOrEmpty()) {
                    org.json.JSONArray().apply {
                        imageUrls.forEach { put(it) }
                    }
                } else {
                    JSONObject.NULL
                })
                put("web_search", webSearch)
                put("agent_type", agentType ?: JSONObject.NULL)
                put("stream", true)
            }

            Log.d("PromptPilot", "Request body: ${requestBody.toString()}")

            // Create request with proper timeout configuration
            val request = Request.Builder()
                .url("$BASE_URL/chat")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/plain")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("User-Agent", "PromptPilot-Android/1.0")
                .build()

            // Create a client with longer timeouts for streaming
            val streamingClient = okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val response = streamingClient.newCall(request).execute()

            // Enhanced logging for debugging
            Log.d("PromptPilot", "Response Code: ${response.code}")
            Log.d("PromptPilot", "Response Message: ${response.message}")
            Log.d("PromptPilot", "Response Headers: ${response.headers}")
            Log.d("PromptPilot", "Response Protocol: ${response.protocol}")

            if (!response.isSuccessful) {
                val errorBody = response.body.string()
                Log.e("PromptPilot", "HTTP Error ${response.code}: $errorBody")
                Log.e("PromptPilot", "Full response details: Code=${response.code}, Message=${response.message}, Protocol=${response.protocol}")

                val errorMessage = when (response.code) {
                    401 -> "Authentication error. Server returned 401. Check API configuration."
                    403 -> "Forbidden. Please check your permissions."
                    404 -> "Endpoint not found. Please check your server URL: $BASE_URL"
                    405 -> "Method not allowed. Server may not support POST method."
                    500 -> "Server error. Please try again."
                    503 -> "Service temporarily unavailable. Please try again later."
                    else -> "Connection error (${response.code}: ${response.message}). Please try again."
                }
                throw Exception(errorMessage)
            }

            val responseBody = response.body
            
            // First, try to parse as direct JSON response (non-streaming)
            // The backend may return JSON directly instead of SSE format
            val responseBytes = responseBody.bytes()
            val responseString = String(responseBytes)
            Log.d("PromptPilot", "Raw response (first 500 chars): ${responseString.take(500)}")
            
            // Check if it's a direct JSON response (not SSE)
            if (responseString.trim().startsWith("{") && !responseString.contains("data:")) {
                try {
                    val jsonResponse = JSONObject(responseString)
                    if (jsonResponse.has("reply")) {
                        val reply = jsonResponse.getString("reply")
                        Log.d("PromptPilot", "Parsed direct JSON reply: ${reply.length} chars")
                        onChunkReceived(reply)
                        return@withContext reply
                    }
                } catch (e: Exception) {
                    Log.d("PromptPilot", "Not a direct JSON response, trying SSE parsing: ${e.message}")
                }
            }
            
            // Fall back to SSE streaming parsing
            val reader = BufferedReader(InputStreamReader(responseBytes.inputStream()))

            var fullResponse = ""
            var line: String?
            var lastUpdateTime = System.currentTimeMillis()
            var lineCount = 0

            try {
                while (reader.readLine().also { line = it } != null) {
                    lineCount++
                    Log.d("PromptPilot", "Received line $lineCount: $line") // Debug each line

                    val currentTime = System.currentTimeMillis()

                    // Check for timeout during streaming
                    if (currentTime - lastUpdateTime > TIMEOUT_SECONDS * 1000) {
                        Log.w("PromptPilot", "Streaming timeout - no data received for ${TIMEOUT_SECONDS} seconds")
                        break
                    }

                    // Handle different line formats
                    when {
                        line!!.startsWith("data: ") -> {
                            val jsonStr = line.substring(6).trim()
                            Log.d("PromptPilot", "Processing JSON: $jsonStr")

                            if (jsonStr == "[DONE]" || jsonStr.isEmpty()) {
                                Log.d("PromptPilot", "Streaming completed normally")
                                break
                            }

                            try {
                                val jsonData = JSONObject(jsonStr)
                                Log.d("PromptPilot", "Parsed JSON keys: ${jsonData.keys().asSequence().toList()}")

                                when {
                                    jsonData.has("chunk") -> {
                                        val chunk = jsonData.getString("chunk")
                                        if (chunk.isNotEmpty()) {
                                            fullResponse += chunk
                                            onChunkReceived(fullResponse)
                                            lastUpdateTime = currentTime
                                            Log.d("PromptPilot", "Added chunk, total length: ${fullResponse.length}")
                                        }
                                    }
                                    jsonData.has("done") && jsonData.getBoolean("done") -> {
                                        Log.d("PromptPilot", "Received done signal")
                                        break
                                    }
                                    jsonData.has("error") -> {
                                        val error = jsonData.getString("error")
                                        Log.e("PromptPilot", "Server error: $error")
                                        throw Exception("Server error: $error")
                                    }
                                    jsonData.has("reply") -> {
                                        val reply = jsonData.getString("reply")
                                        if (reply.isNotEmpty()) {
                                            fullResponse = reply // For non-streaming responses
                                            onChunkReceived(fullResponse)
                                            lastUpdateTime = currentTime
                                            Log.d("PromptPilot", "Received full reply: ${reply.length} chars")
                                        }
                                    }
                                    else -> {
                                        Log.w("PromptPilot", "Unknown JSON format: $jsonStr")
                                    }
                                }
                            } catch (jsonException: Exception) {
                                Log.e("PromptPilot", "JSON parsing error for line: $jsonStr", jsonException)
                                // Try to handle as plain text
                                if (jsonStr.isNotEmpty() && !jsonStr.startsWith("{")) {
                                    fullResponse += jsonStr
                                    onChunkReceived(fullResponse)
                                    lastUpdateTime = currentTime
                                }
                            }
                        }
                        line.isNotEmpty() -> {
                            Log.d("PromptPilot", "Non-SSE line: $line")
                            // Handle non-SSE format responses
                            fullResponse += line + "\n"
                            onChunkReceived(fullResponse)
                            lastUpdateTime = currentTime
                        }
                    }
                }
            } finally {
                Log.d("PromptPilot", "Streaming finished. Total lines: $lineCount, Response length: ${fullResponse.length}")
                try {
                    reader.close()
                    response.close()
                } catch (e: Exception) {
                    Log.w("PromptPilot", "Error closing resources", e)
                }
            }

            Log.d("PromptPilot", "Streaming completed. Full response length: ${fullResponse.length}")

            if (fullResponse.isEmpty()) {
                throw Exception("No response received from server")
            }

            fullResponse

        } catch (e: Exception) {
            Log.e("PromptPilot", "Error in streaming response", e)
            val errorMessage = when {
                e.message?.contains("401", ignoreCase = true) == true ->
                    "Authentication error. Please check your API configuration."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Response timed out. Please try with a shorter message."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                e.message?.contains("server error", ignoreCase = true) == true ->
                    e.message ?: "Server error occurred."
                else -> "Connection error. Please try again."
            }

            onChunkReceived(errorMessage)
            errorMessage
        }
    }
}