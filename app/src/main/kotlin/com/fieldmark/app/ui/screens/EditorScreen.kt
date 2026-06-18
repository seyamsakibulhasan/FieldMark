package com.fieldmark.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fieldmark.app.R
import com.fieldmark.app.annotation.Annotation
import com.fieldmark.app.capture.CaptureState
import com.fieldmark.app.capture.PhotoMetadata
import com.fieldmark.app.export.PdfExporter
import com.fieldmark.app.nav.Routes
import com.fieldmark.app.stereo.AnaglyphGenerator
import com.fieldmark.app.ui.components.AnnotationCanvas
import com.fieldmark.app.ui.components.AnnotationTool
import com.fieldmark.app.ui.components.EditorState
import com.fieldmark.app.ui.components.ToolOptions
import com.fieldmark.app.ui.components.colorToAndroidArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(nav: NavController, path: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
    val items = remember { mutableStateListOf<Annotation>() }
    val undoStack = remember { mutableStateListOf<List<Annotation>>() }
    val redoStack = remember { mutableStateListOf<List<Annotation>>() }
    var tool by remember { mutableStateOf(AnnotationTool.None) }
    var color by remember { mutableStateOf(Color(0xFFFF1744)) }
    var strokeWidth by remember { mutableStateOf(5f) }
    var pixelsPerUnit by remember { mutableStateOf(12f) }
    var unit by remember { mutableStateOf("cm") }
    var projectName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var inspectedBy by remember { mutableStateOf("") }
    var approvedBy by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var textTapPos by remember { mutableStateOf<Offset?>(null) }
    var textValue by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    var metadata by remember { mutableStateOf<PhotoMetadata?>(null) }
    LaunchedEffect(path) {
        val (file, meta) = CaptureState.consume()
        if (meta != null && file?.absolutePath == path) {
            metadata = meta
        }
    }

    fun commit(newItems: List<Annotation>) {
        undoStack.add(items.toList())
        redoStack.clear()
        items.clear()
        items.addAll(newItems)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (items.isNotEmpty()) { undoStack.add(items.toList()); redoStack.clear(); items.removeAt(items.lastIndex) }
                    }) { Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.undo)) }
                    IconButton(onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(items.toList())
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            items.clear(); items.addAll(next)
                        }
                    }) { Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.redo)) }
                    IconButton(onClick = {
                        if (items.isNotEmpty()) { undoStack.add(items.toList()); redoStack.clear(); items.removeAt(items.lastIndex) }
                    }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear)) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (bitmap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.image_unavailable))
                }
                return@Column
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                AnnotationCanvas(
                    state = EditorState(items.toList(), measurementCalibration = pixelsPerUnit, measurementUnit = unit),
                    tool = tool,
                    options = ToolOptions(color = color, strokeWidth = strokeWidth),
                    background = bitmap,
                    metadata = metadata,
                    onCommit = { commit(it) },
                    onTextTap = { offset -> textTapPos = offset; textValue = "" },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(
                            R.string.tool_none to AnnotationTool.None,
                            R.string.tool_freehand to AnnotationTool.Freehand,
                            R.string.tool_arrow to AnnotationTool.Arrow,
                            R.string.tool_line to AnnotationTool.Line,
                            R.string.tool_rect to AnnotationTool.Rect,
                            R.string.tool_circle to AnnotationTool.Circle,
                            R.string.tool_measure to AnnotationTool.Measurement,
                            R.string.tool_text to AnnotationTool.Text
                        )) { (labelRes, t) ->
                            FilterChip(
                                selected = tool == t,
                                onClick = { tool = t },
                                label = { Text(stringResource(labelRes)) },
                                leadingIcon = { Icon(iconForTool(t), contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Color(0xFFFF1744) to R.string.color_red,
                            Color(0xFF1A5FB4) to R.string.color_blue,
                            Color(0xFF008000) to R.string.color_green,
                            Color(0xFFFFB300) to R.string.color_yellow,
                            Color.Black to R.string.color_black,
                            Color.White to R.string.color_white
                        ).forEach { (c, labelRes) ->
                            FilterChip(
                                selected = color == c,
                                onClick = { color = c },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.stroke) + ": ${strokeWidth.toInt()}", modifier = Modifier.width(110.dp))
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 2f..20f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (tool == AnnotationTool.Measurement) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = pixelsPerUnit.toInt().toString(),
                                onValueChange = { v -> pixelsPerUnit = v.toFloatOrNull() ?: pixelsPerUnit },
                                label = { Text(stringResource(R.string.px_per_unit)) },
                                modifier = Modifier.width(140.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text(stringResource(R.string.unit)) },
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text(stringResource(R.string.project_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text(stringResource(R.string.location)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inspectedBy,
                            onValueChange = { inspectedBy = it },
                            label = { Text(stringResource(R.string.inspected_by)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.notes)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    val composited = withContext(Dispatchers.Default) {
                                        composeOnBitmap(bitmap, items.toList(), metadata)
                                    }
                                    val block = PdfExporter.TitleBlock(
                                        projectName = projectName.ifBlank { "FieldMark Project" },
                                        location = location.ifBlank { "-" },
                                        inspectedBy = inspectedBy.ifBlank { "-" },
                                        approvedBy = approvedBy.ifBlank { "-" },
                                        reportNumber = "FM-${SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())}",
                                        notes = notes
                                    )
                                    val uri = withContext(Dispatchers.IO) {
                                        PdfExporter.exportAnnotated(context, composited, block)
                                    }
                                    PdfExporter.share(context, uri)
                                    busy = false
                                }
                            },
                            enabled = !busy
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.export_pdf))
                        }
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    val anaglyph = withContext(Dispatchers.Default) { AnaglyphGenerator.generate(bitmap) }
                                    val out = File(context.filesDir, "anaglyph").apply { mkdirs() }
                                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    val file = File(out, "anaglyph_$ts.png")
                                    withContext(Dispatchers.IO) { file.outputStream().use { anaglyph.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                                    nav.navigate(Routes.anaglyph(file.absolutePath))
                                    busy = false
                                }
                            },
                            enabled = !busy
                        ) {
                            Icon(Icons.Default.ViewInAr, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.make_3d))
                        }
                    }
                }
            }
        }
    }

    if (textTapPos != null) {
        AlertDialog(
            onDismissRequest = { textTapPos = null; textValue = "" },
            confirmButton = {
                TextButton(onClick = {
                    val pos = textTapPos!!
                    commit(items.toList() + Annotation.TextNote(pos, textValue.ifBlank { "Note" }, 18f, color))
                    textTapPos = null; textValue = ""
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { textTapPos = null; textValue = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.add_text)) },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.text_placeholder)) }
                )
            }
        )
    }
}

