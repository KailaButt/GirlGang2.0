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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

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

// ─── Insect Helpers ───────────────────────────────────────────────────────────

private fun drawLadybug(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    cx: Float, cy: Float, radius: Float
) = with(drawScope) {
    // shadow
    drawOval(Color(0x22000000), Offset(cx - radius, cy - radius * 0.3f + radius * 1.6f), Size(radius * 2, radius * 0.6f))
    // shell halves
    val shell = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - radius, cy - radius * 0.8f, cx + radius, cy + radius))
    }
    drawPath(shell, Color(0xFFD32F2F))
    // center divider line
    drawLine(Color(0xFF8B0000), Offset(cx, cy - radius * 0.8f), Offset(cx, cy + radius), strokeWidth = radius * 0.18f)
    // head
    drawCircle(Color(0xFF1A1A1A), radius * 0.42f, Offset(cx, cy - radius * 0.92f))
    // white eye dots on head
    drawCircle(Color(0xFFFFFFFF), radius * 0.12f, Offset(cx - radius * 0.18f, cy - radius * 1.0f))
    drawCircle(Color(0xFFFFFFFF), radius * 0.12f, Offset(cx + radius * 0.18f, cy - radius * 1.0f))
    // black spots on shell
    drawCircle(Color(0xFF1A1A1A), radius * 0.22f, Offset(cx - radius * 0.50f, cy - radius * 0.28f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.22f, Offset(cx + radius * 0.50f, cy - radius * 0.28f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.18f, Offset(cx - radius * 0.46f, cy + radius * 0.32f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.18f, Offset(cx + radius * 0.46f, cy + radius * 0.32f))
    // tiny antennae
    drawLine(Color(0xFF1A1A1A), Offset(cx - radius * 0.10f, cy - radius * 1.28f), Offset(cx - radius * 0.32f, cy - radius * 1.62f), strokeWidth = radius * 0.10f)
    drawLine(Color(0xFF1A1A1A), Offset(cx + radius * 0.10f, cy - radius * 1.28f), Offset(cx + radius * 0.32f, cy - radius * 1.62f), strokeWidth = radius * 0.10f)
    drawCircle(Color(0xFF1A1A1A), radius * 0.11f, Offset(cx - radius * 0.34f, cy - radius * 1.65f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.11f, Offset(cx + radius * 0.34f, cy - radius * 1.65f))
}

private fun drawBee(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    cx: Float, cy: Float, radius: Float
) = with(drawScope) {
    // wings
    val wingPath = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx - radius * 1.4f, cy - radius * 1.6f, cx - radius * 2.2f, cy - radius * 0.4f, cx - radius * 0.8f, cy + radius * 0.1f)
        close()
    }
    drawPath(wingPath, Color(0xAAC8E6FA))
    val wingPathR = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx + radius * 1.4f, cy - radius * 1.6f, cx + radius * 2.2f, cy - radius * 0.4f, cx + radius * 0.8f, cy + radius * 0.1f)
        close()
    }
    drawPath(wingPathR, Color(0xAAC8E6FA))
    // body stripes
    val body = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - radius * 0.7f, cy - radius * 0.5f, cx + radius * 0.7f, cy + radius * 1.2f))
    }
    drawPath(body, Color(0xFFF9A825))
    // stripes
    for (i in 0..1) {
        val sy = cy + radius * (0.1f + i * 0.52f)
        drawLine(Color(0xFF1A1A1A), Offset(cx - radius * 0.62f, sy), Offset(cx + radius * 0.62f, sy), strokeWidth = radius * 0.24f)
    }
    // head
    drawCircle(Color(0xFF1A1A1A), radius * 0.42f, Offset(cx, cy - radius * 0.62f))
    // eyes
    drawCircle(Color(0xFFFFFFFF), radius * 0.14f, Offset(cx - radius * 0.18f, cy - radius * 0.68f))
    drawCircle(Color(0xFFFFFFFF), radius * 0.14f, Offset(cx + radius * 0.18f, cy - radius * 0.68f))
    // stinger
    drawLine(Color(0xFF5D4037), Offset(cx, cy + radius * 1.2f), Offset(cx, cy + radius * 1.55f), strokeWidth = radius * 0.18f, cap = StrokeCap.Round)
    // antennae
    drawLine(Color(0xFF1A1A1A), Offset(cx - radius * 0.12f, cy - radius * 0.98f), Offset(cx - radius * 0.5f, cy - radius * 1.55f), strokeWidth = radius * 0.10f)
    drawLine(Color(0xFF1A1A1A), Offset(cx + radius * 0.12f, cy - radius * 0.98f), Offset(cx + radius * 0.5f, cy - radius * 1.55f), strokeWidth = radius * 0.10f)
    drawCircle(Color(0xFFF9A825), radius * 0.13f, Offset(cx - radius * 0.52f, cy - radius * 1.58f))
    drawCircle(Color(0xFFF9A825), radius * 0.13f, Offset(cx + radius * 0.52f, cy - radius * 1.58f))
}

