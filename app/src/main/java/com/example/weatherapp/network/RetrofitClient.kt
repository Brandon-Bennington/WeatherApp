package com.example.weatherapp.network

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

// automatically retries failed network calls up to maxRetries
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null

        repeat(maxRetries) {
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) { // network failure (no internet, timeout...)
                lastException = e
            }
        }
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
}

object RetrofitClient {
    private lateinit var appContext: Context
    private lateinit var cache: Cache

    // call once from MainActivity.onCreate, before setContent
    fun init(context: Context) {
        // the global app context, not tied to any specific activity -- prevents memory leaks
        appContext = context.applicationContext
    }

    fun clearCache() {
        if (::cache.isInitialized) {
            cache.evictAll() // OkHttp API that deletes every cached response
        }
    }

    // by lazy - create this only when it is first accessed
    val weatherApiService: WeatherApiService by lazy {
        // app's private cache directory, 5MB max disk space
        cache = Cache(File(appContext.cacheDir, "http_cache"), 5L * 1024 * 1024)

        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                // cache responses for 300 sec (5 min)
                chain.proceed(chain.request()).newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
            }
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            // Retrofit prepends this to every @GET path in WeatherApiService
            .baseUrl(AppConstants.WEATHER_BASE_URL)
            // use our OkHttp client so the cache and retry actually apply
            .client(okHttpClient)
            // automatic JSON-to-data-class conversion when the API returns JSON
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            // retrofit generates the real HTTP implementation
            // java - tells retrofit which interface to implement
            .create(WeatherApiService::class.java)
    }

    val feedbackApiService: FeedbackApiService by lazy {
        // no cache -- POST requests should not be cached
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(AppConstants.FEEDBACK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeedbackApiService::class.java)
    }
}
