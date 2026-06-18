package com.fieldmark.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldmark.app.ui.theme.FieldBlue40
import com.fieldmark.app.ui.theme.FieldBlue80
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompassRose(heading: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(size.width, size.height) / 2f - 12f
            val ringColor = FieldBlue80.copy(alpha = 0.35f)
            drawCircle(ringColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 4f))
            drawCircle(ringColor.copy(alpha = 0.2f), radius = radius * 0.7f, center = Offset(cx, cy), style = Stroke(width = 2f))
            drawCircle(ringColor.copy(alpha = 0.2f), radius = radius * 0.4f, center = Offset(cx, cy), style = Stroke(width = 2f))

            for (deg in 0 until 360 step 30) {
                rotate(degrees = deg.toFloat(), pivot = Offset(cx, cy)) {
                    val isMajor = deg % 90 == 0
                    val len = if (isMajor) radius * 0.18f else radius * 0.08f
                    drawLine(
                        Color(0xFF8AA4D8),
                        Offset(cx, cy - radius + 2f),
                        Offset(cx, cy - radius + 2f + len),
                        strokeWidth = if (isMajor) 5f else 3f
                    )
                }
            }
            rotate(degrees = -heading, pivot = Offset(cx, cy)) {
                val cardinals = listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270)
                val intercardinals = listOf("NE" to 45, "SE" to 135, "SW" to 225, "NW" to 315)
                cardinals.forEach { (label, deg) ->
                    rotate(degrees = deg.toFloat(), pivot = Offset(cx, cy)) {
                        val layout = measurer.measure(
                            AnnotatedString(label),
                            TextStyle(color = if (label == "N") Color(0xFFD32F2F) else Color(0xFF1A2540),
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label, cx - layout.size.width / 2f,
                            cy - radius * 0.75f,
                            android.graphics.Paint().apply {
                                color = if (label == "N") android.graphics.Color.RED
                                else android.graphics.Color.parseColor("#1A2540")
                                textSize = layout.size.height.toFloat()
                                isAntiAlias = true
                                isFakeBoldText = true
                            }
                        )
                    }
                }
                intercardinals.forEach { (label, deg) ->
                    rotate(degrees = deg.toFloat(), pivot = Offset(cx, cy)) {
                        val layout = measurer.measure(
                            AnnotatedString(label),
                            TextStyle(color = Color(0xFF6B7A99), fontSize = 12.sp)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label, cx - layout.size.width / 2f,
                            cy - radius * 0.6f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#6B7A99")
                                textSize = layout.size.height.toFloat()
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

            // Pointer (fixed, points to N which represents current heading)
            val pointerPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - radius * 0.55f)
                lineTo(cx - 14f, cy)
                lineTo(cx + 14f, cy)
                close()
            }
            drawPath(pointerPath, Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFF8E1A1A))))
            val tailPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy + radius * 0.25f)
                lineTo(cx - 10f, cy)
                lineTo(cx + 10f, cy)
                close()
            }
            drawPath(tailPath, Color(0xFF1A2540))
            drawCircle(Color.White, radius = 6f, center = Offset(cx, cy))
            drawCircle(FieldBlue40, radius = 3f, center = Offset(cx, cy))
        }
    }
}
