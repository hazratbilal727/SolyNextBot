package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ToolExecutionResult
import com.example.ui.theme.GptAccent
import com.example.ui.theme.GptAccentBlue
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var weatherCity by remember { mutableStateOf("London") }
    var weatherResult by remember { mutableStateOf<ToolExecutionResult?>(null) }
    var isWeatherLoading by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("Artificial Intelligence") }
    var searchResult by remember { mutableStateOf<ToolExecutionResult?>(null) }
    var isSearchLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GptBackground),
                title = { Text("Tools & Telemetry", color = GptTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tools_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GptTextPrimary)
                    }
                }
            )
        },
        containerColor = GptBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weather Tool Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("weather_tool_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GptSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Weather Telemetry", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Live Open-Meteo API forecast", color = GptTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = weatherCity,
                            onValueChange = { weatherCity = it },
                            placeholder = { Text("City name", color = GptTextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("weather_city_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GptBorder,
                                unfocusedBorderColor = GptBorder,
                                focusedTextColor = GptTextPrimary,
                                unfocusedTextColor = GptTextPrimary,
                                focusedContainerColor = GptSurfaceElevated,
                                unfocusedContainerColor = GptSurfaceElevated
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isWeatherLoading = true
                                    weatherResult = viewModel.toolDispatcher.executeWeather(weatherCity)
                                    isWeatherLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GptWhite, contentColor = GptBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("fetch_weather_btn")
                        ) {
                            if (isWeatherLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GptBlack, strokeWidth = 2.dp)
                            } else {
                                Text("Check")
                            }
                        }
                    }

                    weatherResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GptSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Text(res.summary, color = GptTextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Web Search Tool Card
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("search_tool_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GptSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GptAccentBlue, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Web Search & Citations", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Instant web and encyclopedia search", color = GptTextSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search query", color = GptTextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("web_search_input"),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GptBorder,
                                unfocusedBorderColor = GptBorder,
                                focusedTextColor = GptTextPrimary,
                                unfocusedTextColor = GptTextPrimary,
                                focusedContainerColor = GptSurfaceElevated,
                                unfocusedContainerColor = GptSurfaceElevated
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSearchLoading = true
                                    searchResult = viewModel.toolDispatcher.executeSearch(searchQuery)
                                    isSearchLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GptWhite, contentColor = GptBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("execute_search_btn")
                        ) {
                            if (isSearchLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GptBlack, strokeWidth = 2.dp)
                            } else {
                                Text("Search")
                            }
                        }
                    }

                    searchResult?.let { res ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GptSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(res.summary, color = GptTextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                                if (!res.sourceUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(res.sourceUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = GptAccentBlue, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Source Link", color = GptAccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Device Shortcuts
            Card(
                colors = CardDefaults.cardColors(containerColor = GptSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
                modifier = Modifier.fillMaxWidth().testTag("device_actions_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Quick Actions", color = GptTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Open native Android system capabilities", color = GptTextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionTile(
                            icon = Icons.Default.Alarm,
                            label = "Clock / Alarm",
                            color = GptTextPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            try { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } catch (_: Exception) {}
                        }
                        ActionTile(
                            icon = Icons.Default.CalendarToday,
                            label = "Calendar",
                            color = GptTextPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            try {
                                val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionTile(
                            icon = Icons.Default.Call,
                            label = "Phone Dialer",
                            color = GptTextPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            try { context.startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) {}
                        }
                        ActionTile(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            color = GptTextPrimary,
                            modifier = Modifier.weight(1f)
                        ) {
                            try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = GptSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, GptBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = GptTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
