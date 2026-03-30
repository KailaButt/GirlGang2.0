package com.example.consolicalm

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PlantPreview(
    type: PlantType,
    stage: Int,
    fullyUpgradedPreview: Boolean,
    modifier: Modifier = Modifier
) {
    when (type) {
        PlantType.JUNIPER_BONSAI -> JuniperBonsaiPreview(stage, fullyUpgradedPreview, modifier)
        PlantType.REGULAR_TREE -> MountainPinePreview(stage, fullyUpgradedPreview, modifier)
        PlantType.FLOWER -> FlowerPreview(stage, fullyUpgradedPreview, modifier)
        PlantType.CACTUS -> CactusPreview(stage, fullyUpgradedPreview, modifier)
    }
}

@Composable
private fun JuniperBonsaiPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val potClay = Color(0xFFB85A3D)
    val potClayDark = Color(0xFF8F3E2B)
    val trunkBrown = Color(0xFF6B3F2A)
    val trunkDark = Color(0xFF4E2A1C)
    val green = Color(0xFF2E7D32)
    val greenDeep = Color(0xFF1B5E20)
    val greenLight = Color(0xFF43A047)
    val berryBlue = Color(0xFF3F6E9E)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        drawRoundRect(
            color = potClayDark,
            topLeft = Offset(cx - w * 0.40f, h * 0.70f),
            size = Size(w * 0.80f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Fill
        )

        val body = Path().apply {
            moveTo(cx - w * 0.34f, h * 0.80f)
            lineTo(cx + w * 0.34f, h * 0.80f)
            lineTo(cx + w * 0.26f, h * 0.92f)
            lineTo(cx - w * 0.26f, h * 0.92f)
            close()
        }
        drawPath(body, potClay, style = Fill)

        val trunk = Path().apply {
            moveTo(cx - w * 0.06f, h * 0.78f)
            cubicTo(cx - w * 0.22f, h * 0.62f, cx + w * 0.18f, h * 0.58f, cx + w * 0.02f, h * 0.48f)
            cubicTo(cx - w * 0.02f, h * 0.44f, cx + w * 0.28f, h * 0.46f, cx + w * 0.16f, h * 0.32f)
        }
        drawPath(trunk, trunkBrown, style = Stroke(w * 0.10f, cap = StrokeCap.Round))
        drawPath(trunk, trunkDark, style = Stroke(w * 0.04f, cap = StrokeCap.Round))

        fun pad(x: Float, y: Float, ww: Float, hh: Float) {
            drawRoundRect(
                greenDeep,
                Offset(x - ww / 2, y - hh / 2),
                Size(ww, hh),
                cornerRadius = CornerRadius(hh / 2, hh / 2),
                style = Fill
            )
            drawRoundRect(
                green,
                Offset(x - ww * 0.42f, y - hh * 0.36f),
                Size(ww * 0.84f, hh * 0.78f),
                cornerRadius = CornerRadius(hh / 2, hh / 2),
                style = Fill
            )
            drawRoundRect(
                greenLight,
                Offset(x + ww * 0.05f, y - hh * 0.45f),
                Size(ww * 0.42f, hh * 0.40f),
                cornerRadius = CornerRadius(hh / 2, hh / 2),
                style = Fill
            )
        }

        val crownX = cx + w * 0.18f
        val crownY = h * 0.36f
        pad(crownX, crownY + h * 0.08f, w * 0.42f, h * 0.18f)
        pad(crownX + w * 0.20f, crownY + h * 0.12f, w * 0.30f, h * 0.14f)

        if (stage >= 2) pad(crownX - w * 0.18f, crownY + h * 0.16f, w * 0.28f, h * 0.13f)
        if (stage >= 3) pad(crownX + w * 0.08f, crownY - h * 0.05f, w * 0.28f, h * 0.13f)

        if (upgraded) {
            drawCircle(berryBlue, radius = w * 0.03f, center = Offset(crownX + w * 0.06f, crownY + h * 0.12f))
            drawCircle(berryBlue, radius = w * 0.026f, center = Offset(crownX + w * 0.22f, crownY + h * 0.14f))
        }
    }
}

