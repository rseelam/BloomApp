// Create a new file: app/src/main/java/com/bloom/familytasks/ui/components/SuccessBanner.kt
package com.bloom.familytasks.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bloom.familytasks.R

// Banner type enum
enum class BannerType {
    Submitted,
    Success,
    Error
}

// Data class for banner state
data class BannerState(
    val isVisible: Boolean = false,
    val message: String = "",
    val type: BannerType = BannerType.Success
)

/**
 * Reusable Success Banner Component
 *
 * @param bannerState The current state of the banner
 * @param onDismiss Callback when banner is dismissed
 * @param modifier Optional modifier for positioning
 */
@Composable
fun SuccessBanner(
    bannerState: BannerState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bannerState.isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        BannerContent(
            type = bannerState.type,
            message = bannerState.message,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun BannerContent(
    type: BannerType,
    message: String,
    onDismiss: () -> Unit
) {
    // Define colors based on banner type
    val iconColor = when (type) {
        BannerType.Submitted -> Color(0xFFFFA726) // Orange
        BannerType.Success -> Color(0xFF4CAF50)    // Green
        BannerType.Error -> Color(0xFFD32F2F)      // Red
    }

    val textColor = when (type) {
        BannerType.Submitted -> Color(0xFF424242)  // Dark gray
        BannerType.Success -> Color(0xFF2E7D32)    // Dark green
        BannerType.Error -> Color(0xFFB71C1C)      // Dark red
    }

    val bannerIcon = when (type) {
        BannerType.Submitted -> Icons.Default.Send
        BannerType.Success -> Icons.Default.CheckCircle
        BannerType.Error -> Icons.Default.Error
    }

    val bannerTitle = when (type) {
        BannerType.Submitted -> "Submitted"
        BannerType.Success -> "Success!"
        BannerType.Error -> "Error"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = iconColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated icon with colored background circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = iconColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (type) {
                        BannerType.Submitted -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "loading")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(
                                        rotationZ = rotation
                                    ),
                                tint = iconColor
                            )
                        }
                        BannerType.Success -> {
                            val infiniteTransition = rememberInfiniteTransition(label = "success")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.9f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )

                            Icon(
                                bannerIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    ),
                                tint = iconColor
                            )
                        }
                        BannerType.Error -> {
                            Icon(
                                bannerIcon,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = iconColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        bannerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(20.dp),
                        tint = textColor.copy(alpha = 0.5f)
                    )
                }
            }

            // Show WF BLOOM logo on success
            if (type == BannerType.Success) {
//                Spacer(modifier = Modifier.height(12.dp))

//                Divider(
//                    color = iconColor.copy(alpha = 0.1f),
//                    thickness = 1.dp,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )

//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.Center,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bloom_logo),
                        contentDescription = "WF BLOOM",
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 8.dp),
                        contentScale = ContentScale.Fit
                    )
//                }
            }
        }
    }
}

/**
 * Helper class to manage banner state
 * Can be used with remember { BannerStateManager() } in your composables
 */
class BannerStateManager {
    private val _bannerState = mutableStateOf(BannerState())
    val bannerState: State<BannerState> = _bannerState

    fun showSubmitted(message: String) {
        _bannerState.value = BannerState(
            isVisible = true,
            message = message,
            type = BannerType.Submitted
        )
    }

    fun showSuccess(message: String) {
        _bannerState.value = BannerState(
            isVisible = true,
            message = message,
            type = BannerType.Success
        )
    }

    fun showError(message: String) {
        _bannerState.value = BannerState(
            isVisible = true,
            message = message,
            type = BannerType.Error
        )
    }

    fun hide() {
        _bannerState.value = _bannerState.value.copy(isVisible = false)
    }
}

// ============================================
// USAGE IN PARENT AND CHILD SCREENS
// ============================================

/**
 * Example usage in UpdatedParentScreen.kt:
 *
 * @Composable
 * fun UpdatedParentScreen(...) {
 *     // Initialize banner manager
 *     val bannerManager = remember { BannerStateManager() }
 *     val bannerState by bannerManager.bannerState
 *
 *     // Monitor API status
 *     LaunchedEffect(apiStatus) {
 *         when (apiStatus) {
 *             is ApiStatus.Success -> {
 *                 bannerManager.showSuccess("✅ Success! Chore has been sent to Johnny!")
 *             }
 *             is ApiStatus.Error -> {
 *                 bannerManager.showError("❌ Failed to send chore. Please try again.")
 *             }
 *             else -> {}
 *         }
 *     }
 *
 *     Scaffold(
 *         modifier = Modifier.clickable(
 *             interactionSource = remember { MutableInteractionSource() },
 *             indication = null
 *         ) {
 *             bannerManager.hide()
 *         }
 *     ) { paddingValues ->
 *         Box(
 *             modifier = Modifier
 *                 .fillMaxSize()
 *                 .padding(paddingValues)
 *         ) {
 *             // Your main content here
 *             Column { ... }
 *
 *             // Add the success banner
 *             SuccessBanner(
 *                 bannerState = bannerState,
 *                 onDismiss = { bannerManager.hide() },
 *                 modifier = Modifier.align(Alignment.TopCenter)
 *             )
 *         }
 *     }
 * }
 */

/**
 * Example usage in UpdatedChildScreen.kt:
 *
 * @Composable
 * fun UpdatedChildScreen(...) {
 *     // Initialize banner manager
 *     val bannerManager = remember { BannerStateManager() }
 *     val bannerState by bannerManager.bannerState
 *
 *     // Use in button clicks
 *     Button(
 *         onClick = {
 *             bannerManager.showSubmitted("📤 Sending message...")
 *             // Your logic here
 *         }
 *     )
 *
 *     Scaffold(...) { paddingValues ->
 *         Box(...) {
 *             // Your content
 *
 *             // Add the success banner
 *             SuccessBanner(
 *                 bannerState = bannerState,
 *                 onDismiss = { bannerManager.hide() },
 *                 modifier = Modifier.align(Alignment.TopCenter)
 *             )
 *         }
 *     }
 * }
 */