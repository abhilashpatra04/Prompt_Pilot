package com.example.promptpilot.data.remote

import android.content.ContentValues
import android.util.Log
import com.example.promptpilot.constants.messageCollection
import com.example.promptpilot.helpers.DataHolder
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.models.SenderType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val fsInstance: FirebaseFirestore,
    private val appDatabase: AppDatabase
) : MessageRepository {
    private lateinit var result: QuerySnapshot
    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> =
        callbackFlow {
            val listener = fsInstance
                .collection(messageCollection)
                .whereEqualTo("conversationId", conversationId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageRepo", "Error fetching messages", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val messages = snapshot.documents.mapNotNull {
                            try {
                                it.toObject(MessageModel::class.java)
                            } catch (e: Exception) {
                                Log.e("MessageRepo", "Error parsing message: ${it.id}", e)
                                null
                            }
                        }
                        
                        // Save to Room for offline access (don't duplicate, just update)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                messages.forEach { message ->
                                    appDatabase.messageDao().insertMessage(message.toEntity())
                                }
                            } catch (e: Exception) {
                                Log.e("MessageRepo", "Error saving messages to Room", e)
                            }
                        }
                        
                        trySend(messages)
                    } else {
                        trySend(emptyList())
                    }
                }

            awaitClose { listener.remove() }
        }

    override suspend fun createMessage(message: MessageModel): MessageModel {
        // Save to Room
        appDatabase.messageDao().insertMessage(message.toEntity())
        // Save to Firestore (use message.id as the document ID)
        fsInstance.collection(messageCollection).document(message.id).set(message)
        return message
    }

    override suspend fun updateMessage(message: MessageModel) {
        // Update in Room
        appDatabase.messageDao().insertMessage(message.toEntity())
        // Update in Firestore
        fsInstance.collection(messageCollection).document(message.id).set(message)
    }

    override fun deleteMessage() {
        val docRef = fsInstance
            .collection("messages")
            .document(DataHolder.docPath)

        // Remove the fields from the document
        val updates = hashMapOf<String, Any>(
            "answer" to FieldValue.delete(),
            "conversationId" to FieldValue.delete(),
            "createdAt" to FieldValue.delete(),
            "id" to FieldValue.delete(),
            "question" to FieldValue.delete()
        )
        docRef.update(updates)
            .addOnSuccessListener {
                Log.d(
                    ContentValues.TAG,
                    "DocumentSnapshot successfully deleted from message!"
                )
            }
            .addOnFailureListener { e ->
                Log.w(
                    ContentValues.TAG,
                    "Error deleting document", e
                )
            }

    }
    override suspend fun deleteMessagesByConversation(conversationId: String) {
        val querySnapshot = fsInstance.collection(messageCollection)
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()

        for (doc in querySnapshot.documents) {
            doc.reference.delete()
        }
    }
    override suspend fun fetchMessagesLocal(conversationId: String): List<MessageModel> {
        return appDatabase.messageDao().getMessagesByConversation(conversationId)
            .map { it.toModel() }
    }

    private fun MessageModel.toEntity(): MessageEntity = MessageEntity(
        id = id,
        text = text,
        conversationId = conversationId,
        sender = sender.name,
        timestamp = timestamp,
        question = question,
        answer = answer,
        model = model,
        attachments = attachments
    )
    private fun MessageEntity.toModel(): MessageModel = MessageModel(
        id = id,
        text = text,
        conversationId = conversationId,
        sender = SenderType.valueOf(sender),
        timestamp = timestamp,
        createdAt = java.util.Date(timestamp), // Convert timestamp to Date for UI sorting
        question = question,
        answer = answer,
        model = model,
        attachments = attachments
    )
}
