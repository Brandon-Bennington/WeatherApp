package com.example.weatherapp.network

import com.example.weatherapp.data.FeedbackRequest
import retrofit2.Response
import retrofit2.http.Body // send this object as the JSON request body
import retrofit2.http.POST // http POST method

interface FeedbackApiService {
    @POST("feedback") // tells retrofit make a post request
    suspend fun submitFeedback(
        @Body request: FeedbackRequest
        // Gson converts our Feedback object to JSON
        // and places it in HTTP request body
    ): Response<Unit>
}