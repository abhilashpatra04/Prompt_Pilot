
package com.example.promptpilot.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.promptpilot.models.AgentType
import com.example.promptpilot.models.AttachmentType
import com.example.promptpilot.models.ChatAttachment
import com.example.promptpilot.models.MessageModel
import com.example.promptpilot.ui.theme.MainTheme
import com.example.promptpilot.viewmodels.ConversationViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ModernConversation(
    conversationViewModel: ConversationViewModel = hiltViewModel()
) {
    val conversationId by conversationViewModel.currentConversationState.collectAsState()
    val messagesMap by conversationViewModel.messagesState.collectAsState()
    val messages: List<MessageModel> = messagesMap[conversationId] ?: emptyList()
    val isStreaming by conversationViewModel.isStreaming.collectAsState()
    val (zoomedAttachment, setZoomedAttachment) = remember { mutableStateOf<ChatAttachment?>(null) }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val isTtsEnabled by conversationViewModel.isTtsEnabled.collectAsState()

    // Initialize TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    // Cleanup TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Auto-read AI responses
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty() && !isStreaming && isTtsEnabled) {
            val latestMessage = messages.maxByOrNull { it.createdAt }
            if (latestMessage?.answer?.isNotBlank() == true &&
                latestMessage.answer != "Thinking..." &&
                latestMessage.answer.length > 3) {
                tts?.speak(latestMessage.answer, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    // Stop TTS when toggled off
    LaunchedEffect(isTtsEnabled) {
        if (!isTtsEnabled) {
            tts?.stop()
        }
    }

    // State for scrolling the message list
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to latest message when new messages arrive or during streaming
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                // Scroll to the bottom (latest message)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    MainTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(8.dp)
                ) {
                    ModernMessageList(
                        messages = messages,
                        listState = listState,
                        onAttachmentClick = { setZoomedAttachment(it) },
                        isStreaming = isStreaming
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // The modern input box at the bottom
                val selectedModel by conversationViewModel.selectedModel.collectAsState()
                val supportsImage = selectedModel.model.startsWith("gemini")
                val context = LocalContext.current

                Box(
                    modifier = Modifier.imePadding()
                ) {
                    ModernTextInput(
                        onSend = { prompt, imageUrls, webSearch, agentType ->
                            coroutineScope.launch {
                                tts?.stop()
                                conversationViewModel.sendMessage(
                                    message = prompt,
                                    attachments = imageUrls,
                                    context = context,
                                    webSearch = webSearch,
                                    agentType = agentType
                                )
                            }
                        },
                        supportsImage = supportsImage,
                        isStreaming = isStreaming
                    )
                }
            }
        }
    }

    // Zoomed attachment dialog
    if (zoomedAttachment != null) {
        AlertDialog(
            onDismissRequest = { setZoomedAttachment(null) },
            confirmButton = {},
            dismissButton = {},
            title = { Text(zoomedAttachment.name) },
            text = {
                if (zoomedAttachment.type == AttachmentType.IMAGE) {
                    Image(
                        painter = rememberAsyncImagePainter(zoomedAttachment.url),
                        contentDescription = null,
                        modifier = Modifier.size(300.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = Color.Red
                    )
                }
            }
        )
    }
}


// Enhanced message list with proper ordering and streaming indicator
@Composable
fun ModernMessageList(
    messages: List<MessageModel>,
    listState: LazyListState,
    onAttachmentClick: (ChatAttachment) -> Unit,
    isStreaming: Boolean
) {
    // Sort messages by timestamp (oldest to newest) for proper chat flow
    val sortedMessages = messages.sortedBy { it.createdAt }

    LazyColumn(
        state = listState,
        reverseLayout = false, // Normal layout: oldest at top, newest at bottom
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedMessages.size) { index ->
            val message = sortedMessages[index]
            val isLatestMessage = index == sortedMessages.size - 1

            // Show user message bubble if question is not blank
            if (message.question.isNotBlank()) {
                ModernMessageBubble(
                    message = message,
                    isUser = true,
                    onAttachmentClick = onAttachmentClick
                )
            }

            // Show AI response bubble if answer is not blank or if it's streaming
            if (message.answer.isNotBlank() && message.answer != "Thinking...") {
                ModernMessageBubble(
                    message = message,
                    isUser = false,
                    onAttachmentClick = onAttachmentClick,
                    isStreaming = isStreaming && isLatestMessage
                )
            } else if (isStreaming && isLatestMessage && message.answer == "Thinking...") {
                // Show thinking indicator for streaming
                StreamingIndicator()
            }
        }

        // Add some bottom padding so the last message isn't right against the input
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.Gray,
            strokeWidth = 2.dp
        )
        Text(
            text = "AI is thinking...",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Enhanced message bubble with better alignment and streaming support
@Composable
fun ModernMessageBubble(
    message: MessageModel,
    isUser: Boolean,
    onAttachmentClick: (ChatAttachment) -> Unit,
    isStreaming: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isUser) {
            // User message - right aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                // Show attachments if any
                if (message.attachments.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        items(message.attachments) { attachment ->
                            AttachmentChip(
                                attachment = attachment,
                                onClick = { onAttachmentClick(attachment) }
                            )
                        }
                    }
                }

                // User message bubble
                Surface(
                    color = Color.Gray,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    ),
                    shadowElevation = 2.dp,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.question,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            // AI message - left aligned
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // AI message bubble
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Use EnhancedMarkdownText for AI responses
//                            Box(modifier = Modifier.weight(1f)) {
//
//                            }
                            EnhancedMarkdownText(
                                text = message.answer,
                                color = Color.White,
                                fontSize = 16.sp
                            )

//                            Text(
//                                text = message.answer,
//                                color = Color.White,
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Normal,
//                                modifier = Modifier.weight(1f)
//                            )

                            // Show typing indicator if streaming
                            if (isStreaming) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 8.dp),
                                    color = Color.Gray,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }

                // Show model name for AI messages
                if (message.model != null) {
                    Text(
                        text = "• ${message.model}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentChip(
    attachment: ChatAttachment,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray)
            .border(1.dp, Color.White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (attachment.type == AttachmentType.IMAGE)
                    Icons.Default.Image else Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = attachment.name,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}