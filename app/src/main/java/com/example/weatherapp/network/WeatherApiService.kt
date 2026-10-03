package com.example.weatherapp.network

import com.example.weatherapp.data.WeatherResponse
import retrofit2.Response  // wraps the result and gives us isSuccessful + error codes
import retrofit2.http.GET  // annotation for HTTP GET request
import retrofit2.http.Query     // annotation for URL query parameters

// interface - a contract / blueprint
// we write WHAT calls we want, Retrofit generates HOW they will actually work
interface WeatherApiService {
    // @GET = make an HTTP GET request to: WEATHER_BASE_URL + "data/2.5/weather"
    // Full URL example: https://api.openweathermap.org/data/2.5/weather?q=London&appid=KEY&units=metric
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q")  city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Response<WeatherResponse>
}