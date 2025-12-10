package com.example.promptpilot.models

enum class AI_Model(
    val model: String,
    isChatCompletion1: Int,
    val isChatCompletion: Boolean = false
) {
    Maverick("meta-llama/llama-3.3-70b-instruct:free", 500, isChatCompletion = true),
    Gemini_pro_imgpdf("gemini-2.5-pro", 500, isChatCompletion = true),
    Gemini_img("gemini-2.5-flash", 500, isChatCompletion = true),
    Groq("qwen/qwen3-32b", 500, isChatCompletion = true),
}
