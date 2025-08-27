package com.bloom.familytasks.network

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // Your test environment URL
    private const val TEST_BASE_URL = "http://192.168.86.33:5678/webhook-test/"

    // Your production environment URL - CHANGE THIS TO YOUR ACTUAL PRODUCTION URL
    private const val PROD_BASE_URL = "http://192.168.86.33:5678/webhook/"

    const val WEBHOOK_ID = "aae97eb3-5737-4083-b752-36796abac305"

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

    // Test environment API service
    private val testApiService: N8nApiService = Retrofit.Builder()
        .baseUrl(TEST_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(N8nApiService::class.java)

    // Production environment API service
    private val prodApiService: N8nApiService = Retrofit.Builder()
        .baseUrl(PROD_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(N8nApiService::class.java)

    // Main API service that handles fallback
    val apiService: N8nApiService = object : N8nApiService {
        override suspend fun sendChatMessage(
            webhookId: String,
            request: ChatRequest
        ): Response<ResponseBody> {
            // Try test environment first
            try {
                val testResponse = testApiService.sendChatMessage(webhookId, request)

                // If test returns 404, try production
                if (testResponse.code() == 404) {
                    Log.w("NetworkModule", "Test environment returned 404, switching to production")
                    return prodApiService.sendChatMessage(webhookId, request)
                }

                // Otherwise return test response (whether success or other error)
                return testResponse

            } catch (e: Exception) {
                // If test environment is unreachable, try production
                Log.e("NetworkModule", "Test environment failed: ${e.message}, trying production")
                return prodApiService.sendChatMessage(webhookId, request)
            }
        }

        override suspend fun sendTaskWithImages(
            webhookId: String,
            chatInput: RequestBody,
            images: List<MultipartBody.Part>
        ): Response<ValidationResponse> {
            // Try test environment first
            try {
                val testResponse = testApiService.sendTaskWithImages(webhookId, chatInput, images)

                // If test returns 404, try production
                if (testResponse.code() == 404) {
                    Log.w("NetworkModule", "Test environment returned 404, switching to production")
                    return prodApiService.sendTaskWithImages(webhookId, chatInput, images)
                }

                // Otherwise return test response
                return testResponse

            } catch (e: Exception) {
                // If test environment is unreachable, try production
                Log.e("NetworkModule", "Test environment failed: ${e.message}, trying production")
                return prodApiService.sendTaskWithImages(webhookId, chatInput, images)
            }
        }
    }
}