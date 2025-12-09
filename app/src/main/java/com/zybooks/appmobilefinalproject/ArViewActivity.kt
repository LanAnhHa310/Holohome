package com.zybooks.appmobilefinalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.sceneview.ar.ARScene
import com.google.ar.core.Config
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import io.github.sceneview.ar.rememberARCameraStream

class ArViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A Box lets us layer the AR view and UI controls together
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // The AR Scene itself (stays full screen)
                ARScene(
                    sessionFeatures = setOf(),
                    sessionCameraConfig = null,
                    sessionConfiguration = { session, config ->
                        config.depthMode =
                            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                                Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
                        config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    },
                    planeRenderer = true,

                    onSessionCreated = {

                    },

                    onSessionResumed = {

                    },

                    onSessionPaused = {

                    },

                    onSessionUpdated = { session, updatedFrame ->

                    },

                    onSessionFailed = { exception ->

                    },
                    onTrackingFailureChanged = { trackingFailureReason ->

                    }
                )

                // Overlay a Save button at the bottom of the screen
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // --- Dialog state ---
                    val showDialog = remember { mutableStateOf(false) }
                    val layoutName = remember { mutableStateOf("") }
                    val roomType = remember { mutableStateOf("Living Room") }

                    // --- Button to open dialog ---
                    Button(onClick = { showDialog.value = true }) {
                        Text("Save Layout")
                    }

                    // --- Pop-up dialog ---
                    if (showDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showDialog.value = false },
                            confirmButton = {
                                Button(onClick = {
                                    if (layoutName.value.isNotBlank()) {
                                        saveNewLayout(layoutName.value, roomType.value)
                                        showDialog.value = false
                                    }
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                Button(onClick = { showDialog.value = false }) { Text("Cancel") }
                            },
                            title = { Text("Save Layout") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = layoutName.value,
                                        onValueChange = { layoutName.value = it },
                                        label = { Text("Layout name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Room type:")
                                    DropdownMenu(
                                        expanded = true,
                                        onDismissRequest = { /* nothing */ }
                                    ) {
                                        listOf("Living Room", "Bedroom", "Kitchen", "Office").forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = { roomType.value = type }
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                }
            }
        }

    }
    private fun saveNewLayout(name: String, roomType: String) {
        val newLayout = SavedLayoutEntity(
            id = "layout_${System.currentTimeMillis()}",
            name = name,
            roomType = roomType,
            dateCreated = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            val db = DatabaseProvider.getDatabase(this@ArViewActivity)
            val dao = db.savedLayoutDao()
            dao.insert(newLayout)

            runOnUiThread {
                Toast.makeText(
                    this@ArViewActivity,
                    "Layout saved as \"$name\"",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
