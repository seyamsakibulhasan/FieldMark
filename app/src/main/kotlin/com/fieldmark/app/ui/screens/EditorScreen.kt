package com.fieldmark.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
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
import com.fieldmark.app.export.PdfExporter
import com.fieldmark.app.nav.Routes
import com.fieldmark.app.stereo.AnaglyphGenerator
import com.fieldmark.app.ui.components.AnnotationCanvas
import com.fieldmark.app.ui.components.AnnotationTool
import com.fieldmark.app.ui.components.EditorState
import com.fieldmark.app.ui.components.ToolOptions
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
    var color by remember { mutableStateOf(Color(0xFFFF3B30)) }
    var strokeWidth by remember { mutableStateOf(6f) }
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
                        if (items.isNotEmpty()) {
                            undoStack.add(items.toList()); redoStack.clear()
                            items.removeAt(items.lastIndex)
                        }
                    }) { Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.undo)) }
                    IconButton(onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(items.toList())
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            items.clear(); items.addAll(next)
                        }
                    }) { Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.redo)) }
                    IconButton(onClick = {
                        if (items.isNotEmpty()) {
                            undoStack.add(items.toList()); redoStack.clear()
                            items.removeAt(items.lastIndex)
                        }
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
                    onCommit = { commit(it) },
                    onTextTap = { offset ->
                        textTapPos = offset
                        textValue = ""
                    },
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Color(0xFFFF3B30), Color(0xFF1A5FB4), Color(0xFF008000),
                            Color(0xFFFFB300), Color.White, Color.Black
                        ).forEach { c ->
                            val selected = color == c
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 34.dp else 28.dp)
                                    .background(c, CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.Center)
                                        .background(c, CircleShape)
                                        .padding(2.dp)
                                )
                            }
                            if (c != Color.Black) {
                                Spacer(Modifier.width(0.dp))
                            }
                        }
                        // color selection is wired via separate chips below
                    }
                    Spacer(Modifier.size(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Color(0xFFFF3B30) to R.string.color_red,
                            Color(0xFF1A5FB4) to R.string.color_blue,
                            Color(0xFF008000) to R.string.color_green,
                            Color(0xFFFFB300) to R.string.color_yellow
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
                                        composeOnBitmap(bitmap, items.toList())
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
                                    val anaglyph = withContext(Dispatchers.Default) {
                                        AnaglyphGenerator.generate(bitmap)
                                    }
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

private fun composeOnBitmap(base: Bitmap, items: List<Annotation>): Bitmap {
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(out)
    items.forEach { drawOnCanvas(canvas, it) }
    return out
}

private fun drawOnCanvas(canvas: android.graphics.Canvas, a: Annotation) {
    val stroke = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = a.strokeWidth
        color = com.fieldmark.app.ui.components.colorToAndroidArgb(a.color)
    }
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
            val head = 26f
            val lx = (a.end.x - head * kotlin.math.cos(angle - Math.toRadians(28.0))).toFloat()
            val ly = (a.end.y - head * kotlin.math.sin(angle - Math.toRadians(28.0))).toFloat()
            val rx = (a.end.x - head * kotlin.math.cos(angle + Math.toRadians(28.0))).toFloat()
            val ry = (a.end.y - head * kotlin.math.sin(angle + Math.toRadians(28.0))).toFloat()
            canvas.drawLine(a.end.x, a.end.y, lx, ly, stroke)
            canvas.drawLine(a.end.x, a.end.y, rx, ry, stroke)
        }
        is Annotation.Line -> canvas.drawLine(a.start.x, a.start.y, a.end.x, a.end.y, stroke)
        is Annotation.Rect -> canvas.drawRect(a.topLeft.x, a.topLeft.y, a.bottomRight.x, a.bottomRight.y, stroke)
        is Annotation.Circle -> canvas.drawCircle(a.center.x, a.center.y, a.radius, stroke)
        is Annotation.Measurement -> {
            canvas.drawLine(a.start.x, a.start.y, a.end.x, a.end.y, stroke)
            val midX = (a.start.x + a.end.x) / 2f
            val midY = (a.start.y + a.end.y) / 2f
            val bg = android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(220, 255, 255, 255) }
            val tx = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                textSize = 36f
                color = com.fieldmark.app.ui.components.colorToAndroidArgb(a.color)
            }
            val w = tx.measureText(a.label)
            canvas.drawRect(midX - w / 2f - 6, midY - 50, midX + w / 2f + 6, midY - 14, bg)
            canvas.drawText(a.label, midX - w / 2f, midY - 20, tx)
        }
        is Annotation.TextNote -> {
            val tx = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                textSize = a.fontSizeSp * 3f
                color = com.fieldmark.app.ui.components.colorToAndroidArgb(a.color)
            }
            val w = tx.measureText(a.text)
            val h = tx.textSize
            val bg = android.graphics.Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(180, 255, 255, 255) }
            canvas.drawRect(a.position.x - 4, a.position.y - h - 4, a.position.x + w + 4, a.position.y + 4, bg)
            canvas.drawText(a.text, a.position.x, a.position.y, tx)
        }
    }
}
