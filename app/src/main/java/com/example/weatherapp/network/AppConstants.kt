package com.example.weatherapp.network

object AppConstants {
    const val WEATHER_BASE_URL = "https://api.openweathermap.org/"

    const val API_KEY = "38fc189b07128863219bb54833e241ac"

    const val UNITS = "imperial"

    // must end with "/" -- FeedbackApiService appends "feedback" to this
    const val FEEDBACK_BASE_URL = "https://mock.apidog.com/m1/1386452-1392954-default/"
}
