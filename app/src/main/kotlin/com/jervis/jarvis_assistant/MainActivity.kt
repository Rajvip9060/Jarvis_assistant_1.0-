package com.jervis.jarvis_assistant

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jervis.jarvis_assistant.data.PrefsManager
import com.jervis.jarvis_assistant.data.api.OpenRouterApi
import com.jervis.jarvis_assistant.logic.LocalCommandHandler
import com.jervis.jarvis_assistant.logic.VoiceAssistantManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var assistantManager: VoiceAssistantManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PrefsManager(this)

        // Permissions Request (Audio, Camera, Call, SMS)
        requestPermissions(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        ), 101)

        // API Setup using Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(OpenRouterApi::class.java)

        setContent {
            val navController = rememberNavController()
            var statusText by remember { mutableStateOf("System Online, Sir.") }

            // Assistant Manager ko initialize karna
            // Note: API Key dynamic load hogi prefs se
            val currentApiKey = remember { mutableStateOf(prefs.getApiKey()) }
            
            assistantManager = remember(currentApiKey.value) {
                VoiceAssistantManager(
                    this, api, currentApiKey.value, LocalCommandHandler(this)
                ) { statusText = it }
            }

            NavHost(navController = navController, startDestination = "home") {
                // SCREEN 1: Main Assistant UI
                composable("home") {
                    JarvisHomeScreen(
                        status = statusText,
                        onMicClick = { assistantManager.startListening() },
                        onSettingsClick = { navController.navigate("settings") }
                    )
                }

                // SCREEN 2: API Settings UI
                composable("settings") {
                    SettingsScreen(
                        currentKey = currentApiKey.value,
                        onSave = { newKey ->
                            prefs.saveApiKey(newKey)
                            currentApiKey.value = newKey // Dynamic update
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisHomeScreen(status: String, onMicClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)), // Deep dark background
        contentAlignment = Alignment.Center
    ) {
        // Top Settings Button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Cyan)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "JARVIS",
                color = Color.Cyan,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 10.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = status,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Large Cyan Mic Button
            Surface(
                onClick = onMicClick,
                shape = CircleShape,
                color = Color(0xFF00E5FF),
                shadowElevation = 20.dp,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak",
                        tint = Color.Black,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(currentKey: String, onSave: (String) -> Unit) {
    var textState by remember { mutableStateOf(currentKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Configuration", color = Color.Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            label = { Text("OpenRouter API Key", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSave(textState) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
        ) {
            Text("UPDATE SYSTEM KEY", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "Note: Get your key from openrouter.ai",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}