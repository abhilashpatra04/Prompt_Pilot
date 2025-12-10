package com.example.promptpilot.data.remote

import com.example.promptpilot.models.MessageModel
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun fetchMessages(conversationId: String): Flow<List<MessageModel>>
    suspend fun createMessage(message: MessageModel): MessageModel
    suspend fun updateMessage(message: MessageModel)
    fun deleteMessage()
    suspend fun deleteMessagesByConversation(conversationId: String)
    suspend fun fetchMessagesLocal(conversationId: String): List<MessageModel>
}