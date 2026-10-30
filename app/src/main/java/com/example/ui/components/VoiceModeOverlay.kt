package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptAccentBlue
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceActive
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary
import com.example.ui.theme.GptWhite
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun VoiceModeOverlay(
    viewModel: AssistantViewModel,
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioDecibels by viewModel.voiceManager.audioDecibels.collectAsState()
    val liveTranscript by viewModel.voiceManager.liveTranscript.collectAsState()
    val currentProvider by viewModel.preferencesRepository.selectedProviderFlow.collectAsState(initial = com.example.data.model.AiProviderType.GEMINI)

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GptBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (assistantState == AssistantState.ERROR) AlertCrimson else GptAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Voice Session • ${currentProvider.displayName}",
                        color = GptTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.voiceManager.stopListening()
                        viewModel.stopSpeaking()
                        onClose()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GptSurface)
                        .testTag("close_voice_mode_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Session",
                        tint = GptTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center Content: Minimalist AI Voice Orb & Subtitles
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FuturisticAiOrb(
                    state = assistantState,
                    audioLevel = audioDecibels,
                    onOrbClick = { viewModel.toggleVoiceListening() },
                    size = 230.dp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // State indicator pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GptSurface)
                        .border(1.dp, GptBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (assistantState) {
                            AssistantState.LISTENING -> "Listening..."
                            AssistantState.THINKING -> "Thinking..."
                            AssistantState.SPEAKING -> "Speaking..."
                            AssistantState.ERROR -> "Offline / Check API"
                            AssistantState.IDLE -> "Tap orb or mic to speak"
                        },
                        color = when (assistantState) {
                            AssistantState.LISTENING -> GptAccent
                            AssistantState.SPEAKING -> GptAccentBlue
                            AssistantState.ERROR -> AlertCrimson
                            else -> GptTextSecondary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Real-time spoken transcript
                Text(
                    text = if (liveTranscript.isNotBlank()) "\"$liveTranscript\""
                    else if (isSpeaking) "SolyNext is responding aloud..."
                    else if (isListening) "Speak naturally..."
                    else "",
                    color = GptTextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }

            // Bottom Controls Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute / Resume Toggle
                IconButton(
                    onClick = { viewModel.toggleVoiceListening() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isListening) GptWhite else GptSurface)
                        .border(1.dp, GptBorder, CircleShape)
                        .testTag("voice_mode_mic_btn")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone toggle",
                        tint = if (isListening) GptBackground else GptTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isSpeaking) {
                    Spacer(modifier = Modifier.width(20.dp))

                    // Interrupt / Stop Speech Button
                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GptSurface)
                            .border(1.dp, GptBorder, CircleShape)
                            .testTag("voice_mode_stop_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Speaking",
                            tint = AlertCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
