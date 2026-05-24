@file:Suppress("SpellCheckingInspection")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.clima_v100.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.clima_v100.data.remote.CurrentWeatherResponse
import com.example.clima_v100.ui.viewmodel.WeatherTheme
import com.example.clima_v100.ui.viewmodel.WeatherUiState
import com.example.clima_v100.ui.viewmodel.WeatherViewModel

// Colores por tema
fun getThemeColors(theme: WeatherTheme): Triple<Color, Color, Color> = when (theme) {
    WeatherTheme.HOT   -> Triple(Color(0xFFFF6B35), Color(0xFFFFA552), Color(0xFFFFD166))
    WeatherTheme.WARM  -> Triple(Color(0xFF56CCF2), Color(0xFF2F80ED), Color(0xFF1A5276))
    WeatherTheme.COLD  -> Triple(Color(0xFF2D3561), Color(0xFF4A5899), Color(0xFF7B8FCC))
    WeatherTheme.RAINY -> Triple(Color(0xFF1B4965), Color(0xFF2E6F95), Color(0xFF5BA4CF))
    WeatherTheme.NIGHT -> Triple(Color(0xFF0D0D1A), Color(0xFF1A1A35), Color(0xFF2D2D5C))
}

fun getThemeEmoji(theme: WeatherTheme): String = when (theme) {
    WeatherTheme.HOT   -> "☀️"
    WeatherTheme.WARM  -> "⛅"
    WeatherTheme.COLD  -> "🌨️"
    WeatherTheme.RAINY -> "🌧️"
    WeatherTheme.NIGHT -> "🌙"
}

@Composable
fun HomeScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme by viewModel.theme.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.fetchWeather()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val (color1, color2, color3) = getThemeColors(theme)
    val animColor1 by animateColorAsState(color1, animationSpec = tween(1000), label = "c1")
    val animColor2 by animateColorAsState(color2, animationSpec = tween(1000), label = "c2")
    val animColor3 by animateColorAsState(color3, animationSpec = tween(1000), label = "c3")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(animColor1, animColor2, animColor3))
            )
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> LoadingContent()
            is WeatherUiState.Error   -> ErrorContent(state.message) { viewModel.fetchWeather() }
            is WeatherUiState.Success -> WeatherContent(state.data, state.city, theme)
        }
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Obteniendo clima...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
            ) { Text("Reintentar", color = Color.White) }
        }
    }
}

@Composable
fun WeatherContent(data: CurrentWeatherResponse, city: String, theme: WeatherTheme) {
    val textColor = Color.White
    val cardColor = Color.White.copy(alpha = 0.18f)
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ciudad
        Text(city, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(data.location.localtime, color = textColor.copy(alpha = 0.75f), fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))

        // Icono principal
        Text(getThemeEmoji(theme), fontSize = 80.sp)

        Spacer(Modifier.height(8.dp))

        // Temperatura principal
        Text(
            "${data.current.temp_c.toInt()}°C",
            color = textColor,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            data.current.condition.text,
            color = textColor.copy(alpha = 0.85f),
            fontSize = 18.sp
        )

        Text(
            "Sensación: ${data.current.feelslike_c.toInt()}°C",
            color = textColor.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(28.dp))

        // Tarjeta info principal
        WeatherCard(cardColor) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat("💧", "Humedad", "${data.current.humidity}%", textColor)
                WeatherStat("💨", "Viento", "${data.current.wind_kph.toInt()} km/h", textColor)
                WeatherStat("🌬️", "Ráfagas", "${data.current.gust_kph.toInt()} km/h", textColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tarjeta UV y presión
        WeatherCard(cardColor) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat("☀️", "Índice UV", "${data.current.uv.toInt()}", textColor)
                WeatherStat("🌡️", "Presión", "${data.current.pressure_mb.toInt()} hPa", textColor)
                WeatherStat("👁️", "Visibilidad", "${data.current.vis_km.toInt()} km", textColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tarjeta viento y nubosidad
        WeatherCard(cardColor) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat("🧭", "Dirección", data.current.wind_dir, textColor)
                WeatherStat("☁️", "Nubosidad", "${data.current.cloud}%", textColor)
                WeatherStat("🌧️", "Precip.", "${data.current.precip_mm} mm", textColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tarjeta min/max
        WeatherCard(cardColor) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStat("🔼", "Temp. °F", "${data.current.temp_f.toInt()}°F", textColor)
                WeatherStat("🌡️", "Sens. °F", "${data.current.feelslike_f.toInt()}°F", textColor)
                WeatherStat(
                    if (data.current.is_day == 1) "🌞" else "🌑",
                    "Momento",
                    if (data.current.is_day == 1) "Día" else "Noche",
                    textColor
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Indicador UV
        UvIndexBar(data.current.uv, cardColor, textColor)
    }
}

@Composable
fun WeatherCard(cardColor: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
    ) { content() }
}

@Composable
fun WeatherStat(emoji: String, label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(label, color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun UvIndexBar(uv: Float, cardColor: Color, textColor: Color) {
    val uvLevel = when {
        uv <= 2  -> "Bajo" to Color(0xFF4CAF50)
        uv <= 5  -> "Moderado" to Color(0xFFFFEB3B)
        uv <= 7  -> "Alto" to Color(0xFFFF9800)
        uv <= 10 -> "Muy alto" to Color(0xFFF44336)
        else     -> "Extremo" to Color(0xFF9C27B0)
    }

    WeatherCard(cardColor) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☀️ Índice UV", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${uv.toInt()} — ${uvLevel.first}",
                    color = uvLevel.second,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (uv / 11f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = uvLevel.second,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}