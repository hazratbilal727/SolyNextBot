package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptAccentBlue
import com.example.ui.theme.GptBackground
import com.example.ui.theme.GptBorder
import com.example.ui.theme.GptSurface
import com.example.ui.theme.GptSurfaceElevated
import com.example.ui.theme.GptTextMuted
import com.example.ui.theme.GptTextPrimary
import com.example.ui.theme.GptTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onPromptSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GptSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GptBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Capabilities & Tools",
                color = GptTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Instant live tools powered by SolyNext neural core",
                color = GptTextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            ToolRowItem(
                icon = Icons.Default.WbSunny,
                title = "Check Live Weather",
                subtitle = "Real meteorological telemetry via Open-Meteo",
                accentColor = Color(0xFFF59E0B),
                onClick = {
                    onDismiss()
                    onNavigateToTools()
                }
            )

            ToolRowItem(
                icon = Icons.Default.Search,
                title = "Web Search & Citations",
                subtitle = "Real-time web browsing & encyclopedic answers",
                accentColor = GptAccentBlue,
                onClick = {
                    onDismiss()
                    onNavigateToTools()
                }
            )

            ToolRowItem(
                icon = Icons.Default.Alarm,
                title = "Tasks & Reminders",
                subtitle = "Schedule alerts and system alarms",
                accentColor = GptAccent,
                onClick = {
                    onDismiss()
                    onNavigateToReminders()
                }
            )

            ToolRowItem(
                icon = Icons.Default.Psychology,
                title = "Personal Neural Memory",
                subtitle = "Review and edit user-approved facts and context",
                accentColor = Color(0xFF8B5CF6),
                onClick = {
                    onDismiss()
                    onNavigateToMemory()
                }
            )

            ToolRowItem(
                icon = Icons.Default.History,
                title = "Chat History",
                subtitle = "Browse, search, and manage conversation threads",
                accentColor = GptTextSecondary,
                onClick = {
                    onDismiss()
                    onNavigateToHistory()
                }
            )

            ToolRowItem(
                icon = Icons.Default.Settings,
                title = "Settings & AI Models",
                subtitle = "Switch AI providers, configure API keys, adjust voices",
                accentColor = GptTextMuted,
                onClick = {
                    onDismiss()
                    onNavigateToSettings()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToolRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GptSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = GptTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = GptTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
