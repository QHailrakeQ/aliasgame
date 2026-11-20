package com.aliasgame.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint as FrameworkPaint
import android.graphics.Path as FrameworkPath

/**
 * A party in the dark for the home screen of the neon styles: two spotlight beams and a crowd
 * of silhouettes with raised hands, lit from behind by [rimLight].
 *
 * Purely decorative and static (drawn once per size), so it costs nothing while the screen is
 * idle. The crowd is drawn in a 390 × 300 design space and scaled to the full width.
 */
@Composable
fun PartyScene(
    accent: Color,
    beam: Color,
    rimLight: Color,
    modifier: Modifier = Modifier
) {
    val crowd = remember { crowdPeople() }
    Canvas(modifier) {
        val crowdScale = size.width / DESIGN_WIDTH
        val crowdHeight = DESIGN_HEIGHT * crowdScale
        val crowdTop = size.height - crowdHeight

        drawStageLight(accent, beam, crowdTop)

        translate(top = crowdTop) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.scale(crowdScale, crowdScale)
                crowd.forEach { person -> person.draw(native, rimLight) }
                native.restore()
            }
        }
    }
}

private fun DrawScope.drawStageLight(accent: Color, beam: Color, crowdTop: Float) {
    // Light coming from the stage behind the crowd.
    val glowCenter = Offset(size.width / 2, size.height + size.width * 0.15f)
    val glowRadius = size.width * 0.95f
    drawCircle(
        Brush.radialGradient(
            listOf(accent.copy(alpha = 0.45f), beam.copy(alpha = 0.12f), Color.Transparent),
            glowCenter,
            glowRadius
        ),
        glowRadius,
        glowCenter
    )

    // Two spotlight beams crossing above the heads.
    val beamWidth = size.width * 0.3f
    val beamLength = size.height
    val beamTop = crowdTop - size.width * 0.55f
    listOf(
        Triple(size.width * 0.08f, 28f, beam),
        Triple(size.width * 0.64f, -30f, accent)
    ).forEach { (left, degrees, color) ->
        rotate(degrees, pivot = Offset(left + beamWidth / 2, beamTop)) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.22f), Color.Transparent),
                    startY = beamTop,
                    endY = beamTop + beamLength * 0.75f
                ),
                topLeft = Offset(left, beamTop),
                size = Size(beamWidth, beamLength)
            )
        }
    }
}

private const val DESIGN_WIDTH = 390f
private const val DESIGN_HEIGHT = 300f

/** How a silhouette holds its arms; coordinates are relative to its feet, before scaling. */
private enum class Arm(val path: List<Pair<Float, Float>>) {
    LEFT_UP(listOf(-24f to -138f, -50f to -184f, -42f to -234f)),
    RIGHT_UP(listOf(24f to -138f, 50f to -184f, 42f to -234f)),
    LEFT_DOWN(listOf(-24f to -138f, -40f to -92f, -36f to -56f)),
    RIGHT_DOWN(listOf(24f to -138f, 42f to -94f, 34f to -60f)),
    LEFT_WAVE(listOf(-24f to -138f, -30f to -196f, -14f to -238f)),
    RIGHT_CHEERS(listOf(24f to -138f, 46f to -178f, 40f to -224f))
}

private enum class Extra { NONE, BUN, PONYTAIL, CUP_RIGHT, CUP_LEFT }

private class Person(
    val x: Float,
    val scale: Float,
    val color: Color,
    val arms: List<Arm>,
    val extra: Extra = Extra.NONE,
    val isFrontRow: Boolean
) {
    private val body = FrameworkPath().apply {
        moveTo(-30f, -146f)
        quadTo(0f, -158f, 30f, -146f)
        quadTo(36f, -80f, 32f, 0f)
        lineTo(-32f, 0f)
        quadTo(-36f, -80f, -30f, -146f)
        close()
        addCircle(0f, -170f, 18f, FrameworkPath.Direction.CW)
        when (extra) {
            Extra.BUN -> addCircle(0f, -193f, 9f, FrameworkPath.Direction.CW)
            Extra.PONYTAIL -> addOval(-23f, -180f, -5f, -140f, FrameworkPath.Direction.CW)
            Extra.CUP_RIGHT -> addRoundRect(31f, -246f, 47f, -224f, 3f, 3f, FrameworkPath.Direction.CW)
            Extra.CUP_LEFT -> addRoundRect(-52f, -244f, -36f, -222f, 3f, 3f, FrameworkPath.Direction.CW)
            Extra.NONE -> Unit
        }
    }

    private val armPaths = arms.map { arm ->
        FrameworkPath().apply {
            arm.path.forEachIndexed { index, (px, py) -> if (index == 0) moveTo(px, py) else lineTo(px, py) }
        }
    }

    fun draw(canvas: android.graphics.Canvas, rimLight: Color) {
        val fill = FrameworkPaint(FrameworkPaint.ANTI_ALIAS_FLAG).apply {
            style = FrameworkPaint.Style.FILL
            this.color = this@Person.color.toArgb()
            if (isFrontRow) setShadowLayer(6f, 0f, 0f, rimLight.toArgb())
        }
        val stroke = FrameworkPaint(fill).apply {
            style = FrameworkPaint.Style.STROKE
            strokeWidth = 13f
            strokeCap = FrameworkPaint.Cap.ROUND
            strokeJoin = FrameworkPaint.Join.ROUND
        }
        canvas.save()
        canvas.translate(x, DESIGN_HEIGHT)
        canvas.scale(scale, scale)
        canvas.drawPath(body, fill)
        armPaths.forEach { canvas.drawPath(it, stroke) }
        canvas.restore()
    }
}

private val BackRow = Color(0xFF1B0C38)
private val FrontRow = Color(0xFF05020D)

/** Back row first, so the front row is drawn over it. */
private fun crowdPeople() = listOf(
    Person(70f, 0.78f, BackRow, listOf(Arm.LEFT_UP, Arm.RIGHT_UP), isFrontRow = false),
    Person(200f, 0.74f, BackRow, listOf(Arm.RIGHT_UP, Arm.LEFT_DOWN), Extra.BUN, isFrontRow = false),
    Person(330f, 0.8f, BackRow, listOf(Arm.LEFT_UP, Arm.RIGHT_DOWN), isFrontRow = false),
    Person(18f, 1.05f, FrontRow, listOf(Arm.RIGHT_CHEERS), Extra.CUP_RIGHT, isFrontRow = true),
    Person(130f, 1f, FrontRow, listOf(Arm.LEFT_UP, Arm.RIGHT_UP), Extra.PONYTAIL, isFrontRow = true),
    Person(262f, 1.08f, FrontRow, listOf(Arm.LEFT_WAVE, Arm.RIGHT_DOWN), isFrontRow = true),
    Person(378f, 0.96f, FrontRow, listOf(Arm.LEFT_UP), Extra.CUP_LEFT, isFrontRow = true)
)
