package com.example.consolicalm

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        PlantType.CHERRY_BLOSSOM -> CherryBlossomPreview(stage, fullyUpgradedPreview, modifier)
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

        // pot
        drawRoundRect(
            color = potClayDark,
            topLeft = Offset(cx - w * 0.40f, h * 0.70f),
            size = Size(w * 0.80f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f),
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

        // trunk
        val trunk = Path().apply {
            moveTo(cx - w * 0.06f, h * 0.78f)
            cubicTo(cx - w * 0.22f, h * 0.62f, cx + w * 0.18f, h * 0.58f, cx + w * 0.02f, h * 0.48f)
            cubicTo(cx - w * 0.02f, h * 0.44f, cx + w * 0.28f, h * 0.46f, cx + w * 0.16f, h * 0.32f)
        }
        drawPath(trunk, trunkBrown, style = Stroke(w * 0.10f, cap = StrokeCap.Round))
        drawPath(trunk, trunkDark, style = Stroke(w * 0.04f, cap = StrokeCap.Round))

        fun pad(x: Float, y: Float, ww: Float, hh: Float) {
            drawRoundRect(greenDeep, Offset(x - ww / 2, y - hh / 2), Size(ww, hh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(hh / 2, hh / 2), style = Fill)
            drawRoundRect(green, Offset(x - ww * 0.42f, y - hh * 0.36f), Size(ww * 0.84f, hh * 0.78f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(hh / 2, hh / 2), style = Fill)
            drawRoundRect(greenLight, Offset(x + ww * 0.05f, y - hh * 0.45f), Size(ww * 0.42f, hh * 0.40f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(hh / 2, hh / 2), style = Fill)
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

    val pine = Color(0xFF1B5E20)      // deep green
    val pine2 = Color(0xFF2E7D32)     // main green
    val pineHi = Color(0xFF43A047)    // highlight

    val snow = Color(0xFFF3F6FA)      // soft snow white
    val snowShadow = Color(0xFFDCE4EE)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // snow ground only when fully upgraded preview
        if (upgraded) {
            drawRoundRect(
                color = snowShadow,
                topLeft = Offset(cx - w * 0.40f, h * 0.86f),
                size = Size(w * 0.80f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f),
                style = Fill
            )
            drawRoundRect(
                color = snow,
                topLeft = Offset(cx - w * 0.38f, h * 0.85f),
                size = Size(w * 0.76f, h * 0.10f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f, w * 0.10f),
                style = Fill
            )
        }

        // trunk
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

        // layered pine tiers (bigger with stage)
        val tiers = (2 + stage.coerceIn(0, 4)) // 2..6
        for (i in 0 until tiers) {
            val t = i / (tiers.toFloat())
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

            // snow caps only when fully upgraded preview
            if (upgraded) {
                val cap = Path().apply {
                    moveTo(cx, y - h * 0.075f)
                    lineTo(cx - halfW * 0.55f, y - h * 0.005f)
                    lineTo(cx + halfW * 0.55f, y - h * 0.005f)
                    close()
                }
                drawPath(cap, color = snowShadow, style = Fill)
                drawPath(
                    Path().apply {
                        moveTo(cx, y - h * 0.08f)
                        lineTo(cx - halfW * 0.52f, y - h * 0.01f)
                        lineTo(cx + halfW * 0.52f, y - h * 0.01f)
                        close()
                    },
                    color = snow,
                    style = Fill
                )
            }
        }
    }
}

@Composable
private fun FlowerPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val stem = Color(0xFF2E7D32)
    val petal = Color(0xFFEC407A)
    val petal2 = Color(0xFFF48FB1)
    val center = Color(0xFFFFD54F)
    val sparkle = Color(0xFFFFF176)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        drawLine(stem, Offset(cx, h * 0.78f), Offset(cx, h * 0.40f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        drawOval(stem, topLeft = Offset(cx - w * 0.22f, h * 0.58f), size = Size(w * 0.18f, h * 0.10f))
        drawOval(stem, topLeft = Offset(cx + w * 0.04f, h * 0.52f), size = Size(w * 0.18f, h * 0.10f))

        val r = w * (0.10f + stage * 0.02f)
        val headY = h * 0.30f
        for (i in 0..5) {
            val angle = i * (Math.PI.toFloat() / 3f)
            val px = cx + cos(angle) * r * 1.2f
            val py = headY + sin(angle) * r * 1.2f
            drawCircle(if (i % 2 == 0) petal else petal2, radius = r, center = Offset(px, py))
        }
        drawCircle(center, radius = r * 0.75f, center = Offset(cx, headY))

        if (upgraded) {
            // little sparkles
            drawCircle(sparkle, radius = w * 0.02f, center = Offset(cx - w * 0.22f, headY - h * 0.10f))
            drawCircle(sparkle, radius = w * 0.016f, center = Offset(cx + w * 0.24f, headY - h * 0.06f))
        }
    }
}

@Composable
private fun CactusPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val cactus = Color(0xFF2E7D32)
    val cactusDeep = Color(0xFF1B5E20)
    val pot = Color(0xFFB85A3D)
    val flower = Color(0xFFFF80AB)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        drawRoundRect(pot, Offset(cx - w * 0.26f, h * 0.74f), Size(w * 0.52f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f))

        val bodyH = h * (0.34f + stage * 0.03f)
        drawRoundRect(cactus, Offset(cx - w * 0.12f, h * 0.74f - bodyH), Size(w * 0.24f, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f))

        if (stage >= 2) {
            drawRoundRect(cactusDeep, Offset(cx - w * 0.28f, h * 0.56f), Size(w * 0.18f, h * 0.16f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f))
        }
        if (stage >= 3) {
            drawRoundRect(cactusDeep, Offset(cx + w * 0.10f, h * 0.52f), Size(w * 0.18f, h * 0.18f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, w * 0.12f))
        }

        if (upgraded) {
            drawCircle(flower, radius = w * 0.04f, center = Offset(cx, h * 0.36f))
        }
    }
}

@Composable
private fun CherryBlossomPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val trunk = Color(0xFF6B3F2A)
    val leaf = Color(0xFF2E7D32)
    val blossom = Color(0xFFF8BBD0)
    val blossom2 = Color(0xFFEC407A)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        drawLine(trunk, Offset(cx, h * 0.82f), Offset(cx, h * 0.42f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(trunk, Offset(cx, h * 0.52f), Offset(cx - w * 0.22f, h * 0.40f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)
        drawLine(trunk, Offset(cx, h * 0.48f), Offset(cx + w * 0.24f, h * 0.36f), strokeWidth = w * 0.05f, cap = StrokeCap.Round)

        if (stage <= 1) {
            drawCircle(leaf, radius = w * 0.18f, center = Offset(cx, h * 0.36f))
        } else {
            val count = (8 + stage * 6) + if (upgraded) 12 else 0
            for (i in 0 until count) {
                val t = i / count.toFloat()
                val x = cx + (sin(t * 12f) * w * 0.22f)
                val y = h * 0.30f + (sin(t * 9f) * h * 0.08f)
                drawCircle(if (i % 2 == 0) blossom else blossom2, radius = w * 0.03f, center = Offset(x, y))
            }
        }
    }
}

