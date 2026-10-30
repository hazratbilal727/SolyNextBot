package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowEmerald
import com.example.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FuturisticAiOrb(
    state: AssistantState,
    audioLevel: Float, // 0f to 1f normalized
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Continuous rotation for holographic rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantState.THINKING) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // Reverse rotation for counter ring
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRingRotation"
    )

    // Breathing pulse for idle & speaking
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AssistantState.SPEAKING) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    // State-based primary colors
    val (primaryGlow, accentGlow) = when (state) {
        AssistantState.IDLE -> Pair(CyberCyan, ElectricBlue)
        AssistantState.LISTENING -> Pair(CyberCyan, GlowEmerald)
        AssistantState.THINKING -> Pair(NeonViolet, ElectricBlue)
        AssistantState.SPEAKING -> Pair(CyberCyan, NeonViolet)
        AssistantState.ERROR -> Pair(AlertCrimson, Color(0xFFFF5555))
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("ai_orb_button")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = size / 2),
                onClick = onOrbClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.48f

            // Dynamic expansion based on real audio level or state animation
            val audioReactiveMultiplier = when (state) {
                AssistantState.LISTENING -> 1f + (audioLevel * 0.45f)
                AssistantState.SPEAKING -> breathingPulse * 1.05f
                AssistantState.THINKING -> 1.0f
                AssistantState.IDLE -> breathingPulse
                AssistantState.ERROR -> 1.0f
            }

            val currentCoreRadius = baseRadius * audioReactiveMultiplier

            // 1. Outermost Ambient Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (state == AssistantState.LISTENING) 0.35f else 0.18f),
                        accentGlow.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentCoreRadius * 1.8f
                ),
                radius = currentCoreRadius * 1.8f,
                center = center
            )

            // 2. Audio Waveform Echo Rings when Listening / Speaking
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val echoCount = 3
                for (i in 1..echoCount) {
                    val echoRadius = currentCoreRadius + (i * 18f * (if (state == AssistantState.LISTENING) audioLevel.coerceAtLeast(0.3f) else breathingPulse))
                    val alpha = (0.4f / i) * (if (state == AssistantState.LISTENING) audioLevel.coerceAtLeast(0.4f) else 0.6f)
                    drawCircle(
                        color = primaryGlow.copy(alpha = alpha),
                        radius = echoRadius,
                        center = center,
                        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
                    )
                }
            }

            // 3. Futuristic Outer Orbital Ring (Holographic Rotating Segments)
            val outerRingRadius = baseRadius * 1.35f
            val segments = 4
            for (i in 0 until segments) {
                val startAngle = rotationAngle + (i * (360f / segments))
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryGlow.copy(alpha = 0.8f), accentGlow.copy(alpha = 0.2f), primaryGlow.copy(alpha = 0.8f)),
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRingRadius, center.y - outerRingRadius),
                    size = androidx.compose.ui.geometry.Size(outerRingRadius * 2, outerRingRadius * 2),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // 4. Counter-rotating inner dashed ring
            val innerRingRadius = baseRadius * 1.15f
            drawCircle(
                color = accentGlow.copy(alpha = 0.45f),
                radius = innerRingRadius,
                center = center,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 16f), counterRotation)
                )
            )

            // 5. Solid Core Sphere with Holographic Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryGlow.copy(alpha = 0.9f),
                        accentGlow.copy(alpha = 0.75f),
                        Color(0xFF070B18)
                    ),
                    center = Offset(center.x - (currentCoreRadius * 0.2f), center.y - (currentCoreRadius * 0.2f)),
                    radius = currentCoreRadius
                ),
                radius = currentCoreRadius,
                center = center
            )

            // 6. Neural Node Satellites (Orbiting Sparks)
            val satelliteCount = 6
            for (i in 0 until satelliteCount) {
                val angleRad = Math.toRadians((rotationAngle + (i * (360f / satelliteCount))).toDouble())
                val satDist = currentCoreRadius * (1.1f + (0.15f * sin((rotationAngle * 0.05f) + i).toFloat()))
                val satX = center.x + (satDist * cos(angleRad)).toFloat()
                val satY = center.y + (satDist * sin(angleRad)).toFloat()

                drawCircle(
                    color = if (i % 2 == 0) primaryGlow else accentGlow,
                    radius = if (state == AssistantState.LISTENING) 4f + (audioLevel * 3f) else 3.5f,
                    center = Offset(satX, satY)
                )
            }
        }
    }
}
