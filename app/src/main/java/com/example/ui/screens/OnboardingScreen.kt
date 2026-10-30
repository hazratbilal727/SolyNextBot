package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AiProviderType
import com.example.data.provider.GeminiProvider
import com.example.data.provider.OpenAiCompatibleProvider
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBlack
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceActive
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary
import com.example.ui.theme.GptWhite
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: AssistantViewModel,
    onAccountCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var nameText by remember { mutableStateOf("") }
    var photoUriString by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(AiProviderType.GEMINI) }
    var apiKeyText by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var validationSuccess by remember { mutableStateOf(false) }

    // Android Zero-permission photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
        }
    }

    Scaffold(
        containerColor = GptBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Emblem
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(GptSurfaceElevated)
                    .border(1.dp, GptBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_solynext_logo),
                    contentDescription = "SolyNext",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to SolyNext",
                color = GptTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Set up your private local profile and AI engine to begin.",
                color = GptTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Profile Photo Section (Optional)
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(94.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("avatar_picker_box")
            ) {
                UserAvatar(
                    name = nameText,
                    photoUri = photoUriString.ifBlank { null },
                    size = 94.dp
                )

                // Camera action badge
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(GptWhite)
                        .border(2.dp, GptBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload photo",
                        tint = GptBlack,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            TextButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.testTag("upload_photo_btn")
            ) {
                Text(
                    text = if (photoUriString.isNotBlank()) "Change photo" else "Upload photo (optional)",
                    color = GptTextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name Field (Mandatory)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "YOUR NAME *",
                    color = GptTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        validationError = null
                    },
                    placeholder = { Text("Enter your name", color = GptTextMuted, fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GptTextMuted, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Provider Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SELECT AI PROVIDER *",
                    color = GptTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (provider in AiProviderType.values()) {
                        val isSel = selectedProvider == provider
                        Button(
                            onClick = {
                                selectedProvider = provider
                                validationError = null
                                validationSuccess = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) GptWhite else GptSurface,
                                contentColor = if (isSel) GptBlack else GptTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_provider_${provider.name}")
                        ) {
                            Text(provider.displayName.split(" ").first(), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // API Key Input (Mandatory & Validated)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedProvider.displayName.uppercase()} API KEY *",
                        color = GptTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Must be valid",
                        color = GptTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        validationError = null
                        validationSuccess = false
                    },
                    placeholder = {
                        Text(
                            text = when (selectedProvider) {
                                AiProviderType.GEMINI -> "AIzaSy..."
                                AiProviderType.OPENAI -> "sk-proj-..."
                                AiProviderType.GROK -> "xai-..."
                            },
                            color = GptTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = GptTextMuted, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility",
                                tint = GptTextMuted
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_api_key_input"),
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

                Text(
                    text = when (selectedProvider) {
                        AiProviderType.GEMINI -> "Supports any Gemini API key version (Gemini 2.5, 2.0, 1.5, Pro & Flash). Stored privately on-device."
                        AiProviderType.OPENAI -> "Supports OpenAI API keys (GPT-4o, GPT-4o-mini). Stored privately on-device."
                        AiProviderType.GROK -> "Supports xAI Grok API keys. Stored privately on-device."
                    },
                    color = GptTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }

            // Error or Success Banner
            AnimatedVisibility(visible = validationError != null) {
                validationError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlertCrimson.copy(alpha = 0.15f))
                            .border(1.dp, AlertCrimson.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = err,
                            color = AlertCrimson,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = validationSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GptAccent.copy(alpha = 0.15f))
                        .border(1.dp, GptAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✓ API key validated successfully. Initializing account...",
                        color = GptAccent,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Complete Account & Start Button
            Button(
                onClick = {
                    if (nameText.isBlank()) {
                        validationError = "Please enter your name."
                        return@Button
                    }
                    if (apiKeyText.isBlank()) {
                        validationError = "API key is required. Please enter a valid ${selectedProvider.displayName} API key."
                        return@Button
                    }

                    coroutineScope.launch {
                        isValidating = true
                        validationError = null

                        // Create temporary provider to validate the entered key
                        val testProvider = when (selectedProvider) {
                            AiProviderType.GEMINI -> GeminiProvider(customApiKey = apiKeyText.trim())
                            AiProviderType.OPENAI -> OpenAiCompatibleProvider(
                                providerName = "OpenAI",
                                baseUrl = "https://api.openai.com/v1",
                                apiKey = apiKeyText.trim(),
                                currentModel = "gpt-4o-mini"
                            )
                            AiProviderType.GROK -> OpenAiCompatibleProvider(
                                providerName = "xAI Grok",
                                baseUrl = "https://api.x.ai/v1",
                                apiKey = apiKeyText.trim(),
                                currentModel = "grok-beta"
                            )
                        }

                        val result = testProvider.validateConnection()
                        isValidating = false

                        result.onSuccess {
                            validationSuccess = true
                            viewModel.preferencesRepository.completeAccountSetup(
                                name = nameText,
                                photoUri = photoUriString,
                                provider = selectedProvider,
                                apiKey = apiKeyText
                            )
                            onAccountCreated()
                        }.onFailure { ex ->
                            validationError = "API Key Validation Failed: ${ex.localizedMessage ?: "Invalid key or connection timeout"}. Please check your key and try again."
                        }
                    }
                },
                enabled = !isValidating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GptWhite,
                    contentColor = GptBlack
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("create_account_btn")
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = GptBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Validating API Key...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Create Account & Start",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
