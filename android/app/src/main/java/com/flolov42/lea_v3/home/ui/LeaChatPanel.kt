package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isLea: Boolean
)

@Composable
fun LeaChatPanel(
    messages: List<ChatMessage>,
    onSendCommand: (String) -> Unit,
    onMicPress: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .glassCard(alpha = 0.1f, cornerRadius = 24.dp)
            .padding(0.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HomeColors.Cyan)
                    .neonGlow(HomeColors.Cyan, 4.dp, alpha = 0.8f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Léa",
                style = MaterialTheme.typography.titleMedium,
                color = HomeColors.Cyan,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Domotique IA",
                style = MaterialTheme.typography.labelSmall,
                color = HomeColors.OnSurfaceDim
            )
        }

        HorizontalDivider(color = HomeColors.CardBorder.copy(alpha = 0.15f), thickness = 0.5.dp)

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    MessageBubble(msg)
                }
            }
        }

        HorizontalDivider(color = HomeColors.CardBorder.copy(alpha = 0.1f), thickness = 0.5.dp)

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text field
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(color = HomeColors.OnBackground, fontSize = 13.sp),
                cursorBrush = SolidColor(HomeColors.Cyan),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(HomeColors.CardGlass)
                    .border(
                        1.dp,
                        if (inputText.isNotEmpty()) HomeColors.Cyan.copy(alpha = 0.4f)
                        else HomeColors.CardBorder.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .then(if (inputText.isNotEmpty())
                        Modifier.neonGlow(HomeColors.Cyan, 6.dp, 20.dp, 0.2f)
                    else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) { inner ->
                if (inputText.isEmpty()) {
                    Text("Commande domotique…", color = HomeColors.OnSurfaceDim, fontSize = 12.sp)
                }
                inner()
            }

            // Send button
            AnimatedVisibility(visible = inputText.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HomeColors.Cyan)
                        .neonGlow(HomeColors.Cyan, 8.dp, alpha = 0.6f)
                        .clickableNoRipple {
                            if (inputText.isNotBlank()) {
                                onSendCommand(inputText.trim())
                                inputText = ""
                            }
                        }
                ) {
                    Icon(Icons.Filled.Send, null, tint = HomeColors.Background, modifier = Modifier.size(18.dp))
                }
            }

            // Mic button
            MicButton(isListening = isListening, onClick = onMicPress)
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isLea = msg.isLea
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isLea) Arrangement.Start else Arrangement.End
    ) {
        if (isLea) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(HomeColors.Cyan.copy(alpha = 0.2f))
                    .neonGlow(HomeColors.Cyan, 4.dp, alpha = 0.3f),
                contentAlignment = Alignment.Center
            ) {
                Text("L", color = HomeColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isLea) 4.dp else 16.dp,
                        topEnd   = 16.dp,
                        bottomEnd   = if (isLea) 16.dp else 4.dp,
                        bottomStart = 16.dp
                    )
                )
                .background(
                    if (isLea) HomeColors.Cyan.copy(alpha = 0.12f)
                    else HomeColors.Violet.copy(alpha = 0.15f)
                )
                .border(
                    0.5.dp,
                    if (isLea) HomeColors.Cyan.copy(alpha = 0.25f) else HomeColors.Violet.copy(alpha = 0.25f),
                    RoundedCornerShape(
                        topStart = if (isLea) 4.dp else 16.dp,
                        topEnd   = 16.dp,
                        bottomEnd   = if (isLea) 16.dp else 4.dp,
                        bottomStart = 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isLea) TypewriterText(msg.text)
            else Text(msg.text, color = HomeColors.OnBackground, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TypewriterText(fullText: String) {
    var visibleChars by remember(fullText) { mutableStateOf(0) }
    LaunchedEffect(fullText) {
        visibleChars = 0
        for (i in fullText.indices) {
            delay(18L)
            visibleChars = i + 1
        }
    }
    Text(
        text = fullText.take(visibleChars),
        color = HomeColors.Cyan,
        fontSize = 12.sp
    )
}

@Composable
private fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "ms"
    )
    val scale = if (isListening) pulseScale else 1f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .then(if (isListening) Modifier.neonGlow(HomeColors.Violet, 10.dp, alpha = 0.7f) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp * scale)
                .clip(CircleShape)
                .background(
                    if (isListening) HomeColors.Violet else HomeColors.CardGlass
                )
                .border(1.dp,
                    if (isListening) HomeColors.Violet.copy(alpha = 0.5f) else HomeColors.CardBorder.copy(alpha = 0.3f),
                    CircleShape)
                .clickableNoRipple { onClick() }
        ) {
            Icon(
                if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                null,
                tint = if (isListening) Color.White else HomeColors.OnSurfaceDim,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Tap without ripple
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