@Composable
private fun MountainPinePreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val trunk = Color(0xFF6B3F2A)
    val trunkDark = Color(0xFF4E2A1C)
    val pine = Color(0xFF1B5E20)
    val pine2 = Color(0xFF2E7D32)
    val pineHi = Color(0xFF43A047)
    val snow = Color(0xFFF3F6FA)
    val snowShadow = Color(0xFFDCE4EE)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        if (upgraded) {
            drawRoundRect(
                color = snowShadow,
                topLeft = Offset(cx - w * 0.40f, h * 0.86f),
                size = Size(w * 0.80f, h * 0.10f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                style = Fill
            )
            drawRoundRect(
                color = snow,
                topLeft = Offset(cx - w * 0.38f, h * 0.85f),
                size = Size(w * 0.76f, h * 0.10f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                style = Fill
            )
        }

        drawLine(
            color = trunk,
            start = Offset(cx, h * 0.82f),
            end = Offset(cx, h * 0.46f),
            strokeWidth = w * 0.10f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = trunkDark,
            start = Offset(cx + w * 0.02f, h * 0.82f),
            end = Offset(cx + w * 0.02f, h * 0.50f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )

        val tiers = 2 + stage.coerceIn(0, 4)
        for (i in 0 until tiers) {
            val y = h * (0.52f - i * 0.07f)
            val halfW = w * (0.34f - i * 0.04f) * (1f + stage * 0.03f)

            val tri = Path().apply {
                moveTo(cx, y - h * 0.08f)
                lineTo(cx - halfW, y + h * 0.06f)
                lineTo(cx + halfW, y + h * 0.06f)
                close()
            }

            val c = when (i % 3) {
                0 -> pine
                1 -> pine2
                else -> pineHi
            }
            drawPath(tri, color = c, style = Fill)

            if (upgraded) {
                val capShadow = Path().apply {
                    moveTo(cx, y - h * 0.075f)
                    lineTo(cx - halfW * 0.55f, y - h * 0.005f)
                    lineTo(cx + halfW * 0.55f, y - h * 0.005f)
                    close()
                }
                drawPath(capShadow, color = snowShadow, style = Fill)

                val cap = Path().apply {
                    moveTo(cx, y - h * 0.08f)
                    lineTo(cx - halfW * 0.52f, y - h * 0.01f)
                    lineTo(cx + halfW * 0.52f, y - h * 0.01f)
                    close()
                }
                drawPath(cap, color = snow, style = Fill)
            }
        }
    }
}

@Composable
private fun FlowerPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val stem = Color(0xFF3E8E41)
    val leaf = Color(0xFF66BB6A)
    val petalMain = Color(0xFFF06292)
    val petalDark = Color(0xFFEC407A)
    val petalLight = Color(0xFFF8BBD0)
    val dew = Color(0xCCFFFFFF)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        drawLine(
            color = stem,
            start = Offset(cx, h * 0.82f),
            end = Offset(cx, h * 0.42f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )

        val leftLeaf = Path().apply {
            moveTo(cx, h * 0.66f)
            quadraticTo(cx - w * 0.22f, h * 0.58f, cx - w * 0.12f, h * 0.76f)
            quadraticTo(cx - w * 0.02f, h * 0.72f, cx, h * 0.66f)
            close()
        }
        drawPath(leftLeaf, leaf)

        if (stage >= 2) {
            val rightLeaf = Path().apply {
                moveTo(cx, h * 0.58f)
                quadraticTo(cx + w * 0.22f, h * 0.50f, cx + w * 0.10f, h * 0.70f)
                quadraticTo(cx + w * 0.02f, h * 0.65f, cx, h * 0.58f)
                close()
            }
            drawPath(rightLeaf, leaf)
        }

        val bloomScale = 1f + (stage.coerceIn(0, 4) * 0.05f)

        val centerPetal = Path().apply {
            moveTo(cx, h * 0.26f)
            quadraticTo(cx - w * 0.10f * bloomScale, h * 0.40f, cx, h * 0.48f)
            quadraticTo(cx + w * 0.10f * bloomScale, h * 0.40f, cx, h * 0.26f)
            close()
        }
        drawPath(centerPetal, petalMain)

        val leftPetal = Path().apply {
            moveTo(cx, h * 0.42f)
            quadraticTo(cx - w * 0.18f * bloomScale, h * 0.34f, cx - w * 0.12f * bloomScale, h * 0.24f)
            quadraticTo(cx - w * 0.03f, h * 0.30f, cx, h * 0.42f)
            close()
        }
        drawPath(leftPetal, petalDark)

        val rightPetal = Path().apply {
            moveTo(cx, h * 0.42f)
            quadraticTo(cx + w * 0.18f * bloomScale, h * 0.34f, cx + w * 0.12f * bloomScale, h * 0.24f)
            quadraticTo(cx + w * 0.03f, h * 0.30f, cx, h * 0.42f)
            close()
        }
        drawPath(rightPetal, petalLight)

        if (upgraded) {
            drawCircle(
                color = dew,
                radius = w * 0.018f,
                center = Offset(cx + w * 0.08f, h * 0.28f)
            )
            drawCircle(
                color = dew,
                radius = w * 0.014f,
                center = Offset(cx - w * 0.06f, h * 0.33f)
            )
        }
    }
}

