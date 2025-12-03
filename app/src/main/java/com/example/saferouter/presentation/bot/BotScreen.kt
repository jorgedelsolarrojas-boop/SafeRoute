package com.example.saferouter.presentation.bot

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saferouter.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun BotScreen(
    navigateBack: () -> Unit,
    currentLat: Double? = null,
    currentLng: Double? = null,
    viewModel: BotViewModel = koinViewModel() // 🔥 USAR KOIN VIEWMODEL
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var userInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Launcher para reconocimiento de voz
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        results?.get(0)?.let { speechText ->
            userInput = speechText
        }
    }

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryBlueLight, BackgroundWhite),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = navigateBack,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "SRBot - Asistente de Seguridad",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Chat Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }

                if (uiState is BotUiState.Loading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .wrapContentWidth(),
                                elevation = 2.dp,
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = PrimaryBlueLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Analizando...",
                                        color = TextPrimary,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Input Button
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(
                                    RecognizerIntent.EXTRA_PROMPT,
                                    "Habla tu consulta de seguridad"
                                )
                            }
                            speechLauncher.launch(intent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            tint = PrimaryBlue
                        )
                    }

                    // Text Input
                    BasicTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                if (userInput.isEmpty()) {
                                    Text(
                                        text = "Escribe tu consulta de seguridad...",
                                        color = TextSecondary
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                viewModel.sendMessage(userInput, currentLat, currentLng)
                                userInput = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = userInput.isNotBlank() && uiState !is BotUiState.Loading
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (userInput.isNotBlank()) PrimaryBlue else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 280.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = if (message.isUser) PrimaryBlue else PrimaryBlueLight
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Color.White else TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}