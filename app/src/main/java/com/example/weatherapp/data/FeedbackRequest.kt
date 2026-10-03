package com.example.weatherapp.data

data class FeedbackRequest(
    val city: String,  //city user searched
    val rating: Int,   // rating user gave
    val comment: String // comment user gave
)
