package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MessageEntity
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary

@Composable
fun MessageItemCard(
    message: MessageEntity,
    onSpeakMessage: (String) -> Unit,
    userName: String = "",
    userPhotoUri: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.sender == "user"
    val isTool = message.isToolExecution

    if (isUser) {
        // User Message: Crisp capsule bubble aligned to the right with user avatar / initial
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 285.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GptSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = GptTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            UserAvatar(
                name = userName,
                photoUri = userPhotoUri,
                size = 28.dp
            )
        }
    } else {
        // Assistant Message: ChatGPT-style clean layout with avatar icon and subtle action bar
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(GptSurfaceElevated)
                        .border(1.dp, GptBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_solynext_logo),
                        contentDescription = "SolyNext",
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "SolyNext",
                    color = GptTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (isTool) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GptAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = message.toolType ?: "Tool Action",
                            color = GptAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Text Body
            if (isTool) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GptSurface)
                        .border(1.dp, GptBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = message.text,
                        color = GptTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = message.text,
                    color = GptTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(start = 2.dp, end = 8.dp)
                )
            }

            // Subtle Action Row (Copy, Read Aloud)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SolyNext Response", message.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_message_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = GptTextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { onSpeakMessage(message.text) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("speak_message_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak message",
                        tint = GptTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
