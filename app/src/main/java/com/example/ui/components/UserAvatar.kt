package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurfaceActive
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptWhite

@Composable
fun UserAvatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cleanName = name.trim()
    val initial = if (cleanName.isNotEmpty()) cleanName.first().uppercase() else ""

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GptSurfaceActive)
            .border(1.dp, GptBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else if (initial.isNotEmpty()) {
            Text(
                text = initial,
                color = GptWhite,
                fontSize = (size.value * 0.44f).sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User",
                tint = GptTextPrimary,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
