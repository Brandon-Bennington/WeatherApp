package com.example.weatherapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // by lazy - create this only when it is first accessed
    val weatherApiService: WeatherApiService by lazy {
        Retrofit.Builder()
            // Retrofit prepends this to every @GET path in WeatherApiService
            .baseUrl(AppConstants.WEATHER_BASE_URL)
            // automatic JSON-to-data-class conversion when the API returns JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            // retrofit generates the real HTTP implementation
            // java - tells retrofit which interface to implement
            .create(WeatherApiService::class.java)
    }
}