private fun drawButterfly(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    cx: Float, cy: Float, radius: Float
) = with(drawScope) {
    // upper wings
    val ulWing = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx - radius * 1.8f, cy - radius * 2.0f, cx - radius * 2.6f, cy + radius * 0.2f, cx, cy + radius * 0.4f)
        close()
    }
    drawPath(ulWing, Color(0xFFFF7043))
    drawPath(ulWing, Color(0xFF1A1A1A), style = Stroke(radius * 0.14f))
    val urWing = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx + radius * 1.8f, cy - radius * 2.0f, cx + radius * 2.6f, cy + radius * 0.2f, cx, cy + radius * 0.4f)
        close()
    }
    drawPath(urWing, Color(0xFFFF7043))
    drawPath(urWing, Color(0xFF1A1A1A), style = Stroke(radius * 0.14f))
    // lower wings
    val llWing = Path().apply {
        moveTo(cx, cy + radius * 0.4f)
        cubicTo(cx - radius * 1.4f, cy + radius * 1.2f, cx - radius * 1.0f, cy + radius * 2.0f, cx, cy + radius * 1.6f)
        close()
    }
    drawPath(llWing, Color(0xFFFFB300))
    val lrWing = Path().apply {
        moveTo(cx, cy + radius * 0.4f)
        cubicTo(cx + radius * 1.4f, cy + radius * 1.2f, cx + radius * 1.0f, cy + radius * 2.0f, cx, cy + radius * 1.6f)
        close()
    }
    drawPath(lrWing, Color(0xFFFFB300))
    // white spots on upper wings
    drawCircle(Color(0x99FFFFFF), radius * 0.22f, Offset(cx - radius * 1.1f, cy - radius * 0.8f))
    drawCircle(Color(0x99FFFFFF), radius * 0.18f, Offset(cx + radius * 1.1f, cy - radius * 0.8f))
    // body
    drawLine(Color(0xFF1A1A1A), Offset(cx, cy - radius * 0.4f), Offset(cx, cy + radius * 1.6f), strokeWidth = radius * 0.24f, cap = StrokeCap.Round)
    // antennae
    drawLine(Color(0xFF1A1A1A), Offset(cx, cy - radius * 0.4f), Offset(cx - radius * 0.6f, cy - radius * 1.6f), strokeWidth = radius * 0.10f)
    drawLine(Color(0xFF1A1A1A), Offset(cx, cy - radius * 0.4f), Offset(cx + radius * 0.6f, cy - radius * 1.6f), strokeWidth = radius * 0.10f)
    drawCircle(Color(0xFF1A1A1A), radius * 0.14f, Offset(cx - radius * 0.62f, cy - radius * 1.64f))
    drawCircle(Color(0xFF1A1A1A), radius * 0.14f, Offset(cx + radius * 0.62f, cy - radius * 1.64f))
}

// ─── Bonsai ───────────────────────────────────────────────────────────────────

