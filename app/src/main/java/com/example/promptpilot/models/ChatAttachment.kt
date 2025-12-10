package com.example.promptpilot.models

data class ChatAttachment(
    val name: String = "",
    val url: String = "",
    val type: AttachmentType = AttachmentType.IMAGE
)

enum class AttachmentType {
    IMAGE,
    PDF
}