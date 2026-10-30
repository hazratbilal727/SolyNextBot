package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBlack
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary
import com.example.ui.theme.GptWhite
import com.example.ui.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val conversations by viewModel.allConversations.collectAsState()
    val currentConvId by viewModel.currentConversationId.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredConversations = conversations.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GptBackground),
                title = { Text("Chat History", color = GptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("history_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GptTextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.createNewConversation()
                    onNavigateBack()
                },
                containerColor = GptWhite,
                contentColor = GptBlack,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("new_chat_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        },
        containerColor = GptBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search chats...", color = GptTextMuted, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_history_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GptBorder,
                    unfocusedBorderColor = GptBorder,
                    focusedTextColor = GptTextPrimary,
                    unfocusedTextColor = GptTextPrimary,
                    focusedContainerColor = GptSurface,
                    unfocusedContainerColor = GptSurface
                )
            )

            if (filteredConversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = GptTextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No conversation history", color = GptTextSecondary, fontSize = 15.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val isSelected = conv.id == currentConvId
                        val dateFormatted = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date(conv.updatedAt))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GptSurfaceElevated else GptSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GptTextSecondary else GptBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectConversation(conv.id)
                                    onNavigateBack()
                                }
                                .testTag("conversation_item_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) GptWhite.copy(alpha = 0.15f) else GptBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isSelected) GptWhite else GptTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        color = GptTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = dateFormatted,
                                        color = GptTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteConversation(conv.id) },
                                    modifier = Modifier.testTag("delete_conv_${conv.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete conversation",
                                        tint = GptTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
