// app/src/main/java/com/bloom/familytasks/MainActivity.kt
package com.bloom.familytasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bloom.familytasks.navigation.FamilyTasksNavigation
import com.bloom.familytasks.network.NetworkModule
import com.bloom.familytasks.ui.theme.FamilyTasksTheme
import com.bloom.familytasks.viewmodel.EnhancedTaskViewModel

class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            when (permission) {
                Manifest.permission.CAMERA -> {
                    if (isGranted) {
                        Log.d("MainActivity", "Camera permission granted")
                    } else {
                        Log.d("MainActivity", "Camera permission denied")
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    if (isGranted) {
                        Log.d("MainActivity", "Audio recording permission granted")
                    } else {
                        Log.d("MainActivity", "Audio recording permission denied")
                    }
                }
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES -> {
                    if (isGranted) {
                        Log.d("MainActivity", "Storage permission granted")
                    } else {
                        Log.d("MainActivity", "Storage permission denied")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NetworkModule with application context
        NetworkModule.init(applicationContext)

        // Check and request permissions
        checkAndRequestPermissions()

        setContent {
            FamilyTasksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val taskViewModel: EnhancedTaskViewModel = viewModel()

                    // Check for audio permission before enabling voice features
                    val hasAudioPermission = remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val hasCameraPermission = remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    // Pass permission state to navigation if needed
                    FamilyTasksNavigation(taskViewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Check audio recording permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // Check storage permissions based on Android version
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ uses photo picker, no permission needed for images
                // But we might still need READ_MEDIA_IMAGES for some operations
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-12, check for READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else -> {
                // Android 9 and below
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        // Request all needed permissions at once
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}