private fun iconForTool(t: AnnotationTool) = when (t) {
    AnnotationTool.None -> Icons.Default.Brush
    AnnotationTool.Freehand -> Icons.Default.Brush
    AnnotationTool.Arrow -> Icons.Default.ArrowForward
    AnnotationTool.Line -> Icons.Default.ArrowForward
    AnnotationTool.Rect -> Icons.Default.CropSquare
    AnnotationTool.Circle -> Icons.Default.Circle
    AnnotationTool.Measurement -> Icons.Default.Straighten
    AnnotationTool.Text -> Icons.Default.TextFields
}

private fun composeOnBitmap(base: Bitmap, items: List<Annotation>, metadata: PhotoMetadata?): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    items.forEach { drawOnCanvas(canvas, it, base.width.toFloat(), base.height.toFloat()) }
    metadata?.let { drawInfoStampOnCanvas(canvas, it, base.width.toFloat(), base.height.toFloat()) }
    return out
}

private fun drawOnCanvas(canvas: android.graphics.Canvas, a: Annotation, imgW: Float, imgH: Float) {
    val sw = a.strokeWidth
    val stroke = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = sw
        color = colorToAndroidArgb(a.color)
    }
    val fill = android.graphics.Paint(stroke).apply { style = android.graphics.Paint.Style.FILL }
    when (a) {
        is Annotation.Freehand -> {
            if (a.points.size < 2) return
            val path = android.graphics.Path()
            path.moveTo(a.points.first().x, a.points.first().y)
            a.points.drop(1).forEach { path.lineTo(it.x, it.y) }
            stroke.strokeCap = android.graphics.Paint.Cap.ROUND
            stroke.strokeJoin = android.graphics.Paint.Join.ROUND
            canvas.drawPath(path, stroke)
        }
        is Annotation.Arrow -> {
            canvas.drawLine(a.start.x, a.start.y, a.end.x, a.end.y, stroke)
            val angle = kotlin.math.atan2((a.end.y - a.start.y).toDouble(), (a.end.x - a.start.x).toDouble())
            val headLen = (sw * 4.5f).coerceAtLeast(14f)
            val headW = headLen * 0.55f
            val baseCx = a.end.x - headLen * kotlin.math.cos(angle).toFloat()
            val baseCy = a.end.y - headLen * kotlin.math.sin(angle).toFloat()
            val lx = (baseCx + headW * kotlin.math.cos(angle + Math.PI / 2.0)).toFloat()
            val ly = (baseCy + headW * kotlin.math.sin(angle + Math.PI / 2.0)).toFloat()
            val rx = (baseCx - headW * kotlin.math.cos(angle + Math.PI / 2.0)).toFloat()
            val ry = (baseCy - headW * kotlin.math.sin(angle + Math.PI / 2.0)).toFloat()
            val head = android.graphics.Path().apply {
                moveTo(a.end.x, a.end.y); lineTo(lx, ly); lineTo(rx, ry); close()
            }
            canvas.drawPath(head, fill)
            val anchorR = (sw * 1.1f).coerceAtLeast(5f)
            canvas.drawCircle(a.start.x, a.start.y, anchorR + 1f, android.graphics.Paint(fill).apply { color = android.graphics.Color.WHITE })
            canvas.drawCircle(a.start.x, a.start.y, anchorR, fill)
        }
        is Annotation.Line -> canvas.drawLine(a.start.x, a.start.y, a.end.x, a.end.y, stroke)
        is Annotation.Rect -> canvas.drawRect(a.topLeft.x, a.topLeft.y, a.bottomRight.x, a.bottomRight.y, stroke)
        is Annotation.Circle -> canvas.drawCircle(a.center.x, a.center.y, a.radius, stroke)
        is Annotation.Measurement -> {
            val extLen = (sw * 5f).coerceAtLeast(18f)
            val arrowLen = (sw * 3.2f).coerceAtLeast(10f)
            val arrowW = arrowLen * 0.45f
            val dx = a.end.x - a.start.x
            val dy = a.end.y - a.start.y
            val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (len >= 0.5f) {
                val nx = -dy / len; val ny = dx / len
                val extColor = colorToAndroidArgb(a.color.copy(alpha = 0.55f))
                val extPaint = android.graphics.Paint(stroke).apply { color = extColor; strokeWidth = (sw * 0.5f).coerceAtLeast(1.2f) }
                val gap = (sw * 0.8f).coerceAtLeast(3f)
                canvas.drawLine(a.start.x + nx * gap, a.start.y + ny * gap, a.start.x + nx * extLen, a.start.y + ny * extLen, extPaint)
                canvas.drawLine(a.end.x + nx * gap, a.end.y + ny * gap, a.end.x + nx * extLen, a.end.y + ny * extLen, extPaint)
                val textH = sw * 4f
                val textPaint = android.graphics.Paint(fill).apply { textSize = textH; isFakeBoldText = true }
                val textW = textPaint.measureText(a.label)
                val padX = sw * 1.6f; val padY = sw * 0.8f
                val midX = (a.start.x + a.end.x) / 2f
                val midY = (a.start.y + a.end.y) / 2f
                val breakHalf = textW / 2f + padX + 4f
                canvas.drawLine(a.start.x, a.start.y, midX - breakHalf, midY, stroke)
                canvas.drawLine(midX + breakHalf, midY, a.end.x, a.end.y, stroke)
                val drawArrow = { tx: Float, ty: Float, reverse: Boolean ->
                    val dir = if (reverse) 1.0 else -1.0
                    val baseAng = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                    val ax = (tx + dir * arrowLen * kotlin.math.cos(baseAng)).toFloat()
                    val ay = (ty + dir * arrowLen * kotlin.math.sin(baseAng)).toFloat()
                    val lxa = (ax + arrowW * kotlin.math.cos(baseAng + Math.PI / 2.0)).toFloat()
                    val lya = (ay + arrowW * kotlin.math.sin(baseAng + Math.PI / 2.0)).toFloat()
                    val rxa = (ax - arrowW * kotlin.math.cos(baseAng + Math.PI / 2.0)).toFloat()
                    val rya = (ay - arrowW * kotlin.math.sin(baseAng + Math.PI / 2.0)).toFloat()
                    val p = android.graphics.Path().apply { moveTo(tx, ty); lineTo(lxa, lya); lineTo(rxa, rya); close() }
                    canvas.drawPath(p, fill)
                }
                drawArrow(a.start.x, a.start.y, reverse = false)
                drawArrow(a.end.x, a.end.y, reverse = true)
                val bg = android.graphics.Paint(fill).apply { color = android.graphics.Color.argb(235, 255, 255, 255) }
                canvas.drawRect(midX - textW / 2f - padX, midY - textH / 2f - padY / 2f, midX + textW / 2f + padX, midY + textH / 2f + padY / 2f, bg)
                canvas.drawRect(midX - textW / 2f - padX, midY - textH / 2f - padY / 2f, midX + textW / 2f + padX, midY + textH / 2f + padY / 2f, android.graphics.Paint(stroke).apply { strokeWidth = (sw * 0.4f).coerceAtLeast(1f) })
                canvas.drawText(a.label, midX - textW / 2f, midY + textH / 2.5f, textPaint)
            }
        }
        is Annotation.TextNote -> {
            val markerR = (sw * 1.3f + 5f).coerceAtLeast(8f)
            val whiteFill = android.graphics.Paint(fill).apply { color = android.graphics.Color.WHITE }
            val innerR = markerR * 0.62f
            canvas.drawCircle(a.position.x, a.position.y, markerR + 1.5f, whiteFill)
            canvas.drawCircle(a.position.x, a.position.y, markerR, fill)
            canvas.drawCircle(a.position.x, a.position.y, innerR, whiteFill)
            canvas.drawCircle(a.position.x, a.position.y, innerR * 0.55f, fill)
            val textH = a.fontSizeSp * 2.6f
            val tp = android.graphics.Paint(fill).apply { textSize = textH; isFakeBoldText = true }
            val textW = tp.measureText(a.text)
            val padX = sw * 1.8f; val padY = sw * 1.0f
            val boxW = textW + padX * 2f
            val boxH = textH + padY * 2f
            val dirX = if (a.position.x + boxW + markerR * 3f < imgW) 1f else -1f
            val boxX = a.position.x + dirX * (markerR * 2.5f)
            val boxY = a.position.y - boxH / 2f
            val leaderStartX = a.position.x + dirX * markerR
            val leaderStartY = a.position.y
            val leaderEndX = boxX
            val leaderEndY = boxY + boxH / 2f
            val leader = android.graphics.Paint(stroke).apply { strokeWidth = (sw * 0.6f).coerceAtLeast(1.2f) }
            canvas.drawLine(leaderStartX, leaderStartY, leaderEndX, leaderEndY, leader)
            val boxRect = android.graphics.RectF(boxX, boxY, boxX + boxW, boxY + boxH)
            val boxBg = android.graphics.Paint(fill).apply { color = android.graphics.Color.argb(245, 255, 255, 255) }
            canvas.drawRoundRect(boxRect, 4f, 4f, boxBg)
            canvas.drawRoundRect(boxRect, 4f, 4f, android.graphics.Paint(stroke).apply { strokeWidth = (sw * 0.45f).coerceAtLeast(1f) })
            canvas.drawText(a.text, boxX + padX, boxY + padY + textH * 0.85f, tp)
        }
    }
}

