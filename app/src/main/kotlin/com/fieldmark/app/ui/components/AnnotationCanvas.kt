package com.fieldmark.app.ui.components

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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class AnnotationTool {
    None, Freehand, Arrow, Line, Rect, Circle, Measurement, Text
}

data class ToolOptions(
    val color: Color = Color(0xFFFF3B30),
    val strokeWidth: Float = 6f
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
    background: android.graphics.Bitmap? = null,
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
        // Background image (fit inside the canvas, letterboxed)
        if (img != null) {
            val scale = min(size.width / img.width, size.height / img.height)
            val dw = img.width * scale
            val dh = img.height * scale
            val left = (size.width - dw) / 2f
            val top = (size.height - dh) / 2f
            drawImage(
                image = img,
                dstSize = IntSize(dw.toInt(), dh.toInt()),
                dstOffset = IntOffset(left.toInt(), top.toInt())
            )
        }
        if (drawItems) {
            state.items.forEach { drawAnnotation(it, textMeasurer) }
        }
        // Draft preview
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
        is Annotation.Arrow -> {
            drawLine(a.color, a.start, a.end, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
            val angle = atan2((a.end.y - a.start.y).toDouble(), (a.end.x - a.start.x).toDouble())
            val head = 22f
            val left = Offset(
                (a.end.x - head * cos(angle - Math.toRadians(28.0))).toFloat(),
                (a.end.y - head * sin(angle - Math.toRadians(28.0))).toFloat()
            )
            val right = Offset(
                (a.end.x - head * cos(angle + Math.toRadians(28.0))).toFloat(),
                (a.end.y - head * sin(angle + Math.toRadians(28.0))).toFloat()
            )
            drawLine(a.color, a.end, left, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
            drawLine(a.color, a.end, right, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
        }
        is Annotation.Line -> drawLine(a.color, a.start, a.end, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
        is Annotation.Rect -> drawRect(
            color = a.color,
            topLeft = a.topLeft,
            size = Size(a.bottomRight.x - a.topLeft.x, a.bottomRight.y - a.topLeft.y),
            style = Stroke(a.strokeWidth)
        )
        is Annotation.Circle -> drawCircle(a.color, radius = a.radius, center = a.center, style = Stroke(a.strokeWidth))
        is Annotation.Measurement -> {
            drawLine(a.color, a.start, a.end, strokeWidth = a.strokeWidth, cap = StrokeCap.Round)
            val mid = Offset((a.start.x + a.end.x) / 2f, (a.start.y + a.end.y) / 2f)
            val layout = measurer.measure(
                AnnotatedString(a.label),
                style = TextStyle(color = a.color, fontSize = 14.sp, textAlign = TextAlign.Center)
            )
            val pad = 6
            val w = layout.size.width + pad * 2
            val h = layout.size.height + pad
            drawRect(
                Color.White.copy(alpha = 0.85f),
                topLeft = Offset(mid.x - w / 2f, mid.y - h / 2f),
                size = Size(w.toFloat(), h.toFloat())
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    a.label,
                    mid.x - layout.size.width / 2f,
                    mid.y + layout.size.height / 2f,
                    android.graphics.Paint().apply {
                        color = a.color.toAndroidColor()
                        textSize = layout.size.height.toFloat()
                        isAntiAlias = true
                    }
                )
            }
        }
        is Annotation.TextNote -> {
            val layout = measurer.measure(
                AnnotatedString(a.text),
                style = TextStyle(color = a.color, fontSize = a.fontSizeSp.sp)
            )
            val pad = 4
            drawRect(
                Color.White.copy(alpha = 0.7f),
                topLeft = Offset(a.position.x - pad, a.position.y - layout.size.height - pad),
                size = Size((layout.size.width + pad * 2).toFloat(), (layout.size.height + pad * 2).toFloat())
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    a.text,
                    a.position.x,
                    a.position.y,
                    android.graphics.Paint().apply {
                        color = a.color.toAndroidColor()
                        textSize = layout.size.height.toFloat()
                        isAntiAlias = true
                    }
                )
            }
        }
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
