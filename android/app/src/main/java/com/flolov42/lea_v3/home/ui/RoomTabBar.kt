package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoomTabBar(
    rooms: List<String>,
    selectedRoom: String?,
    showFavorites: Boolean,
    onRoomSelected: (String?) -> Unit,
    onToggleFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // "Tous" pill
        RoomPill(
            label      = "Tous",
            isSelected = selectedRoom == null && !showFavorites,
            isAll      = true,
            onClick    = { onRoomSelected(null) }
        )
        // "Favoris" pill
        FavoritesPill(isSelected = showFavorites, onClick = onToggleFavorites)
        // Room pills
        rooms.forEach { room ->
            RoomPill(
                label      = room,
                isSelected = selectedRoom == room && !showFavorites,
                onClick    = { onRoomSelected(room) }
            )
        }
    }
}

@Composable
private fun FavoritesPill(isSelected: Boolean, onClick: () -> Unit) {
    val starColor = HomeColors.deviceColors["LOCK"] ?: HomeColors.Cyan

    val pulseAnim = rememberInfiniteTransition(label = "fav_pulse")
    val glowAlpha by pulseAnim.animateFloat(
        initialValue  = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            repeatMode = RepeatMode.Reverse
        ), label = "fga"
    )
    val bgColor     by animateColorAsState(if (isSelected) starColor.copy(alpha = 0.18f) else HomeColors.CardGlass, tween(250), label = "fb")
    val borderColor by animateColorAsState(if (isSelected) starColor.copy(alpha = glowAlpha) else HomeColors.CardBorder.copy(alpha = 0.15f), tween(250), label = "fbd")
    val textColor   by animateColorAsState(if (isSelected) starColor else HomeColors.OnSurfaceDim, tween(250), label = "ft")
    val scale       by animateFloatAsState(if (isSelected) 1.05f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fs")

    Box(
        modifier = Modifier
            .scale(scale)
            .then(if (isSelected) Modifier.neonGlow(starColor, glowRadius = 8.dp, alpha = 0.3f) else Modifier)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickableNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Star, null, tint = textColor, modifier = Modifier.size(13.dp))
            Text("Favoris", color = textColor, fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, letterSpacing = 0.3.sp)
        }
    }
}

@Composable
private fun RoomPill(
    label: String,
    isSelected: Boolean,
    isAll: Boolean = false,
    onClick: () -> Unit
) {
    val pulseAnim = rememberInfiniteTransition(label = "pill_pulse_$label")
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            repeatMode = RepeatMode.Reverse
        ), label = "ga"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) HomeColors.Cyan.copy(alpha = 0.18f) else HomeColors.CardGlass,
        animationSpec = tween(250), label = "pillBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) HomeColors.Cyan.copy(alpha = if (isSelected) glowAlpha else 0.22f)
                      else HomeColors.CardBorder.copy(alpha = 0.15f),
        animationSpec = tween(250), label = "pillBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) HomeColors.Cyan else HomeColors.OnSurfaceDim,
        animationSpec = tween(250), label = "pillText"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .then(if (isSelected) Modifier.neonGlow(HomeColors.Cyan, glowRadius = 8.dp, alpha = 0.3f) else Modifier)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickableNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isAll) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.3.sp
            )
        }
    }
}
