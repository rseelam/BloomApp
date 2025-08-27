package com.bloom.familytasks.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Update this with your actual n8n webhook URL
    private const val BASE_URL = "http://192.168.86.33:5678/"

//    private const val BASE_URL = "http://10.2.250.33:5678/"
    const val WEBHOOK_ID = "aae97eb3-5737-4083-b752-36796abac305"

    const val WEBHOOK_ENDPOINT = "webhook-test/$WEBHOOK_ID"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val apiService: N8nApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(N8nApiService::class.java)
}