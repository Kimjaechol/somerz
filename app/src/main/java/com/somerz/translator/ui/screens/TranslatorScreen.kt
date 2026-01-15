package com.somerz.translator.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.somerz.translator.audio.AudioPlayer
import com.somerz.translator.audio.AudioRecorder
import com.somerz.translator.viewmodel.TranslationState
import com.somerz.translator.viewmodel.TranslatorViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object TranslatorRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TranslatorViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val audioRecorder = remember { AudioRecorder() }
    val audioPlayer = remember { AudioPlayer() }

    val transcription by viewModel.transcription.collectAsState()
    val audioOutput by viewModel.audioOutput.collectAsState()

    val languagePair = viewModel.getLanguagePair()

    // Play audio when received
    LaunchedEffect(audioOutput) {
        audioOutput?.let { audio ->
            audioPlayer.play(audio)
        }
    }

    // Clean up resources
    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.stopRecording()
            audioPlayer.release()
            viewModel.endTranslation()
        }
    }

    // Request permission on start
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live Translator",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language selector
            LanguageSelector(
                sourceLanguage = languagePair.sourceName,
                targetLanguage = languagePair.targetName,
                onSwapClick = { viewModel.swapLanguages() },
                enabled = viewModel.translationState == TranslationState.Idle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status card
            StatusCard(
                state = viewModel.translationState,
                transcription = transcription
            )

            Spacer(modifier = Modifier.weight(1f))

            // Main action button
            if (hasPermission) {
                TranslationButton(
                    state = viewModel.translationState,
                    onStartClick = {
                        viewModel.startTranslation()
                        scope.launch {
                            audioRecorder.startRecording().collect { audioData ->
                                viewModel.sendAudioData(audioData)
                            }
                        }
                    },
                    onStopClick = {
                        audioRecorder.stopRecording()
                        viewModel.endTranslation()
                    }
                )
            } else {
                PermissionDeniedMessage(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LanguageSelector(
    sourceLanguage: String,
    targetLanguage: String,
    onSwapClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source language
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "From",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sourceLanguage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Swap button
            IconButton(
                onClick = onSwapClick,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap languages",
                    modifier = Modifier.size(32.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Target language
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "To",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetLanguage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: TranslationState,
    transcription: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is TranslationState.Idle -> {
                    Text(
                        text = "Press the microphone button to start translating",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                is TranslationState.Connecting -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Connecting...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is TranslationState.Active -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PulsingIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (transcription.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = transcription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is TranslationState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingIndicator() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "pulse"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun TranslationButton(
    state: TranslationState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val isActive = state == TranslationState.Active || state == TranslationState.Connecting
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFFE63946) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "button_color"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(300),
        label = "button_scale"
    )

    FloatingActionButton(
        onClick = {
            if (isActive) onStopClick() else onStartClick()
        },
        modifier = Modifier
            .size(90.dp)
            .scale(buttonScale),
        containerColor = backgroundColor,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CallEnd else Icons.Default.Mic,
            contentDescription = if (isActive) "Stop translation" else "Start translation",
            modifier = Modifier.size(40.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun PermissionDeniedMessage(
    onRequestPermission: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Microphone permission required",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = onRequestPermission
        ) {
            Text("Grant Permission")
        }
    }
}
