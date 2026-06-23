package com.flolov42.lea_v3.home.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.*
import kotlin.random.Random

data class Particle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
    val lifetime: Float   // 0..1
)

@Composable
fun DiscoveryParticles(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return

    val particles = remember {
        List(30) { randomParticle() }.toMutableStateList()
    }

    val tick = rememberInfiniteTransition(label = "particles")
    val progress by tick.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(40, easing = LinearEasing)),
        label = "tick"
    )

    LaunchedEffect(progress) {
        val iter = particles.listIterator()
        while (iter.hasNext()) {
            val p = iter.next()
            val newLifetime = p.lifetime + 0.008f
            if (newLifetime >= 1f) {
                iter.set(randomParticle())
            } else {
                iter.set(p.copy(
                    x = p.x + p.vx,
                    y = p.y + p.vy,
                    lifetime = newLifetime
                ))
            }
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val alpha = sin(p.lifetime * Math.PI.toFloat()) * p.alpha
            if (alpha > 0.02f) {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.radius * (1f - p.lifetime * 0.5f),
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }
    }
}

private fun randomParticle(): Particle {
    val colors = listOf(HomeColors.Cyan, HomeColors.Violet, HomeColors.Blue,
                        HomeColors.Cyan.copy(alpha = 0.7f))
    val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
    val speed = Random.nextFloat() * 0.003f + 0.001f
    return Particle(
        x        = Random.nextFloat(),
        y        = Random.nextFloat(),
        vx       = cos(angle) * speed,
        vy       = sin(angle) * speed,
        radius   = Random.nextFloat() * 4f + 1.5f,
        color    = colors.random(),
        alpha    = Random.nextFloat() * 0.6f + 0.2f,
        lifetime = Random.nextFloat()
    )
}

@Composable
fun PulsingLoader(
    color: Color = HomeColors.Cyan,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "pulse_loader")
    val scale by anim.animateFloat(
        1f, 1.6f,
        infiniteRepeatable(tween(800, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            RepeatMode.Reverse),
        label = "pl"
    )
    val alpha by anim.animateFloat(
        0.8f, 0.2f,
        infiniteRepeatable(tween(800, easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)),
            RepeatMode.Reverse),
        label = "pa"
    )
    Canvas(modifier = modifier) {
        drawCircle(color = color.copy(alpha = alpha * 0.3f), radius = (size.minDimension / 2) * scale)
        drawCircle(color = color.copy(alpha = alpha), radius = size.minDimension / 4)
    }
}
