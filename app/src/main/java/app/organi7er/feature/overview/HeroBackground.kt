package app.organi7er.feature.overview

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ------------------------------------------------------------------ Einstellbare Werte

/** Rundung der unteren Ecken des dunklen Kopfbereichs. */
private val HeroRadius = 36.dp

/** Verlauf von oben (fast schwarz) nach unten (etwas heller). */
private val HeroTop = Color(0xFF000000)
private val HeroMid = Color(0xFF09090B)
private val HeroBottom = Color(0xFF121316)

/** Lage des Horizonts der Punktlandschaft (0 = oben, 1 = unten im Kopfbereich). */
private const val HorizonAt = 0.56f

/** Abstand der Punkte in der vordersten Reihe (dp) – kleiner = dichteres Raster. */
private const val DotSpacing = 8f

/** Größte Deckkraft der Punkte (vorne); nach hinten wird es schwächer. */
private const val DotAlpha = 0.42f

/** Unten (hinter der KW-Zeile) werden die Punkte über diese Höhe (dp) ausgeblendet. */
private const val BottomFade = 90f

/** Höhe der Hügel (Anteil der Kamerahöhe). 0 = flache Ebene. */
private const val WaveHeight = 0.32f

/**
 * Dunkler Hintergrund des Kopfbereichs: Verlauf, weicher Schimmer und unten eine
 * Landschaft aus Punkten in Perspektive (wie ein gepunktetes Gelände bis zum Horizont).
 * Unten abgerundet, oben läuft er bis an die Bildschirmkante.
 */
internal fun Modifier.heroBackground(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val shape = RoundedCornerShape(bottomStart = HeroRadius, bottomEnd = HeroRadius)
    val clip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val horizon = h * HorizonAt
    val base = Brush.verticalGradient(0f to HeroTop, 0.45f to HeroMid, 1f to HeroBottom)
    // Leichter Dunst oben links und ein Schimmer am Horizont.
    val haze = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.05f), Color.Transparent), Offset(w * 0.2f, h * 0.3f), w * 0.9f)
    val glow = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), Offset(w * 0.5f, horizon), w * 0.65f)
    val rows = dotRows(w, h, horizon, density)
    onDrawBehind {
        clipPath(clip) {
            drawRect(base)
            drawRect(haze)
            drawRect(glow)
            rows.forEach { r ->
                drawPoints(r.points, PointMode.Points, Color.White.copy(alpha = r.alpha), strokeWidth = r.size, cap = StrokeCap.Round)
            }
            // Feine helle Kante unten, damit sich der Bereich vom Seitenhintergrund löst.
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent)),
                Offset(0f, h - 0.5f),
                Offset(w, h - 0.5f),
                strokeWidth = 1f,
            )
        }
    }
}

private class DotRow(val points: List<Offset>, val alpha: Float, val size: Float)

/**
 * Punkte auf einer Ebene, von einer Kamera aus gesehen: Reihe in Tiefe z liegt bei
 * horizon + (h − horizon)·(1 − Höhe)/z. Vorne große, helle Punkte, zum Horizont kleiner und blasser.
 */
private fun dotRows(w: Float, h: Float, horizon: Float, density: Float): List<DotRow> {
    val depth = h - horizon
    val spacing = DotSpacing * density
    val rows = ArrayList<DotRow>()
    var z = 1f
    while (z < 18f) {
        val half = ceil(w / 2f * z / spacing) + 1
        val pts = ArrayList<Offset>()
        var i = -half
        while (i <= half) {
            val x = w / 2f + i * spacing / z
            // Sanfte Hügel, zur Mitte hin höher.
            val side = (x - w / 2f) / (w * 0.6f)
            val elev = WaveHeight * (0.5f + 0.5f * sin(i * 0.09f + z * 0.55f)) * exp(-side * side)
            val y = horizon + depth * (1f - elev) / z
            if (y in 0f..h) pts += Offset(x, y)
            i += 1f
        }
        // Vorne kräftig, zum Horizont ausblenden.
        val far = ((18f - z) / 8f).coerceIn(0f, 1f)
        // Grundlinie der Reihe (ohne Hügel) entscheidet über das Ausblenden unten.
        val rowY = horizon + depth / z
        val bottom = ((h - rowY) / (BottomFade * density)).coerceIn(0.12f, 1f)
        val alpha = DotAlpha / z.pow(0.55f) * far * bottom
        val dot = (1.9f / sqrt(z)).coerceAtLeast(0.8f) * density
        rows += DotRow(pts, alpha, dot)
        z *= 1.085f
    }
    return rows
}

// ------------------------------------------------------------------ KW-Form

/**
 * Trapez für die gewählte KW: oben die kürzere, unten die längere Kante,
 * alle Ecken weich abgerundet (oben etwas stärker als unten).
 */
internal class TrapezoidShape(
    private val inset: Dp = 9.dp,
    private val topRadius: Dp = 10.dp,
    private val bottomRadius: Dp = 8.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val d = with(density) { inset.toPx() }.coerceAtMost(w / 3)
        val rt = with(density) { topRadius.toPx() }
        val rb = with(density) { bottomRadius.toPx() }
        val corners = listOf(
            Offset(d, 0f) to rt,
            Offset(w - d, 0f) to rt,
            Offset(w, h) to rb,
            Offset(0f, h) to rb,
        )
        val path = Path()
        corners.forEachIndexed { k, (p, r) ->
            val prev = corners[(k + 3) % 4].first
            val next = corners[(k + 1) % 4].first
            val a = towards(p, prev, r)
            val b = towards(p, next, r)
            if (k == 0) path.moveTo(a.x, a.y) else path.lineTo(a.x, a.y)
            path.quadraticTo(p.x, p.y, b.x, b.y)
        }
        path.close()
        return Outline.Generic(path)
    }

    /** Punkt auf der Strecke p→q im Abstand r von p (höchstens bis zur halben Strecke). */
    private fun towards(p: Offset, q: Offset, r: Float): Offset {
        val len = hypot(q.x - p.x, q.y - p.y)
        val t = (r / len).coerceAtMost(0.5f)
        return Offset(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
    }
}
