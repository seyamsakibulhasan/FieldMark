package com.fieldmark.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.fieldmark.app.annotation.Annotation
import com.fieldmark.app.capture.PhotoMetadata
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class AnnotationTool {
    None, Freehand, Arrow, Line, Rect, Circle, Measurement, Text
}

data class ToolOptions(
    val color: Color = Color(0xFFFF1744),
    val strokeWidth: Float = 5f
)

data class EditorState(
    val items: List<Annotation> = emptyList(),
    val draft: Annotation? = null,
    val measurementCalibration: Float = 12f,
    val measurementUnit: String = "cm"
)

@Composable
fun AnnotationCanvas(
    state: EditorState,
    tool: AnnotationTool,
    options: ToolOptions,
    modifier: Modifier = Modifier,
    background: Bitmap? = null,
    metadata: PhotoMetadata? = null,
    onCommit: (List<Annotation>) -> Unit,
    onTextTap: (Offset) -> Unit = {}
) {
    var startPt by remember { mutableStateOf<Offset?>(null) }
    var freehandPts by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var currentEnd by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val drawItems = tool == AnnotationTool.None
    val img = background?.asImageBitmap()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(tool, options, state.measurementCalibration, state.measurementUnit) {
                if (tool == AnnotationTool.None) return@pointerInput
                if (tool == AnnotationTool.Text) {
                    detectTapGestures { offset -> onTextTap(offset) }
                    return@pointerInput
                }
                detectDragGestures(
                    onDragStart = { offset ->
                        startPt = offset
                        currentEnd = offset
                        if (tool == AnnotationTool.Freehand) freehandPts = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        currentEnd = change.position
                        if (tool == AnnotationTool.Freehand) freehandPts = freehandPts + change.position
                        change.consume()
                    },
                    onDragEnd = {
                        val s = startPt
                        if (s != null) {
                            val e: Offset = currentEnd ?: s
                            val newAnno: Annotation? = when (tool) {
                                AnnotationTool.Freehand -> if (freehandPts.size > 1)
                                    Annotation.Freehand(freehandPts, options.color, options.strokeWidth) else null
                                AnnotationTool.Arrow -> Annotation.Arrow(s, e, options.color, options.strokeWidth)
                                AnnotationTool.Line -> Annotation.Line(s, e, options.color, options.strokeWidth)
                                AnnotationTool.Rect -> Annotation.Rect(
                                    Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
                                    Offset(maxOf(s.x, e.x), maxOf(s.y, e.y)),
                                    options.color, options.strokeWidth
                                )
                                AnnotationTool.Circle -> Annotation.Circle(
                                    s, hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()).toFloat(),
                                    options.color, options.strokeWidth
                                )
                                AnnotationTool.Measurement -> {
                                    val px = hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()).toFloat()
                                    val real = if (state.measurementCalibration > 0f) px / state.measurementCalibration else 0f
                                    Annotation.Measurement(
                                        s, e, String.format("%.2f %s", real, state.measurementUnit),
                                        state.measurementCalibration, state.measurementUnit,
                                        options.color, options.strokeWidth
                                    )
                                }
                                else -> null
                            }
                            if (newAnno != null) onCommit(state.items + newAnno)
                        }
                        startPt = null
                        currentEnd = null
                        freehandPts = emptyList()
                    },
                    onDragCancel = {
                        startPt = null
                        currentEnd = null
                        freehandPts = emptyList()
                    }
                )
            }
    ) {
        var imgLeft = 0f; var imgTop = 0f; var imgW = 0f; var imgH = 0f
        if (img != null) {
            val scale = min(size.width / img.width, size.height / img.height)
            imgW = img.width * scale
            imgH = img.height * scale
            imgLeft = (size.width - imgW) / 2f
            imgTop = (size.height - imgH) / 2f
            drawImage(
                image = img,
                dstSize = IntSize(imgW.toInt(), imgH.toInt()),
                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt())
            )
            if (metadata != null) {
                drawInfoStamp(metadata, imgLeft, imgTop, imgW, imgH, textMeasurer)
            }
        }
        if (drawItems) {
            state.items.forEach { drawAnnotation(it, textMeasurer) }
        }
        val s = startPt
        val e = currentEnd
        if (s != null && e != null) {
            val draftAnno: Annotation? = when (tool) {
                AnnotationTool.Freehand -> if (freehandPts.size > 1) Annotation.Freehand(freehandPts, options.color, options.strokeWidth) else null
                AnnotationTool.Arrow -> Annotation.Arrow(s, e, options.color, options.strokeWidth)
                AnnotationTool.Line -> Annotation.Line(s, e, options.color, options.strokeWidth)
                AnnotationTool.Rect -> Annotation.Rect(
                    Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
                    Offset(maxOf(s.x, e.x), maxOf(s.y, e.y)),
                    options.color, options.strokeWidth
                )
                AnnotationTool.Circle -> Annotation.Circle(s,
                    hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()).toFloat(),
                    options.color, options.strokeWidth)
                AnnotationTool.Measurement -> {
                    val px = hypot((e.x - s.x).toDouble(), (e.y - s.y).toDouble()).toFloat()
                    val real = if (state.measurementCalibration > 0f) px / state.measurementCalibration else 0f
                    Annotation.Measurement(s, e, String.format("%.2f %s", real, state.measurementUnit),
                        state.measurementCalibration, state.measurementUnit, options.color, options.strokeWidth)
                }
                else -> null
            }
            if (draftAnno != null) drawAnnotation(draftAnno, textMeasurer)
        }
    }
}

