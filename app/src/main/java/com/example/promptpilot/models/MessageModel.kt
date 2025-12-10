package com.example.promptpilot.models

import java.util.*

//enum class AttachmentType { IMAGE, PDF }
//
//data class ChatAttachment(
//    val name: String = "",
//    val url: String = "",
//    val type: AttachmentType = AttachmentType.IMAGE,
//    val useAsContext: Boolean = true
//)

data class MessageModel (
    var id: String = "",
    var conversationId: String = "",
    var question: String = "",
    var answer: String = "",
    var createdAt: Date = Date(),
    var imageUrl: String? = null,
    val text: String = "",
    val sender: SenderType = SenderType.USER,
    val timestamp: Long = 0L,
    val model: String? = null,
    val attachments: List<ChatAttachment> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageModel

        if (id != other.id) return false
        if (conversationId != other.conversationId) return false
        if (question != other.question) return false
        if (answer != other.answer) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + question.hashCode()
        result = 31 * result + answer.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}