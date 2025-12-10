package com.example.promptpilot.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun EnhancedMarkdownText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp
) {
    // Use key to recalculate when text content changes (important for streaming)
    // Using text directly as key ensures recomposition on every text change
    val sanitizedText = remember(text) {
        runCatching { preprocessMarkdown(text) }
            .getOrElse { 
                Log.e("EnhancedMarkdownText", "Preprocessing error: ${it.message}")
                text 
            }
    }
    
    // Check if text might cause issues
    val shouldUsePlainText = sanitizedText.isBlank() || sanitizedText.length > 100000
    
    if (shouldUsePlainText) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            modifier = modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                lineHeight = (fontSize.value * 1.5).sp
            )
        )
    } else {
        MarkdownText(
            markdown = sanitizedText,
            color = color,
            fontSize = fontSize,
            modifier = modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                lineHeight = (fontSize.value * 1.5).sp
            ),
            linkColor = Color(0xFF4A9EFF)
        )
    }
}

/**
 * Preprocesses markdown text to handle special characters and improve rendering
 */
private fun preprocessMarkdown(text: String): String {
    return text
        .trim()
        // Remove excessive newlines (more than 2 consecutive)
        .replace(Regex("\n{3,}"), "\n\n")
        // Ensure proper spacing around code blocks
        .replace(Regex("```(\\w*)\\n"), "```$1\n")
        .replace(Regex("\\n```"), "\n```")
        // Ensure proper spacing around headers
        .replace(Regex("(^|\\n)(#{1,6})\\s*"), "$1$2 ")
        // Clean up excessive spaces
        .replace(Regex(" {2,}"), " ")
}

