package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpaceGroteskFamily
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedFlagStrip(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ceylon_flag_strip")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val shimmerOffset by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0A1428),
                        Color(0xFF1A0F24),
                        Color(0xFF0B2239),
                        Color(0xFF091426)
                    )
                )
            )
            .testTag("animated_flag_strip"),
        contentAlignment = Alignment.Center
    ) {
        // Animated bioluminescent sweep across the slim strip
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * shimmerOffset
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x2A22D3EE),
                        Color(0x28F5B700),
                        Color.Transparent
                    ),
                    startX = (cx - w * 0.25f),
                    endX = (cx + w * 0.25f)
                )
            )
            // Subtle bottom laser hairline
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF00534E),
                        Color(0xFFF5B700),
                        Color(0xFF22D3EE),
                        Color(0xFF8D153A)
                    )
                ),
                start = Offset(0f, h - 1.dp.toPx()),
                end = Offset(w, h - 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            WavingSriLankaFlag(
                phase = wavePhase,
                modifier = Modifier
                    .width(44.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.6.dp, Color(0x88F5B700), RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "CEYLONESE FUTURISM",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp
                ),
                color = Color(0xFFFDE68A)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22D3EE))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x2222D3EE)
            ) {
                Text(
                    text = "ශ්‍රී ලංකා",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFA5F3FC),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun WavingSriLankaFlag(
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val slices = 16
        val sliceW = w / slices
        val waveAmp = h * 0.055f

        val goldColor = Color(0xFFF5B700)
        val greenColor = Color(0xFF00534E)
        val saffronColor = Color(0xFFEB7400)
        val maroonColor = Color(0xFF8D153A)

        // Draw waving vertical slices for realistic cloth ripple
        for (i in 0 until slices) {
            val u0 = i.toFloat() / slices
            val dy = sin(phase + u0 * 5.2f) * waveAmp * (0.25f + 0.75f * u0)
            val x = i * sliceW

            val baseColor = when {
                u0 < 0.055f || u0 > 0.945f -> goldColor
                u0 < 0.21f -> greenColor
                u0 < 0.365f -> saffronColor
                u0 < 0.415f -> goldColor
                else -> maroonColor
            }

            // Gold top and bottom frame on every slice
            val topFrame = h * 0.11f
            drawRect(
                color = goldColor,
                topLeft = Offset(x, dy.coerceAtLeast(0f)),
                size = Size(sliceW + 0.8f, h)
            )
            drawRect(
                color = baseColor,
                topLeft = Offset(x, topFrame + dy * 0.7f),
                size = Size(sliceW + 0.8f, h - topFrame * 2f)
            )

            // Cloth highlight / shadow ripple
            val shade = sin(phase + u0 * 5.2f)
            if (shade > 0f) {
                drawRect(
                    color = Color.White.copy(alpha = 0.10f * shade),
                    topLeft = Offset(x, dy.coerceAtLeast(0f)),
                    size = Size(sliceW + 0.8f, h)
                )
            } else {
                drawRect(
                    color = Color.Black.copy(alpha = 0.12f * (-shade)),
                    topLeft = Offset(x, dy.coerceAtLeast(0f)),
                    size = Size(sliceW + 0.8f, h)
                )
            }
        }

        // Maroon field bounds for the 4 Bo leaves and Lion emblem
        val maroonLeft = w * 0.42f
        val maroonRight = w * 0.94f
        val maroonTop = h * 0.13f
        val maroonBottom = h * 0.87f
        val centerWave = sin(phase + 3.4f) * waveAmp * 0.55f

        drawBoLeaf(Offset(maroonLeft + 2.5f, maroonTop + 2.5f + centerWave), goldColor, 2.2f)
        drawBoLeaf(Offset(maroonRight - 2.5f, maroonTop + 2.5f + centerWave), goldColor, 2.2f)
        drawBoLeaf(Offset(maroonLeft + 2.5f, maroonBottom - 2.5f + centerWave), goldColor, 2.2f)
        drawBoLeaf(Offset(maroonRight - 2.5f, maroonBottom - 2.5f + centerWave), goldColor, 2.2f)

        // Stylized Golden Lion & Sword silhouette
        val lx = (maroonLeft + maroonRight) * 0.52f
        val ly = (maroonTop + maroonBottom) * 0.53f + centerWave
        val lionW = (maroonRight - maroonLeft) * 0.48f
        val lionH = (maroonBottom - maroonTop) * 0.52f

        val bodyPath = Path().apply {
            moveTo(lx - lionW * 0.36f, ly + lionH * 0.30f)
            lineTo(lx - lionW * 0.16f, ly - lionH * 0.10f)
            lineTo(lx + lionW * 0.25f, ly - lionH * 0.14f)
            lineTo(lx + lionW * 0.34f, ly + lionH * 0.28f)
            lineTo(lx + lionW * 0.15f, ly + lionH * 0.28f)
            lineTo(lx + lionW * 0.10f, ly + lionH * 0.08f)
            lineTo(lx - lionW * 0.12f, ly + lionH * 0.10f)
            lineTo(lx - lionW * 0.22f, ly + lionH * 0.30f)
            close()
        }
        drawPath(bodyPath, color = goldColor)

        // Lion head & mane
        drawCircle(
            color = goldColor,
            radius = lionH * 0.22f,
            center = Offset(lx - lionW * 0.18f, ly - lionH * 0.22f)
        )

        // Raised sword in right forepaw
        drawLine(
            color = goldColor,
            start = Offset(lx - lionW * 0.34f, ly - lionH * 0.05f),
            end = Offset(lx - lionW * 0.38f, ly - lionH * 0.52f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Curved tail
        drawLine(
            color = goldColor,
            start = Offset(lx + lionW * 0.28f, ly - lionH * 0.08f),
            end = Offset(lx + lionW * 0.42f, ly - lionH * 0.36f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBoLeaf(center: Offset, color: Color, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.75f, center.y + radius * 0.2f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.75f, center.y + radius * 0.2f)
        close()
    }
    drawPath(path, color = color)
}