@Composable
private fun CactusPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val pot = Color(0xFFC97A56)
    val potDark = Color(0xFFA85F40)
    val leafOuter = Color(0xFF4CAF50)
    val leafInner = Color(0xFF81C784)
    val centerColor = Color(0xFF2E7D32)
    val bloom = Color(0xFFF48FB1)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.55f

        drawRoundRect(
            color = potDark,
            topLeft = Offset(cx - w * 0.30f, h * 0.74f),
            size = Size(w * 0.60f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = pot,
            topLeft = Offset(cx - w * 0.24f, h * 0.80f),
            size = Size(w * 0.48f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )

        fun leaf(angleDeg: Float, length: Float, widthScale: Float) {
            val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
            val tipX = cx + cos(angle) * length
            val tipY = cy + sin(angle) * length

            val outer = Path().apply {
                moveTo(cx, cy)
                quadraticTo(
                    cx + cos(angle - 0.6f) * length * widthScale,
                    cy + sin(angle - 0.6f) * length * widthScale,
                    tipX, tipY
                )
                quadraticTo(
                    cx + cos(angle + 0.6f) * length * widthScale,
                    cy + sin(angle + 0.6f) * length * widthScale,
                    cx, cy
                )
                close()
            }
            drawPath(outer, leafOuter)

            val inner = Path().apply {
                moveTo(cx, cy)
                quadraticTo(
                    cx + cos(angle - 0.45f) * length * widthScale * 0.72f,
                    cy + sin(angle - 0.45f) * length * widthScale * 0.72f,
                    cx + cos(angle) * length * 0.82f,
                    cy + sin(angle) * length * 0.82f
                )
                quadraticTo(
                    cx + cos(angle + 0.45f) * length * widthScale * 0.72f,
                    cy + sin(angle + 0.45f) * length * widthScale * 0.72f,
                    cx, cy
                )
                close()
            }
            drawPath(inner, leafInner)
        }

        leaf(-90f, h * 0.24f, 0.42f)
        leaf(-35f, h * 0.20f, 0.40f)
        leaf(-145f, h * 0.20f, 0.40f)

        if (stage >= 1) {
            leaf(8f, h * 0.16f, 0.34f)
            leaf(-188f, h * 0.16f, 0.34f)
        }
        if (stage >= 2) {
            leaf(-65f, h * 0.18f, 0.30f)
            leaf(-115f, h * 0.18f, 0.30f)
        }
        if (stage >= 3) {
            leaf(-15f, h * 0.14f, 0.28f)
            leaf(-165f, h * 0.14f, 0.28f)
        }

        drawCircle(
            color = centerColor,
            radius = w * 0.05f,
            center = Offset(cx, cy)
        )

        if (upgraded) {
            drawCircle(
                color = bloom,
                radius = w * 0.03f,
                center = Offset(cx, cy - h * 0.22f)
            )
            drawCircle(
                color = bloom,
                radius = w * 0.022f,
                center = Offset(cx - w * 0.05f, cy - h * 0.18f)
            )
            drawCircle(
                color = bloom,
                radius = w * 0.022f,
                center = Offset(cx + w * 0.05f, cy - h * 0.18f)
            )
        }
    }
}