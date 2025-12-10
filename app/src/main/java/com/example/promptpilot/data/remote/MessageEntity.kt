package com.example.promptpilot.data.remote

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.promptpilot.models.ChatAttachment

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val sender: String,
    val timestamp: Long,
    val question: String = "",
    val answer: String = "",
    val model: String? = null,
    val attachments: List<ChatAttachment> = emptyList()
)