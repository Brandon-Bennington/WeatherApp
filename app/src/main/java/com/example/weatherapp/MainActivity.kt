package com.example.weatherapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weatherapp.network.AppConstants
import com.example.weatherapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    WeatherScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherScreen() {
    var city by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("imperial") }
    var cityText by remember { mutableStateOf("City: --") }
    var temperatureText by remember { mutableStateOf("Temperature: --") }
    var descriptionText by remember { mutableStateOf("Description: --") }
    var windResult by remember { mutableStateOf("Wind: --") }
    var humidityResult by remember { mutableStateOf("Humidity: --") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().padding(all = 16.dp)) {
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter a city") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = selectedUnit == "imperial",
                onClick = { selectedUnit = "imperial" },
                label = { Text("Fahrenheit (°F)") }
            )
            FilterChip(
                selected = selectedUnit == "metric",
                onClick = { selectedUnit = "metric" },
                label = { Text("Celsius (°C)") }
            )
        }

        Button(
            enabled = !isLoading,
            onClick = {
                val trimmedCity = city.trim()
                if (trimmedCity.isEmpty()) {
                    Toast.makeText(context, "Please enter a city name", Toast.LENGTH_SHORT).show()
                } else {
                    isLoading = true
                    scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.weatherApiService.getWeather(
                                    city = trimmedCity,
                                    apiKey = AppConstants.API_KEY,
                                    units = selectedUnit
                                )
                            }
                            Log.d("WeatherApp", "Request URL: ${response.raw().request.url}")
                            Log.d("WeatherApp", "Request code: ${response.code()}")
                            if (response.isSuccessful) {
                                val weather = response.body()
                                if (weather != null) {
                                    val c = weather.name
                                    val unitSymbol = if (selectedUnit == "imperial") "°F" else "°C"
                                    val t = "${weather.main.temp} $unitSymbol"
                                    val d = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "N/A"

                                    cityText = "City: $c"
                                    temperatureText = "Temperature: $t"
                                    descriptionText = "Description: $d"
                                    windResult = "Wind: ${weather.wind.speed} ${if (selectedUnit == "imperial") "MPH" else "m/s"}"
                                    humidityResult = "Humidity: ${weather.main.humidity}%"
                                } else {
                                    Toast.makeText(context, "City not found. Check the name and try again.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val message = when (response.code()) {
                                    404 -> "City not found. Check the name and try again."
                                    401 -> "Invalid API key. Check AppConstants.API_KEY"
                                    else -> "Error: ${response.code()}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: UnknownHostException) {
                            Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
                        } catch (_: IOException) {
                            Toast.makeText(context, "Network error. Please check your connection and try again.", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error fetching weather: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                        finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(if(isLoading) "Loading..." else "Get Weather")
        }

        Text(cityText,fontSize = 20.sp, modifier = Modifier.padding(top = 24.dp))
        Text(temperatureText,fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(descriptionText, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(windResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(humidityResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
