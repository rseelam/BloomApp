package com.bloom.familytasks.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.bloom.familytasks.ui.screens.SimpleSettingsScreen

object NetworkModule {
    // Your test environment URL
    private const val TEST_BASE_URL = "http://192.168.86.33:5678/webhook-test/"

    // Default production URL (used if not set in preferences)
    private const val DEFAULT_PROD_BASE_URL = "http://192.168.86.33:5678/webhook/"

    const val WEBHOOK_ID = "aae97eb3-5737-4083-b752-36796abac305"

    private const val PREFS_NAME = "n8n_settings"
    private const val PREF_PROD_URL = "production_url"

    private lateinit var appContext: Context
    private lateinit var sharedPrefs: SharedPreferences

    // Initialize with application context (call this from your Application class or MainActivity)
    fun init(context: Context) {
        appContext = context.applicationContext
        sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Composable
    fun HomeScreen(
        onParentClick: () -> Unit,
        onChildClick: () -> Unit,
        onSettingsClick: () -> Unit = {} // Add this parameter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... existing UI ...

            // Add Settings button at the bottom
            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onSettingsClick
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("N8N Settings")
            }
        }
    }

    // Get production URL from preferences or use default
    fun getProductionUrl(): String {
        if (!::sharedPrefs.isInitialized) {
            Log.w("NetworkModule", "SharedPreferences not initialized, using default production URL")
            return DEFAULT_PROD_BASE_URL
        }
        return sharedPrefs.getString(PREF_PROD_URL, DEFAULT_PROD_BASE_URL) ?: DEFAULT_PROD_BASE_URL
    }

    // Set production URL in preferences
    fun setProductionUrl(url: String) {
        if (!::sharedPrefs.isInitialized) {
            Log.e("NetworkModule", "Cannot set production URL - SharedPreferences not initialized")
            return
        }
        sharedPrefs.edit().putString(PREF_PROD_URL, url).apply()
        Log.i("NetworkModule", "Production URL updated to: $url")
    }

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

    // Function to create production API service with current URL
    private fun createProdApiService(): N8nApiService {
        val prodUrl = getProductionUrl()
        Log.d("NetworkModule", "Creating production API service with URL: $prodUrl")

        return Retrofit.Builder()
            .baseUrl(prodUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(N8nApiService::class.java)
    }

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
                    val prodApiService = createProdApiService()
                    return prodApiService.sendChatMessage(webhookId, request)
                }

                // Otherwise return test response (whether success or other error)
                return testResponse

            } catch (e: Exception) {
                // If test environment is unreachable, try production
                Log.e("NetworkModule", "Test environment failed: ${e.message}, trying production")
                val prodApiService = createProdApiService()
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
                    val prodApiService = createProdApiService()
                    return prodApiService.sendTaskWithImages(webhookId, chatInput, images)
                }

                // Otherwise return test response
                return testResponse

            } catch (e: Exception) {
                // If test environment is unreachable, try production
                Log.e("NetworkModule", "Test environment failed: ${e.message}, trying production")
                val prodApiService = createProdApiService()
                return prodApiService.sendTaskWithImages(webhookId, chatInput, images)
            }
        }
    }
}