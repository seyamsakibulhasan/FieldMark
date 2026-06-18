package com.fieldmark.app.annotation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

sealed class Annotation {
    abstract val color: Color
    abstract val strokeWidth: Float

    data class Freehand(
        val points: List<Offset>,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class Arrow(
        val start: Offset,
        val end: Offset,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class Line(
        val start: Offset,
        val end: Offset,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class Rect(
        val topLeft: Offset,
        val bottomRight: Offset,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class Circle(
        val center: Offset,
        val radius: Float,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class Measurement(
        val start: Offset,
        val end: Offset,
        val label: String,
        val pixelsPerUnit: Float,
        val unit: String,
        override val color: Color,
        override val strokeWidth: Float
    ) : Annotation()

    data class TextNote(
        val position: Offset,
        val text: String,
        val fontSizeSp: Float,
        override val color: Color,
        override val strokeWidth: Float = 0f
    ) : Annotation()
}
