package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.sceneview.ar.ARScene
import com.google.ar.core.Config
import io.github.sceneview.ar.rememberARCameraStream

class ArViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ARScene(
                // Configure AR session features
                sessionFeatures = setOf(),
                sessionCameraConfig = null,

                // Configure AR session settings
                sessionConfiguration = { session, config ->
                    // Enable depth if supported on the device
                    config.depthMode =
                        when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                            true -> Config.DepthMode.AUTOMATIC
                            else -> Config.DepthMode.DISABLED
                        }
                    config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                },

                // Enable plane detection visualization
                planeRenderer = true,

                // Session lifecycle callbacks
                onSessionCreated = { session ->
                    // Handle session creation
                },
                onSessionResumed = { session ->
                    // Handle session resume
                },
                onSessionPaused = { session ->
                    // Handle session pause
                },

                // Frame update callback
                onSessionUpdated = { session, updatedFrame ->
                    // Process AR frame updates
                },

                // Error handling
                onSessionFailed = { exception ->
                    // Handle ARCore session errors
                },

                // Track camera tracking state changes
                onTrackingFailureChanged = { trackingFailureReason ->
                    // Handle tracking failures
                }
            )
        }
    }
}