private fun drawInfoStampOnCanvas(canvas: android.graphics.Canvas, meta: PhotoMetadata, imgW: Float, imgH: Float) {
    val pad = (imgW * 0.012f).coerceAtLeast(10f)
    val stampW = (imgW * 0.36f).coerceAtLeast(220f)
    val stampH = (imgH * 0.11f).coerceAtLeast(72f)
    val sx = pad
    val sy = imgH - stampH - pad
    val bg = android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#EE0A0E1A") }
    val rect = android.graphics.RectF(sx, sy, sx + stampW, sy + stampH)
    canvas.drawRoundRect(rect, 10f, 10f, bg)
    val border = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1f; color = android.graphics.Color.argb(45, 255, 255, 255) }
    canvas.drawRoundRect(rect, 10f, 10f, border)

    val iconSize = stampH * 0.62f
    val iconCx = sx + iconSize / 2f + pad
    val iconCy = sy + stampH / 2f
    val iconR = iconSize / 2f
    val ring = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1.4f; color = android.graphics.Color.argb(55, 255, 255, 255) }
    canvas.drawCircle(iconCx, iconCy, iconR, ring)
    canvas.drawCircle(iconCx, iconCy, iconR * 0.65f, android.graphics.Paint(ring).apply { strokeWidth = 1f; color = android.graphics.Color.argb(35, 255, 255, 255) })
    val angleRad = ((meta.heading - 90f) * Math.PI / 180.0)
    val tipX = (iconCx + iconR * kotlin.math.cos(angleRad)).toFloat()
    val tipY = (iconCy + iconR * kotlin.math.sin(angleRad)).toFloat()
    val tailX = (iconCx - iconR * 0.55f * kotlin.math.cos(angleRad)).toFloat()
    val tailY = (iconCy - iconR * 0.55f * kotlin.math.sin(angleRad)).toFloat()
    val perpA = angleRad + Math.PI / 2.0
    val w = iconR * 0.28f
    val needle = android.graphics.Path().apply {
        moveTo(tipX, tipY)
        lineTo((tipX + w * kotlin.math.cos(perpA)).toFloat(), (tipY + w * kotlin.math.sin(perpA)).toFloat())
        lineTo(iconCx, iconCy)
        lineTo((tipX - w * kotlin.math.cos(perpA)).toFloat(), (tipY - w * kotlin.math.sin(perpA)).toFloat())
        close()
    }
    canvas.drawPath(needle, android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#FFFF1744") })
    val tw = w * 0.7f
    val tail = android.graphics.Path().apply {
        moveTo(tailX, tailY)
        lineTo((tailX + tw * kotlin.math.cos(perpA)).toFloat(), (tailY + tw * kotlin.math.sin(perpA)).toFloat())
        lineTo(iconCx, iconCy)
        lineTo((tailX - tw * kotlin.math.cos(perpA)).toFloat(), (tailY - tw * kotlin.math.sin(perpA)).toFloat())
        close()
    }
    canvas.drawPath(tail, android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(220, 255, 255, 255) })
    canvas.drawCircle(iconCx, iconCy, iconR * 0.10f, android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE })
    canvas.drawCircle(iconCx, iconCy, iconR * 0.05f, android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#FF0A0E1A") })

    val textX = iconCx + iconR + pad
    val headingText = "${meta.cardinal}  ${meta.heading.toInt()}°"
    val prText = "P ${"%.1f".format(meta.pitch)}°   R ${"%.1f".format(meta.roll)}°"
    val timeText = meta.formattedTime()
    val headingPaint = android.graphics.Paint().apply { isAntiAlias = true; isFakeBoldText = true; color = android.graphics.Color.WHITE; textSize = stampH * 0.22f }
    val prPaint = android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#B8C2D9"); textSize = stampH * 0.16f }
    val timePaint = android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.parseColor("#8B95AD"); textSize = stampH * 0.14f }
    canvas.drawText(headingText, textX, sy + stampH * 0.36f, headingPaint)
    canvas.drawText(prText, textX, sy + stampH * 0.62f, prPaint)
    canvas.drawText(timeText, textX, sy + stampH * 0.84f, timePaint)
}
