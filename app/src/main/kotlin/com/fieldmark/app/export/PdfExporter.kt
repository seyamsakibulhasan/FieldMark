package com.fieldmark.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    data class TitleBlock(
        val projectName: String,
        val location: String,
        val inspectedBy: String,
        val approvedBy: String,
        val reportNumber: String,
        val revision: String = "Rev-01",
        val notes: String = ""
    )

    fun exportAnnotated(
        context: Context,
        composited: Bitmap,
        block: TitleBlock
    ): Uri {
        val doc = PdfDocument()
        val pageWidth = 1240
        val pageHeight = 1754
        val margin = 48
        val blockHeight = 260
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val topArea = (pageHeight - margin - blockHeight).toFloat()
        val availW = pageWidth - margin * 2
        val srcW = composited.width
        val srcH = composited.height
        val scale = minOf(availW.toFloat() / srcW, topArea / srcH)
        val drawW = (srcW * scale).toInt()
        val drawH = (srcH * scale).toInt()
        val left = (pageWidth - drawW) / 2
        val top = margin
        val dst = Rect(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(composited, null, dst, null)

        drawFrameAndBlock(canvas, pageWidth, pageHeight, margin, blockHeight, block)
        doc.finishPage(page)

        val outDir = File(context.filesDir, "exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(outDir, "FieldMark_$ts.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun drawFrameAndBlock(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        margin: Int,
        blockHeight: Int,
        block: TitleBlock
    ) {
        val frame = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        canvas.drawRect(
            margin.toFloat(), margin.toFloat(),
            (pageWidth - margin).toFloat(), (pageHeight - margin).toFloat(),
            frame
        )

        val blockTop = pageHeight - margin - blockHeight
        val blockBottom = pageHeight - margin
        val blockLeft = margin
        val blockRight = pageWidth - margin
        val blockPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        canvas.drawRect(
            blockLeft.toFloat(), blockTop.toFloat(),
            blockRight.toFloat(), blockBottom.toFloat(),
            blockPaint
        )

        val rowH = (blockHeight - 50) / 3
        val headerRowH = 50
        val cols = intArrayOf(
            blockLeft,
            (blockLeft + blockRight) / 3,
            (blockLeft + blockRight) * 2 / 3,
            blockRight
        )
        val rows = intArrayOf(
            blockTop,
            blockTop + headerRowH,
            blockTop + headerRowH + rowH,
            blockTop + headerRowH + rowH * 2,
            blockBottom
        )
        for (i in 0 until cols.size - 1) {
            canvas.drawLine(cols[i].toFloat(), rows[0].toFloat(), cols[i].toFloat(), rows[4].toFloat(), blockPaint)
        }
        for (i in 0 until rows.size - 1) {
            canvas.drawLine(cols[0].toFloat(), rows[i].toFloat(), cols[3].toFloat(), rows[i].toFloat(), blockPaint)
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE; textSize = 16f; isFakeBoldText = true; isAntiAlias = true
        }
        val titleBg = Paint().apply { color = Color.parseColor("#1A2540"); isAntiAlias = true }
        canvas.drawRect(cols[0].toFloat(), rows[0].toFloat(), cols[3].toFloat(), rows[1].toFloat(), titleBg)
        canvas.drawText("  FIELDMARK — SITE DOCUMENTATION REPORT", (cols[0] + 12).toFloat(), (rows[0] + 32).toFloat(), titlePaint)

        val labelPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = 9.5f; isAntiAlias = true }
        val valuePaint = Paint().apply { color = Color.BLACK; textSize = 15f; isAntiAlias = true; isFakeBoldText = true }
        val valueSmPaint = Paint().apply { color = Color.BLACK; textSize = 13f; isAntiAlias = true }

        val cellPadX = 10
        val labelY = 18
        val valueYBig = 48
        val valueYSmall = 44

        fun cell(col: Int, row: Int, rowHeight: Int, label: String, value: String, big: Boolean) {
            val cx = cols[col] + cellPadX
            canvas.drawText(label, cx.toFloat(), (rows[row] + labelY).toFloat(), labelPaint)
            canvas.drawText(value, cx.toFloat(), (rows[row] + (if (big) valueYBig else valueYSmall)).toFloat(), if (big) valuePaint else valueSmPaint)
        }

        cell(0, 1, rowH, "PROJECT", block.projectName.ifBlank { "-" }, big = true)
        cell(1, 1, rowH, "LOCATION", block.location.ifBlank { "-" }, big = true)
        cell(2, 1, rowH, "REPORT NO.", block.reportNumber.ifBlank { "-" }, big = true)
        cell(3, 1, rowH, "DATE", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), big = true)

        cell(0, 2, rowH, "INSPECTED BY", block.inspectedBy.ifBlank { "-" }, big = false)
        cell(1, 2, rowH, "APPROVED BY", block.approvedBy.ifBlank { "-" }, big = false)
        cell(2, 2, rowH, "REVISION", block.revision.ifBlank { "Rev-01" }, big = false)
        cell(3, 2, rowH, "TIME", SimpleDateFormat("HH:mm", Locale.US).format(Date()), big = false)

        cell(0, 3, rowH, "NOTES", block.notes.take(80).ifBlank { "-" }, big = false)
        cell(1, 3, rowH, "GENERATED BY", "FieldMark", big = false)
        cell(2, 3, rowH, "FORMAT", "Engineering v1", big = false)
        cell(3, 3, rowH, "PAGE", "1 / 1", big = false)

        val footerY = (pageHeight - margin + 14).toFloat()
        val footerPaint = Paint().apply { color = Color.parseColor("#888888"); textSize = 8.5f; isAntiAlias = true }
        canvas.drawText("Generated by FieldMark (Mizanur Rahman) — Jetpack Compose", (blockLeft + 4).toFloat(), footerY, footerPaint)
    }
}
