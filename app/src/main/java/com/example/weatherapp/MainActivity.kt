package com.example.weatherapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import com.example.weatherapp.data.FeedbackRequest
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
        // must run before setContent -- RetrofitClient needs the context to build its cache
        RetrofitClient.init(this)
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
    // last 5 cities successfully searched, newest first
    var recentSearches by remember { mutableStateOf(listOf<String>()) }

    // feedback form
    var currentCity by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(3) }
    var comment by remember { mutableStateOf("") }
    var feedbackResult by remember { mutableStateOf("") }
    var feedbackLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(all = 16.dp)
    ) {
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
                                    currentCity = weather.name
                                    // move this city to the front, drop duplicates, cap at 5
                                    recentSearches = (listOf(weather.name) + recentSearches.filter { it != weather.name }).take(5)
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

        OutlinedButton(
            onClick = {
                RetrofitClient.clearCache()
                Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Clear Cache")
        }

        if (recentSearches.isNotEmpty()) {
            Text("Recent Searches", fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp)
            ) {
                recentSearches.forEach { recentCity ->
                    AssistChip(
                        onClick = { city = recentCity },
                        label = { Text(recentCity) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))

        Text("How do you feel about today's weather?", fontSize = 16.sp)

        Slider(
            value = rating.toFloat(),
            onValueChange = { rating = it.toInt() },
            valueRange = 1f..5f,
            steps = 3 // 3 stops between 1 and 5 = 5 selectable whole numbers
        )
        Text("Rating: $rating/5")

        TextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Leave a comment about the weather...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Button(
            enabled = !feedbackLoading,
            onClick = {
                if (currentCity.isEmpty()) {
                    Toast.makeText(context, "Please fetch the weather for a city first", Toast.LENGTH_SHORT).show()
                } else if (comment.isBlank()) {
                    Toast.makeText(context, "Please leave a comment", Toast.LENGTH_SHORT).show()
                } else {
                    feedbackLoading = true
                    scope.launch {
                        try {
                            // Gson converts this to the JSON request body
                            val request = FeedbackRequest(city = currentCity, rating = rating, comment = comment.trim())
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.feedbackApiService.submitFeedback(request)
                            }
                            if (response.isSuccessful) {
                                feedbackResult = "Feedback submitted successfully!"
                                comment = ""
                                rating = 3
                            } else {
                                feedbackResult = "Failed to submit feedback (code ${response.code()}). Try again."
                            }
                        } catch (_: Exception) {
                            feedbackResult = "Error submitting feedback. Check your connection."
                        } finally {
                            feedbackLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(if (feedbackLoading) "Submitting..." else "Submit Feedback")
        }

        Text(feedbackResult, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
