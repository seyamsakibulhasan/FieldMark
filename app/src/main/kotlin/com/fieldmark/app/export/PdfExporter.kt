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
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val margin = 48
        val topArea = (pageHeight - margin - 220).toInt()
        val availW = pageWidth - margin * 2
        val srcW = composited.width
        val srcH = composited.height
        val scale = minOf(availW.toFloat() / srcW, topArea.toFloat() / srcH)
        val drawW = (srcW * scale).toInt()
        val drawH = (srcH * scale).toInt()
        val left = (pageWidth - drawW) / 2
        val top = margin
        val dst = Rect(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(composited, null, dst, null)

        drawFrameAndBlock(canvas, pageWidth, pageHeight, margin, block)
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
        block: TitleBlock
    ) {
        val frame = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(
            margin.toFloat(), margin.toFloat(),
            (pageWidth - margin).toFloat(), (pageHeight - margin).toFloat(),
            frame
        )

        val blockTop = pageHeight - margin - 200
        val blockBottom = pageHeight - margin
        val blockLeft = margin
        val blockRight = pageWidth - margin
        val blockPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRect(
            blockLeft.toFloat(), blockTop.toFloat(),
            blockRight.toFloat(), blockBottom.toFloat(),
            blockPaint
        )

        val cols = intArrayOf(
            blockLeft,
            (blockLeft + blockRight) / 2,
            blockRight
        )
        val rows = intArrayOf(
            blockTop,
            blockTop + 50,
            blockTop + 100,
            blockTop + 150,
            blockBottom
        )
        for (i in 0 until cols.size - 1) {
            canvas.drawLine(cols[i].toFloat(), blockTop.toFloat(), cols[i].toFloat(), blockBottom.toFloat(), blockPaint)
        }
        for (i in 0 until rows.size - 1) {
            canvas.drawLine(blockLeft.toFloat(), rows[i].toFloat(), blockRight.toFloat(), rows[i].toFloat(), blockPaint)
        }

        val title = Paint().apply { color = Color.BLACK; textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val label = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }
        val value = Paint().apply { color = Color.BLACK; textSize = 14f; isAntiAlias = true }
        val small = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }

        val cellPadX = 8
        val cellPadY = 14
        fun cell(col: Int, row: Int, labelText: String, valueText: String, big: Boolean = false) {
            val cx = cols[col] + cellPadX
            canvas.drawText(labelText, cx.toFloat(), (rows[row] + cellPadY).toFloat(), label)
            canvas.drawText(valueText, cx.toFloat(), (rows[row] + cellPadY + (if (big) 18 else 16)).toFloat(),
                if (big) value else small)
        }
        cell(0, 0, "PROJECT", block.projectName, true)
        cell(1, 0, "LOCATION", block.location, true)
        cell(2, 0, "REPORT NO.", block.reportNumber, true)
        cell(0, 1, "INSPECTED BY", block.inspectedBy)
        cell(1, 1, "APPROVED BY", block.approvedBy)
        cell(2, 1, "REVISION", block.revision)
        val today = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        cell(0, 2, "DATE / TIME", today)
        cell(1, 2, "NOTES", block.notes.take(60))
        cell(2, 2, "GENERATED BY", "FieldMark")
        canvas.drawText("FieldMark — Site Documentation Report", (blockLeft + cellPadX).toFloat(),
            (rows[3] + cellPadY + 16).toFloat(), small)
    }
}