@Composable
private fun JuniperBonsaiPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    // Realistic aged bonsai with detailed bark texture and layered foliage pads
    val potTerracotta = Color(0xFFC1714F)
    val potShadow = Color(0xFF8B4A30)
    val potRim = Color(0xFFD4855F)
    val trailDark = Color(0xFF3E2211)
    val trailMid = Color(0xFF5C3218)
    val trailLight = Color(0xFF7A4A2C)
    val moss = Color(0xFF5D7C3A)
    val mossDark = Color(0xFF3A5220)
    val mossLight = Color(0xFF7BAA4A)
    val foliage1 = Color(0xFF2A6B2E)
    val foliage2 = Color(0xFF3D8C41)
    val foliage3 = Color(0xFF1E5222)
    val foliageHi = Color(0xFF56B05C)
    val berryBlue = Color(0xFF4682B4)
    val berryDark = Color(0xFF2C5F8C)
    val soilDark = Color(0xFF3B2208)
    val soilMid = Color(0xFF5A3618)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // --- Pot shadow on ground ---
        drawOval(
            Color(0x33000000),
            Offset(cx - w * 0.38f, h * 0.93f),
            Size(w * 0.76f, h * 0.06f)
        )

        // --- Pot drainage tray ---
        drawRoundRect(
            Color(0xFF7A4828),
            Offset(cx - w * 0.38f, h * 0.90f),
            Size(w * 0.76f, h * 0.05f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // --- Pot rim (top band) ---
        drawRoundRect(
            potRim,
            Offset(cx - w * 0.37f, h * 0.71f),
            Size(w * 0.74f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            potShadow,
            Offset(cx - w * 0.36f, h * 0.74f),
            Size(w * 0.72f, h * 0.05f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // --- Pot body with trapezoidal shape ---
        val potBody = Path().apply {
            moveTo(cx - w * 0.32f, h * 0.79f)
            lineTo(cx + w * 0.32f, h * 0.79f)
            lineTo(cx + w * 0.26f, h * 0.91f)
            lineTo(cx - w * 0.26f, h * 0.91f)
            close()
        }
        drawPath(potBody, potTerracotta)
        // Pot highlight stripe
        val potHighlight = Path().apply {
            moveTo(cx - w * 0.32f, h * 0.79f)
            lineTo(cx - w * 0.20f, h * 0.79f)
            lineTo(cx - w * 0.16f, h * 0.91f)
            lineTo(cx - w * 0.26f, h * 0.91f)
            close()
        }
        drawPath(potHighlight, potRim.copy(alpha = 0.5f))
        // Pot shadow stripe
        val potShade = Path().apply {
            moveTo(cx + w * 0.18f, h * 0.79f)
            lineTo(cx + w * 0.32f, h * 0.79f)
            lineTo(cx + w * 0.26f, h * 0.91f)
            lineTo(cx + w * 0.14f, h * 0.91f)
            close()
        }
        drawPath(potShade, potShadow.copy(alpha = 0.5f))

        // --- Soil surface with moss patches ---
        drawRoundRect(
            soilDark,
            Offset(cx - w * 0.34f, h * 0.70f),
            Size(w * 0.68f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawRoundRect(
            soilMid,
            Offset(cx - w * 0.30f, h * 0.70f),
            Size(w * 0.60f, h * 0.06f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Moss patches on soil
        drawOval(moss, Offset(cx - w * 0.28f, h * 0.69f), Size(w * 0.22f, h * 0.05f))
        drawOval(mossDark, Offset(cx - w * 0.25f, h * 0.69f), Size(w * 0.14f, h * 0.03f))
        drawOval(mossLight.copy(alpha = 0.6f), Offset(cx + w * 0.04f, h * 0.70f), Size(w * 0.18f, h * 0.04f))

        // --- Main trunk with realistic curves ---
        // Trunk base
        val trunkBase = Path().apply {
            moveTo(cx - w * 0.05f, h * 0.76f)
            lineTo(cx + w * 0.05f, h * 0.76f)
            lineTo(cx + w * 0.12f, h * 0.52f)
            lineTo(cx + w * 0.04f, h * 0.50f)
            close()
        }
        drawPath(trunkBase, trailDark)

        // Main trunk stroke
        val trunk = Path().apply {
            moveTo(cx - w * 0.02f, h * 0.76f)
            cubicTo(cx - w * 0.18f, h * 0.64f, cx + w * 0.20f, h * 0.56f, cx + w * 0.08f, h * 0.44f)
            cubicTo(cx + w * 0.04f, h * 0.40f, cx + w * 0.28f, h * 0.42f, cx + w * 0.18f, h * 0.30f)
        }
        drawPath(trunk, trailMid, style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(trunk, trailLight, style = Stroke(w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Bark texture lines
        drawPath(trunk, trailDark.copy(alpha = 0.45f), style = Stroke(w * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Left branch
        val branch1 = Path().apply {
            moveTo(cx - w * 0.06f, h * 0.58f)
            cubicTo(cx - w * 0.22f, h * 0.52f, cx - w * 0.30f, h * 0.48f, cx - w * 0.26f, h * 0.42f)
        }
        drawPath(branch1, trailMid, style = Stroke(w * 0.05f, cap = StrokeCap.Round))
        drawPath(branch1, trailLight, style = Stroke(w * 0.02f, cap = StrokeCap.Round))

        // Right lower branch
        if (stage >= 2) {
            val branch2 = Path().apply {
                moveTo(cx + w * 0.08f, h * 0.62f)
                cubicTo(cx + w * 0.24f, h * 0.56f, cx + w * 0.34f, h * 0.52f, cx + w * 0.30f, h * 0.46f)
            }
            drawPath(branch2, trailMid, style = Stroke(w * 0.04f, cap = StrokeCap.Round))
            drawPath(branch2, trailLight, style = Stroke(w * 0.018f, cap = StrokeCap.Round))
        }

        // --- Foliage pads ---
        fun foliagePad(x: Float, y: Float, rw: Float, rh: Float, flip: Boolean = false) {
            // dark base shadow
            drawRoundRect(
                foliage3,
                Offset(x - rw * 0.5f, y - rh * 0.5f + rh * 0.1f),
                Size(rw, rh),
                cornerRadius = CornerRadius(rh * 0.48f, rh * 0.48f)
            )
            // main pad
            drawRoundRect(
                foliage1,
                Offset(x - rw * 0.5f, y - rh * 0.5f),
                Size(rw, rh),
                cornerRadius = CornerRadius(rh * 0.48f, rh * 0.48f)
            )
            // mid highlight
            drawRoundRect(
                foliage2,
                Offset(x - rw * 0.42f, y - rh * 0.40f),
                Size(rw * 0.84f, rh * 0.72f),
                cornerRadius = CornerRadius(rh * 0.38f, rh * 0.38f)
            )
            // top specular
            val hiX = if (flip) x - rw * 0.20f else x + rw * 0.08f
            drawRoundRect(
                foliageHi,
                Offset(hiX, y - rh * 0.48f),
                Size(rw * 0.38f, rh * 0.36f),
                cornerRadius = CornerRadius(rh * 0.28f, rh * 0.28f)
            )
        }

        // Base crown pads
        foliagePad(cx + w * 0.18f, h * 0.40f, w * 0.46f, h * 0.20f)
        foliagePad(cx + w * 0.32f, h * 0.46f, w * 0.32f, h * 0.15f)

        if (stage >= 1) foliagePad(cx - w * 0.06f, h * 0.50f, w * 0.30f, h * 0.14f, flip = true)
        if (stage >= 2) foliagePad(cx + w * 0.12f, h * 0.30f, w * 0.26f, h * 0.13f)
        if (stage >= 3) foliagePad(cx - w * 0.22f, h * 0.44f, w * 0.24f, h * 0.12f, flip = true)
        if (stage >= 4) foliagePad(cx + w * 0.28f, h * 0.35f, w * 0.22f, h * 0.11f)

        // Juniper berries on foliage
        if (upgraded) {
            for ((bx, by) in listOf(
                cx + w * 0.12f to h * 0.36f,
                cx + w * 0.24f to h * 0.38f,
                cx + w * 0.30f to h * 0.44f,
                cx + w * 0.06f to h * 0.46f
            )) {
                drawCircle(berryDark, w * 0.028f, Offset(bx, by))
                drawCircle(berryBlue, w * 0.022f, Offset(bx, by))
                drawCircle(Color(0xAAB8D4EE), w * 0.010f, Offset(bx - w * 0.006f, by - w * 0.006f))
            }
            // Ladybug on a branch
            drawLadybug(this, cx - w * 0.14f, h * 0.52f, w * 0.045f)
        }
    }
}

// ─── Mountain Pine ────────────────────────────────────────────────────────────

@Composable
private fun MountainPinePreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val trunk = Color(0xFF5C3418)
    val trunkLight = Color(0xFF7A4C28)
    val trunkDark = Color(0xFF3B2010)
    val pine1 = Color(0xFF1A4F1E)
    val pine2 = Color(0xFF2D6E32)
    val pine3 = Color(0xFF3E8E45)
    val pineHi = Color(0xFF52B25A)
    val snow = Color(0xFFF0F5FA)
    val snowShadow = Color(0xFFCDD8E8)
    val snowBlue = Color(0xFFD0DCF0)
    val ground = Color(0xFF5C7A3A)
    val groundDark = Color(0xFF3A5222)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // Ground / snow base
        if (upgraded) {
            drawRoundRect(
                snowBlue,
                Offset(cx - w * 0.44f, h * 0.87f),
                Size(w * 0.88f, h * 0.10f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
            )
            drawRoundRect(
                snowShadow,
                Offset(cx - w * 0.42f, h * 0.86f),
                Size(w * 0.84f, h * 0.08f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
            )
            drawRoundRect(
                snow,
                Offset(cx - w * 0.40f, h * 0.84f),
                Size(w * 0.80f, h * 0.08f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
            )
        } else {
            // grass mound
            drawOval(groundDark, Offset(cx - w * 0.42f, h * 0.87f), Size(w * 0.84f, h * 0.10f))
            drawOval(ground, Offset(cx - w * 0.40f, h * 0.85f), Size(w * 0.80f, h * 0.08f))
        }

        // Trunk with bark detail
        drawLine(trunkDark, Offset(cx + w * 0.012f, h * 0.84f), Offset(cx + w * 0.012f, h * 0.46f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
        drawLine(trunkLight, Offset(cx - w * 0.012f, h * 0.84f), Offset(cx - w * 0.012f, h * 0.50f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
        drawLine(trunk, Offset(cx, h * 0.84f), Offset(cx, h * 0.46f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        // bark notches
        for (i in 0..3) {
            val ny = h * (0.55f + i * 0.07f)
            drawLine(trunkDark, Offset(cx - w * 0.02f, ny), Offset(cx + w * 0.04f, ny + h * 0.015f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
        }

        val tiers = 2 + stage.coerceIn(0, 4)
        for (i in 0 until tiers) {
            val t = i.toFloat() / tiers.toFloat()
            val y = h * (0.80f - i * 0.10f - i * 0.02f)
            val halfW = w * (0.40f - i * 0.055f)

            // Drop shadow
            val shadow = Path().apply {
                moveTo(cx, y - h * 0.10f)
                lineTo(cx - halfW * 1.05f, y + h * 0.06f)
                lineTo(cx + halfW * 1.05f, y + h * 0.06f)
                close()
            }
            drawPath(shadow, Color(0x28000000))

            // Main tier — three-tone for depth
            val back = Path().apply {
                moveTo(cx, y - h * 0.10f)
                lineTo(cx - halfW, y + h * 0.06f)
                lineTo(cx + halfW, y + h * 0.06f)
                close()
            }
            drawPath(back, pine1)

            val mid = Path().apply {
                moveTo(cx, y - h * 0.10f)
                lineTo(cx - halfW * 0.75f, y + h * 0.04f)
                lineTo(cx + halfW * 0.75f, y + h * 0.04f)
                close()
            }
            drawPath(mid, pine2)

            val front = Path().apply {
                moveTo(cx, y - h * 0.10f)
                lineTo(cx - halfW * 0.40f, y + h * 0.01f)
                lineTo(cx + halfW * 0.40f, y + h * 0.01f)
                close()
            }
            drawPath(front, pine3)

            // Edge highlight tips (needle-like)
            for (side in listOf(-1f, 1f)) {
                for (tip in 0..2) {
                    val tx = cx + side * halfW * (0.55f + tip * 0.15f)
                    val ty = y + h * (0.00f + tip * 0.02f)
                    val tipPath = Path().apply {
                        moveTo(tx - side * w * 0.04f, ty)
                        lineTo(tx, ty - h * 0.03f)
                        lineTo(tx + side * w * 0.02f, ty + h * 0.02f)
                        close()
                    }
                    drawPath(tipPath, pineHi)
                }
            }

            if (upgraded) {
                // Snow cap with blue-shadow layer
                val capShadow = Path().apply {
                    moveTo(cx, y - h * 0.102f)
                    lineTo(cx - halfW * 0.56f, y - h * 0.002f)
                    lineTo(cx + halfW * 0.56f, y - h * 0.002f)
                    close()
                }
                drawPath(capShadow, snowBlue)

                val cap = Path().apply {
                    moveTo(cx, y - h * 0.10f)
                    lineTo(cx - halfW * 0.52f, y - h * 0.01f)
                    lineTo(cx + halfW * 0.52f, y - h * 0.01f)
                    close()
                }
                drawPath(cap, snow)

                // Snow drips on edges
                drawLine(snow, Offset(cx - halfW * 0.50f, y - h * 0.01f), Offset(cx - halfW * 0.58f, y + h * 0.03f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
                drawLine(snow, Offset(cx + halfW * 0.50f, y - h * 0.01f), Offset(cx + halfW * 0.58f, y + h * 0.03f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
            }
        }

        if (upgraded) {
            // Bee hovering near top
            drawBee(this, cx + w * 0.30f, h * 0.18f, w * 0.045f)
        }
    }
}

// ─── Flower ───────────────────────────────────────────────────────────────────

@Composable
private fun FlowerPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val stemGreen = Color(0xFF2E7D32)
    val stemMid = Color(0xFF43A047)
    val leafColor = Color(0xFF388E3C)
    val leafVein = Color(0xFF2E7D32)
    val leafLight = Color(0xFF66BB6A)
    val petalBase = Color(0xFFE91E8C)
    val petalMid = Color(0xFFF06292)
    val petalTip = Color(0xFFF8BBD0)
    val centerYolk = Color(0xFFFDD835)
    val centerRing = Color(0xFFF9A825)
    val centerDot = Color(0xFF5D4037)
    val dewColor = Color(0xCCFFFFFF)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // Ground shadow
        drawOval(Color(0x22000000), Offset(cx - w * 0.20f, h * 0.93f), Size(w * 0.40f, h * 0.05f))

        // Pot (small terracotta)
        drawRoundRect(Color(0xFFB85C42), Offset(cx - w * 0.22f, h * 0.78f), Size(w * 0.44f, h * 0.08f), cornerRadius = CornerRadius(w * 0.04f, w * 0.04f))
        val potBody2 = Path().apply {
            moveTo(cx - w * 0.20f, h * 0.86f)
            lineTo(cx + w * 0.20f, h * 0.86f)
            lineTo(cx + w * 0.16f, h * 0.94f)
            lineTo(cx - w * 0.16f, h * 0.94f)
            close()
        }
        drawPath(potBody2, Color(0xFFD4775A))
        drawRoundRect(Color(0xFF4A2E14), Offset(cx - w * 0.18f, h * 0.76f), Size(w * 0.36f, h * 0.04f), cornerRadius = CornerRadius(w * 0.03f))

        // Stem with curvature
        val stemPath = Path().apply {
            moveTo(cx, h * 0.80f)
            cubicTo(cx - w * 0.04f, h * 0.68f, cx + w * 0.04f, h * 0.58f, cx, h * 0.46f)
        }
        drawPath(stemPath, stemGreen, style = Stroke(w * 0.062f, cap = StrokeCap.Round))
        drawPath(stemPath, stemMid, style = Stroke(w * 0.030f, cap = StrokeCap.Round))

        // Left leaf — teardrop with vein
        val leftLeaf = Path().apply {
            moveTo(cx, h * 0.66f)
            cubicTo(cx - w * 0.05f, h * 0.60f, cx - w * 0.28f, h * 0.56f, cx - w * 0.20f, h * 0.72f)
            cubicTo(cx - w * 0.12f, h * 0.78f, cx, h * 0.70f, cx, h * 0.66f)
            close()
        }
        drawPath(leftLeaf, leafColor)
        drawLine(leafVein, Offset(cx, h * 0.66f), Offset(cx - w * 0.18f, h * 0.70f), strokeWidth = w * 0.016f, cap = StrokeCap.Round)
        drawLine(leafLight, Offset(cx - w * 0.04f, h * 0.64f), Offset(cx - w * 0.14f, h * 0.62f), strokeWidth = w * 0.010f, cap = StrokeCap.Round)

        if (stage >= 2) {
            val rightLeaf = Path().apply {
                moveTo(cx, h * 0.58f)
                cubicTo(cx + w * 0.04f, h * 0.52f, cx + w * 0.26f, h * 0.48f, cx + w * 0.18f, h * 0.64f)
                cubicTo(cx + w * 0.10f, h * 0.70f, cx, h * 0.62f, cx, h * 0.58f)
                close()
            }
            drawPath(rightLeaf, leafColor)
            drawLine(leafVein, Offset(cx, h * 0.58f), Offset(cx + w * 0.16f, h * 0.62f), strokeWidth = w * 0.016f, cap = StrokeCap.Round)
        }

        val bloomScale = 0.92f + stage.coerceIn(0, 4) * 0.06f

        // Petals — 8 petals arranged in a circle
        val petalCount = if (stage >= 3) 8 else if (stage >= 1) 6 else 4
        val petalLen = h * 0.14f * bloomScale
        val flowerCX = cx
        val flowerCY = h * 0.36f

        for (i in 0 until petalCount) {
            val angleDeg = i * (360f / petalCount) - 90f
            val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
            val px = flowerCX + cos(angle) * petalLen
            val py = flowerCY + sin(angle) * petalLen

            val petal = Path().apply {
                moveTo(flowerCX, flowerCY)
                cubicTo(
                    flowerCX + cos(angle - 0.5f) * petalLen * 0.6f,
                    flowerCY + sin(angle - 0.5f) * petalLen * 0.6f,
                    px + cos(angle - 0.4f) * petalLen * 0.3f,
                    py + sin(angle - 0.4f) * petalLen * 0.3f,
                    px, py
                )
                cubicTo(
                    px + cos(angle + 0.4f) * petalLen * 0.3f,
                    py + sin(angle + 0.4f) * petalLen * 0.3f,
                    flowerCX + cos(angle + 0.5f) * petalLen * 0.6f,
                    flowerCY + sin(angle + 0.5f) * petalLen * 0.6f,
                    flowerCX, flowerCY
                )
                close()
            }
            val petalCol = when {
                i % 3 == 0 -> petalBase
                i % 3 == 1 -> petalMid
                else -> petalTip
            }
            drawPath(petal, petalCol)
        }

        // Center disk — layered rings
        drawCircle(centerDot, w * 0.090f, Offset(flowerCX, flowerCY))
        drawCircle(centerRing, w * 0.075f, Offset(flowerCX, flowerCY))
        drawCircle(centerYolk, w * 0.058f, Offset(flowerCX, flowerCY))
        // Center texture dots
        for (i in 0..5) {
            val a = i * 60f
            val ar = Math.toRadians(a.toDouble()).toFloat()
            drawCircle(centerDot, w * 0.012f, Offset(flowerCX + cos(ar) * w * 0.038f, flowerCY + sin(ar) * w * 0.038f))
        }
        drawCircle(centerDot, w * 0.016f, Offset(flowerCX, flowerCY))

        if (upgraded) {
            // Dew drops on petals
            drawCircle(dewColor, w * 0.020f, Offset(flowerCX + w * 0.10f, flowerCY - h * 0.10f))
            drawCircle(dewColor, w * 0.016f, Offset(flowerCX - w * 0.08f, flowerCY - h * 0.12f))
            drawCircle(dewColor, w * 0.013f, Offset(flowerCX + w * 0.12f, flowerCY + h * 0.06f))
            // Butterfly near flower
            drawButterfly(this, cx + w * 0.36f, h * 0.26f, w * 0.038f)
        }
    }
}

// ─── Cactus ───────────────────────────────────────────────────────────────────

@Composable
private fun CactusPreview(stage: Int, upgraded: Boolean, modifier: Modifier) {
    val potClay = Color(0xFFBF6A44)
    val potDark = Color(0xFF8C4428)
    val potRim2 = Color(0xFFD4845A)
    val sand = Color(0xFFD4AA6A)
    val sandDark = Color(0xFFAA8846)
    val bodyDark = Color(0xFF2E7D32)
    val bodyMid = Color(0xFF43A047)
    val bodyLight = Color(0xFF66BB6A)
    val bodyHi = Color(0xFF88CC8A)
    val spineColor = Color(0xFFF5ECD0)
    val spineBase = Color(0xFFD4C490)
    val bloomPink = Color(0xFFFF4081)
    val bloomYellow = Color(0xFFFDD835)
    val bloomOrange = Color(0xFFFF6D00)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // Ground shadow
        drawOval(Color(0x2A000000), Offset(cx - w * 0.32f, h * 0.94f), Size(w * 0.64f, h * 0.05f))

        // Pot rim
        drawRoundRect(potRim2, Offset(cx - w * 0.28f, h * 0.73f), Size(w * 0.56f, h * 0.06f), cornerRadius = CornerRadius(w * 0.05f))
        drawRoundRect(potDark, Offset(cx - w * 0.26f, h * 0.76f), Size(w * 0.52f, h * 0.03f), cornerRadius = CornerRadius(w * 0.03f))
        // Pot body
        val potB = Path().apply {
            moveTo(cx - w * 0.24f, h * 0.79f)
            lineTo(cx + w * 0.24f, h * 0.79f)
            lineTo(cx + w * 0.19f, h * 0.93f)
            lineTo(cx - w * 0.19f, h * 0.93f)
            close()
        }
        drawPath(potB, potClay)
        val potBHi = Path().apply {
            moveTo(cx - w * 0.24f, h * 0.79f)
            lineTo(cx - w * 0.14f, h * 0.79f)
            lineTo(cx - w * 0.10f, h * 0.93f)
            lineTo(cx - w * 0.19f, h * 0.93f)
            close()
        }
        drawPath(potBHi, potRim2.copy(alpha = 0.4f))

        // Sandy soil
        drawRoundRect(sandDark, Offset(cx - w * 0.24f, h * 0.71f), Size(w * 0.48f, h * 0.08f), cornerRadius = CornerRadius(w * 0.04f))
        drawRoundRect(sand, Offset(cx - w * 0.22f, h * 0.71f), Size(w * 0.44f, h * 0.05f), cornerRadius = CornerRadius(w * 0.03f))
        // Pebbles on soil
        for ((px, py, pr) in listOf(
            Triple(cx - w * 0.12f, h * 0.73f, w * 0.022f),
            Triple(cx + w * 0.08f, h * 0.74f, w * 0.018f),
            Triple(cx - w * 0.04f, h * 0.74f, w * 0.014f)
        )) {
            drawCircle(sandDark, pr, Offset(px, py))
            drawCircle(sand.copy(alpha = 0.7f), pr * 0.7f, Offset(px - pr * 0.2f, py - pr * 0.2f))
        }

        // Helper to draw a rounded cactus arm/column
        fun cactusColumn(left: Float, top: Float, ww: Float, hh: Float, isArm: Boolean = false) {
            val rr = ww * 0.48f
            // Dark back
            drawRoundRect(bodyDark, Offset(left + ww * 0.06f, top + hh * 0.04f), Size(ww * 0.94f, hh * 0.94f), cornerRadius = CornerRadius(rr, rr))
            // Main body
            drawRoundRect(bodyMid, Offset(left, top), Size(ww, hh), cornerRadius = CornerRadius(rr, rr))
            // Left shade strip
            drawRoundRect(bodyDark.copy(alpha = 0.45f), Offset(left, top + hh * 0.1f), Size(ww * 0.22f, hh * 0.80f), cornerRadius = CornerRadius(rr, rr))
            // Right highlight strip
            drawRoundRect(bodyLight, Offset(left + ww * 0.28f, top + hh * 0.08f), Size(ww * 0.44f, hh * 0.78f), cornerRadius = CornerRadius(rr * 0.8f, rr * 0.8f))
            // Top shine
            drawRoundRect(bodyHi, Offset(left + ww * 0.34f, top + hh * 0.04f), Size(ww * 0.30f, hh * 0.22f), cornerRadius = CornerRadius(rr * 0.6f, rr * 0.6f))
            // Vertical ribs
            for (rib in 1..2) {
                val rx = left + ww * (0.28f + rib * 0.18f)
                drawLine(bodyDark.copy(alpha = 0.18f), Offset(rx, top + hh * 0.08f), Offset(rx, top + hh * 0.92f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
            }
            // Spines
            val spineRows = (hh / (h * 0.12f)).toInt().coerceAtLeast(1)
            for (row in 0 until spineRows) {
                val sy = top + hh * ((row + 0.5f) / spineRows)
                for (side in listOf(-1f, 1f)) {
                    val sx = if (side < 0) left + ww * 0.08f else left + ww * 0.92f
                    // spine base dot
                    drawCircle(spineBase, w * 0.018f, Offset(sx, sy))
                    // spine line
                    drawLine(spineColor, Offset(sx, sy), Offset(sx + side * w * 0.06f, sy - h * 0.018f), strokeWidth = w * 0.018f, cap = StrokeCap.Round)
                    drawLine(spineColor, Offset(sx, sy), Offset(sx + side * w * 0.04f, sy + h * 0.015f), strokeWidth = w * 0.014f, cap = StrokeCap.Round)
                }
            }
        }

        // Main trunk
        cactusColumn(cx - w * 0.13f, h * 0.24f, w * 0.26f, h * 0.48f)

        // Arms
        if (stage >= 1) {
            // Right arm with elbow
            cactusColumn(cx + w * 0.14f, h * 0.34f, w * 0.16f, h * 0.08f) // elbow
            cactusColumn(cx + w * 0.18f, h * 0.22f, w * 0.16f, h * 0.20f)  // upper
        }
        if (stage >= 2) {
            // Left arm
            cactusColumn(cx - w * 0.30f, h * 0.38f, w * 0.16f, h * 0.06f)
            cactusColumn(cx - w * 0.32f, h * 0.22f, w * 0.16f, h * 0.22f)
        }
        if (stage >= 3) {
            // Right lower arm nub
            cactusColumn(cx + w * 0.10f, h * 0.48f, w * 0.12f, h * 0.04f)
            cactusColumn(cx + w * 0.13f, h * 0.38f, w * 0.12f, h * 0.14f)
        }
        if (stage >= 4) {
            // Left lower nub
            cactusColumn(cx - w * 0.22f, h * 0.50f, w * 0.12f, h * 0.04f)
            cactusColumn(cx - w * 0.24f, h * 0.40f, w * 0.12f, h * 0.14f)
        }

        if (upgraded) {
            // Bloom flower on top
            val bx = cx
            val by = h * 0.22f
            for (i in 0..4) {
                val a = Math.toRadians((i * 72.0)).toFloat()
                val px = bx + cos(a) * w * 0.08f
                val py = by + sin(a) * w * 0.08f
                val petalP = Path().apply {
                    moveTo(bx, by)
                    cubicTo(bx + cos(a - 0.5f) * w * 0.06f, by + sin(a - 0.5f) * w * 0.06f, px, py, px, py)
                    cubicTo(px, py, bx + cos(a + 0.5f) * w * 0.06f, by + sin(a + 0.5f) * w * 0.06f, bx, by)
                    close()
                }
                drawPath(petalP, bloomPink)
            }
            drawCircle(bloomOrange, w * 0.036f, Offset(bx, by))
            drawCircle(bloomYellow, w * 0.024f, Offset(bx, by))

            // Bee visiting the bloom
            drawBee(this, bx + w * 0.26f, by - h * 0.06f, w * 0.040f)
        }
    }
}