package com.example.clima_v100.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clima_v100.BuildConfig
import com.example.clima_v100.data.remote.CurrentWeatherResponse
import com.example.clima_v100.data.remote.WeatherApiService
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: CurrentWeatherResponse, val city: String) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

enum class WeatherTheme {
    HOT, WARM, COLD, RAINY, NIGHT
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherApiService: WeatherApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    private val _theme = MutableStateFlow(WeatherTheme.WARM)
    val theme: StateFlow<WeatherTheme> = _theme

    @SuppressLint("MissingPermission")
    fun fetchWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)

                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        viewModelScope.launch {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                )
                                val city = addresses?.firstOrNull()?.locality ?: "Mi ubicación"

                                val response = weatherApiService.getCurrentWeather(
                                    apiKey = "6e78dc2e4dce407aa92195839262405",  // ← temporal hardcodeado
                                    query = "${location.latitude},${location.longitude}"
                                )

                                _theme.value = determineTheme(
                                    tempC = response.current.temp_c,
                                    isDay = response.current.is_day,
                                    condition = response.current.condition.text
                                )

                                _uiState.value = WeatherUiState.Success(
                                    data = response,
                                    city = city
                                )

                            } catch (e: Exception) {
                                _uiState.value = WeatherUiState.Error(
                                    e.message ?: "Error al obtener el clima"
                                )
                            }
                        }
                    } else {
                        _uiState.value = WeatherUiState.Error(
                            "No se pudo obtener la ubicación. Activa el GPS."
                        )
                    }
                }.addOnFailureListener { e ->
                    _uiState.value = WeatherUiState.Error(
                        e.message ?: "Error de ubicación"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private fun determineTheme(tempC: Float, isDay: Int, condition: String): WeatherTheme {
        val cond = condition.lowercase()
        if (isDay == 0) return WeatherTheme.NIGHT
        if (cond.contains("rain") || cond.contains("drizzle") ||
            cond.contains("thunder") || cond.contains("lluvi") ||
            cond.contains("shower")
        ) return WeatherTheme.RAINY
        return when {
            tempC >= 28f -> WeatherTheme.HOT
            tempC >= 18f -> WeatherTheme.WARM
            else -> WeatherTheme.COLD
        }
    }
}