fun DrawScope.drawInfoStamp(
    meta: PhotoMetadata,
    imgLeft: Float, imgTop: Float, imgW: Float, imgH: Float,
    measurer: androidx.compose.ui.text.TextMeasurer
) {
    val pad = (imgW * 0.012f).coerceAtLeast(10f)
    val stampW = (imgW * 0.36f).coerceAtLeast(220f)
    val stampH = (imgH * 0.11f).coerceAtLeast(72f)
    val sx = imgLeft + pad
    val sy = imgTop + imgH - stampH - pad
    val bg = Color(0xEE0A0E1A)
    drawRoundRect(
        color = bg,
        topLeft = Offset(sx, sy),
        size = Size(stampW, stampH),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = Offset(sx, sy),
        size = Size(stampW, stampH),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(1f)
    )
    val iconSize = stampH * 0.62f
    val iconCx = sx + iconSize / 2f + pad
    val iconCy = sy + stampH / 2f
    drawCompassIcon(iconCx, iconCy, iconSize / 2f, meta.heading)
    val textX = iconCx + iconSize / 2f + pad
    val headingLayout = measurer.measure(
        AnnotatedString("${meta.cardinal}  ${meta.heading.toInt()}°"),
        style = TextStyle(color = Color.White, fontSize = (stampH * 0.22f).coerceAtLeast(11f).sp)
    )
    val prLayout = measurer.measure(
        AnnotatedString("P ${"%.1f".format(meta.pitch)}°   R ${"%.1f".format(meta.roll)}°"),
        style = TextStyle(color = Color(0xFFB8C2D9), fontSize = (stampH * 0.16f).coerceAtLeast(9f).sp)
    )
    val timeLayout = measurer.measure(
        AnnotatedString(meta.formattedTime()),
        style = TextStyle(color = Color(0xFF8B95AD), fontSize = (stampH * 0.14f).coerceAtLeast(8f).sp)
    )
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            "${meta.cardinal}  ${meta.heading.toInt()}°",
            textX, sy + stampH * 0.36f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = headingLayout.size.height.toFloat()
                isAntiAlias = true
                isFakeBoldText = true
            }
        )
        canvas.nativeCanvas.drawText(
            "P ${"%.1f".format(meta.pitch)}°   R ${"%.1f".format(meta.roll)}°",
            textX, sy + stampH * 0.62f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#B8C2D9")
                textSize = prLayout.size.height.toFloat()
                isAntiAlias = true
            }
        )
        canvas.nativeCanvas.drawText(
            meta.formattedTime(),
            textX, sy + stampH * 0.84f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#8B95AD")
                textSize = timeLayout.size.height.toFloat()
                isAntiAlias = true
            }
        )
    }
}

fun DrawScope.drawCompassIcon(cx: Float, cy: Float, r: Float, heading: Float) {
    drawCircle(Color.White.copy(alpha = 0.20f), radius = r, center = Offset(cx, cy), style = Stroke(1.2f))
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.65f, center = Offset(cx, cy), style = Stroke(1f))
    val angleRad = ((heading - 90f) * Math.PI / 180.0)
    val tipX = (cx + r * cos(angleRad)).toFloat()
    val tipY = (cy + r * sin(angleRad)).toFloat()
    val tailX = (cx - r * 0.55f * cos(angleRad)).toFloat()
    val tailY = (cy - r * 0.55f * sin(angleRad)).toFloat()
    val perpA = angleRad + Math.PI / 2.0
    val w = r * 0.28f
    val lX = (tipX + w * cos(perpA)).toFloat()
    val lY = (tipY + w * sin(perpA)).toFloat()
    val rX = (tipX - w * cos(perpA)).toFloat()
    val rY = (tipY - w * sin(perpA)).toFloat()
    val needle = Path().apply {
        moveTo(tipX, tipY); lineTo(lX, lY); lineTo(cx, cy); lineTo(rX, rY); close()
    }
    drawPath(needle, Color(0xFFFF1744))
    val tailW = w * 0.7f
    val tlX = (tailX + tailW * cos(perpA)).toFloat()
    val tlY = (tailY + tailW * sin(perpA)).toFloat()
    val trX = (tailX - tailW * cos(perpA)).toFloat()
    val trY = (tailY - tailW * sin(perpA)).toFloat()
    val tail = Path().apply {
        moveTo(tailX, tailY); lineTo(tlX, tlY); lineTo(cx, cy); lineTo(trX, trY); close()
    }
    drawPath(tail, Color.White.copy(alpha = 0.85f))
    drawCircle(Color.White, radius = r * 0.10f, center = Offset(cx, cy))
    drawCircle(Color(0xFF0A0E1A), radius = r * 0.05f, center = Offset(cx, cy))
}

