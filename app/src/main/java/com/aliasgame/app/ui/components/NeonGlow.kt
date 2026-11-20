package com.aliasgame.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a soft colored light around a rounded rectangle the size of this element, the way a
 * neon tube lights the wall behind it. Place it before the element's own background or border.
 *
 * The light is a blurred shadow ([android.graphics.Paint.setShadowLayer]), which needs no
 * extra layer and costs little to draw. Android only renders it for shapes since API 28; on
 * older versions the element simply has no glow and keeps its colored outline.
 *
 * Does nothing for a transparent [color], so flat styles can pass
 * [com.aliasgame.app.ui.theme.GamePalette.glow] as is.
 */
fun Modifier.neonGlow(color: Color, cornerRadius: Dp, glowRadius: Dp = 14.dp): Modifier {
    if (color.alpha == 0f) return this
    return drawBehind {
        val radius = cornerRadius.toPx()
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(glowRadius.toPx(), 0f, 0f, color.toArgb())
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
        }
    }
}

/** This style with a glow of [color] around the letters, or unchanged for a transparent [color]. */
fun TextStyle.withGlow(color: Color, blurRadius: Float = 24f): TextStyle =
    if (color.alpha == 0f) this else copy(shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = blurRadius))
