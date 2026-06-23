package com.flolov42.lea_v3.home.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape

fun Modifier.glassCard(
    alpha: Float = 0.09f,
    borderAlpha: Float = 0.22f,
    cornerRadius: Dp = 20.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = alpha * 1.5f),
            1f to Color.White.copy(alpha = alpha)
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f   to Color.White.copy(alpha = borderAlpha),
            0.5f to Color.White.copy(alpha = borderAlpha * 0.6f),
            1f   to Color.White.copy(alpha = borderAlpha * 0.3f)
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.neonGlow(
    color: Color,
    glowRadius: Dp = 18.dp,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.65f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = color.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(glowRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        val r = cornerRadius.toPx()
        val pad = glowRadius.toPx() * 0.3f
        canvas.nativeCanvas.drawRoundRect(
            -pad, -pad,
            size.width + pad, size.height + pad,
            r, r, paint
        )
    }
}

fun Modifier.shimmer(
    color: Color = Color.White,
    alpha: Float = 0.06f
): Modifier = this.drawWithCache {
    val brush = Brush.linearGradient(
        0f   to Color.Transparent,
        0.4f to color.copy(alpha = alpha),
        0.6f to color.copy(alpha = alpha * 1.5f),
        1f   to Color.Transparent,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end   = androidx.compose.ui.geometry.Offset(size.width, size.height)
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush = brush)
    }
}