fun DrawScope.drawAnnotation(a: Annotation, measurer: androidx.compose.ui.text.TextMeasurer) {
    when (a) {
        is Annotation.Freehand -> {
            if (a.points.size < 2) return
            val p = Path().apply {
                moveTo(a.points.first().x, a.points.first().y)
                a.points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(p, a.color, style = Stroke(a.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        is Annotation.Arrow -> drawProfessionalArrow(a, measurer)
        is Annotation.Line -> drawLine(a.color, a.start, a.end, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
        is Annotation.Rect -> {
            drawRect(
                color = a.color,
                topLeft = a.topLeft,
                size = Size(a.bottomRight.x - a.topLeft.x, a.bottomRight.y - a.topLeft.y),
                style = Stroke(a.strokeWidth)
            )
        }
        is Annotation.Circle -> drawCircle(a.color, radius = a.radius, center = a.center, style = Stroke(a.strokeWidth))
        is Annotation.Measurement -> drawProfessionalDimension(a, measurer)
        is Annotation.TextNote -> drawProfessionalCallout(a, measurer)
    }
}

private fun DrawScope.drawProfessionalArrow(a: Annotation.Arrow, @Suppress("UNUSED_PARAMETER") measurer: androidx.compose.ui.text.TextMeasurer) {
    val sw = a.strokeWidth
    drawLine(a.color, a.start, a.end, strokeWidth = sw, cap = StrokeCap.Round)
    val angle = atan2((a.end.y - a.start.y).toDouble(), (a.end.x - a.start.x).toDouble())
    val headLen = (sw * 4.5f).coerceAtLeast(14f)
    val headW = headLen * 0.55f
    val tip = a.end
    val baseCx = (tip.x - headLen * cos(angle)).toFloat()
    val baseCy = (tip.y - headLen * sin(angle)).toFloat()
    val lx = (baseCx + headW * cos(angle + Math.PI / 2.0)).toFloat()
    val ly = (baseCy + headW * sin(angle + Math.PI / 2.0)).toFloat()
    val rx = (baseCx - headW * cos(angle + Math.PI / 2.0)).toFloat()
    val ry = (baseCy - headW * sin(angle + Math.PI / 2.0)).toFloat()
    val head = Path().apply {
        moveTo(tip.x, tip.y); lineTo(lx, ly); lineTo(rx, ry); close()
    }
    drawPath(head, a.color)
    val anchorR = (sw * 1.1f).coerceAtLeast(5f)
    drawCircle(Color.White, radius = anchorR + 1f, center = a.start)
    drawCircle(a.color, radius = anchorR, center = a.start)
}

private fun DrawScope.drawProfessionalDimension(a: Annotation.Measurement, measurer: androidx.compose.ui.text.TextMeasurer) {
    val sw = a.strokeWidth
    val extLen = (sw * 5f).coerceAtLeast(18f)
    val arrowLen = (sw * 3.2f).coerceAtLeast(10f)
    val arrowW = arrowLen * 0.45f
    val dx = a.end.x - a.start.x
    val dy = a.end.y - a.start.y
    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (len < 0.5f) return
    val nx = -dy / len
    val ny = dx / len
    val extColor = a.color.copy(alpha = 0.55f)
    val gap = (sw * 0.8f).coerceAtLeast(3f)
    drawLine(extColor, Offset(a.start.x + nx * gap, a.start.y + ny * gap), Offset(a.start.x + nx * extLen, a.start.y + ny * extLen), strokeWidth = (sw * 0.5f).coerceAtLeast(1.2f))
    drawLine(extColor, Offset(a.end.x + nx * gap, a.end.y + ny * gap), Offset(a.end.x + nx * extLen, a.end.y + ny * extLen), strokeWidth = (sw * 0.5f).coerceAtLeast(1.2f))
    val layout = measurer.measure(AnnotatedString(a.label), style = TextStyle(color = a.color, fontSize = 13.sp, textAlign = TextAlign.Center))
    val textW = layout.size.width.toFloat()
    val textH = layout.size.height.toFloat()
    val padX = 8f; val padY = 4f
    val midX = (a.start.x + a.end.x) / 2f
    val midY = (a.start.y + a.end.y) / 2f
    val breakHalf = (textW / 2f) + padX + 4f
    drawLine(a.color, a.start, Offset(midX - breakHalf, midY), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(a.color, Offset(midX + breakHalf, midY), a.end, strokeWidth = sw, cap = StrokeCap.Round)
    val drawArrow = { tip: Offset, reverse: Boolean ->
        val dir = if (reverse) 1.0 else -1.0
        val baseAng = atan2(dy.toDouble(), dx.toDouble())
        val ax = (tip.x + dir * arrowLen * cos(baseAng)).toFloat()
        val ay = (tip.y + dir * arrowLen * sin(baseAng)).toFloat()
        val lxa = (ax + arrowW * cos(baseAng + Math.PI / 2.0)).toFloat()
        val lya = (ay + arrowW * sin(baseAng + Math.PI / 2.0)).toFloat()
        val rxa = (ax - arrowW * cos(baseAng + Math.PI / 2.0)).toFloat()
        val rya = (ay - arrowW * sin(baseAng + Math.PI / 2.0)).toFloat()
        val p = Path().apply { moveTo(tip.x, tip.y); lineTo(lxa, lya); lineTo(rxa, rya); close() }
        drawPath(p, a.color)
    }
    drawArrow(a.start, reverse = false)
    drawArrow(a.end, reverse = true)
    drawRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(midX - textW / 2f - padX, midY - textH / 2f - padY / 2f),
        size = Size(textW + padX * 2f, textH + padY)
    )
    drawRect(
        color = a.color,
        topLeft = Offset(midX - textW / 2f - padX, midY - textH / 2f - padY / 2f),
        size = Size(textW + padX * 2f, textH + padY),
        style = Stroke((sw * 0.4f).coerceAtLeast(1f))
    )
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            a.label,
            midX - textW / 2f,
            midY + textH / 2f - 1f,
            android.graphics.Paint().apply {
                color = a.color.toAndroidColor()
                textSize = textH * 0.92f
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.LEFT
            }
        )
    }
}

private fun DrawScope.drawProfessionalCallout(a: Annotation.TextNote, measurer: androidx.compose.ui.text.TextMeasurer) {
    val sw = a.strokeWidth
    val markerR = (sw * 1.3f + 5f).coerceAtLeast(8f)
    val markerInnerR = markerR * 0.62f
    drawCircle(Color.White, radius = markerR + 1.5f, center = a.position)
    drawCircle(a.color, radius = markerR, center = a.position)
    drawCircle(Color.White, radius = markerInnerR, center = a.position)
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawCircle(a.position.x, a.position.y, markerInnerR * 0.55f, android.graphics.Paint().apply {
            color = a.color.toAndroidColor(); isAntiAlias = true
        })
    }
    val layout = measurer.measure(AnnotatedString(a.text), style = TextStyle(color = a.color, fontSize = a.fontSizeSp.sp))
    val textW = layout.size.width.toFloat()
    val textH = layout.size.height.toFloat()
    val padX = 9f; val padY = 5f
    val boxW = textW + padX * 2f
    val boxH = textH + padY * 2f
    val dirX = if (a.position.x + boxW + markerR * 3f < size.width) 1f else -1f
    val boxX = a.position.x + dirX * (markerR * 2.5f)
    val boxY = a.position.y - boxH / 2f
    val leaderStart = Offset(a.position.x + dirX * markerR, a.position.y)
    val leaderEnd = Offset(boxX, boxY + boxH / 2f)
    drawLine(a.color, leaderStart, leaderEnd, strokeWidth = (sw * 0.6f).coerceAtLeast(1.2f), cap = StrokeCap.Round)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.96f),
        topLeft = Offset(boxX, boxY),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = a.color,
        topLeft = Offset(boxX, boxY),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(5f, 5f),
        style = Stroke((sw * 0.45f).coerceAtLeast(1f))
    )
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(
            a.text,
            boxX + padX,
            boxY + padY + textH * 0.86f,
            android.graphics.Paint().apply {
                color = a.color.toAndroidColor()
                textSize = textH * 0.9f
                isAntiAlias = true
                isFakeBoldText = true
            }
        )
    }
}

internal fun colorToAndroidArgb(c: androidx.compose.ui.graphics.Color): Int {
    val a = (c.alpha * 255).toInt() and 0xFF
    val r = (c.red * 255).toInt() and 0xFF
    val g = (c.green * 255).toInt() and 0xFF
    val b = (c.blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

internal fun Color.toAndroidColor(): Int = colorToAndroidArgb(this)
