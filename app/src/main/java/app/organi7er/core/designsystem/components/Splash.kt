package app.organi7er.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

/** Wort, das nach der „7“ eingetippt und danach silbern weggeradiert wird. */
private const val SplashWord = "Organiser"

/** Größe der großen „7“ beim Start (Zeichenfläche 108 × 108 wie das Icon). */
private val MarkSize = 200.dp

/** Rückfall-Größe des Kopfzeilen-Logos, falls dessen Position noch nicht gemessen ist. */
private val LogoSize = 40.dp
private val LogoStart = 24.dp

/**
 * Gemessene Position des Kopfzeilen-Logos (Root-Koordinaten, in px). Die Kopfzeile trägt sie ein,
 * ohne sich zu verändern; der Ladebildschirm landet exakt dort.
 */
object LogoAnchor {
    var bounds by mutableStateOf<Rect?>(null)
}

// Geometrie der „7“ im 108er-Raster des Icons (identisch zu ic_launcher_foreground.xml).
private const val DotRadius = 5.34f
private const val NeckWidth = 3.64f
private val MarkDots = listOf(
    Offset(41.11f, 29.58f), Offset(53.89f, 29.58f), Offset(66.84f, 29.58f),
    Offset(62.97f, 41.84f), Offset(59.4f, 54f),
    Offset(56.61f, 66.27f), Offset(56.1f, 78.42f),
)
private val MarkNecks = listOf(0 to 1, 3 to 4, 5 to 6)

// Zeitachse in ms (Summe unter 4 s).
private const val PulseCycle = 950L
private const val PulseEnd = 2 * PulseCycle // 1,9 s: zwei langsame Atemzüge
private const val TypeMsPerLetter = 35L
private const val EraseMsPerLetter = 20L
private const val FadeMs = 220L
private val SplashTotalMs: Long =
    PulseEnd + 800 + SplashWord.length * TypeMsPerLetter + 100 + 260 +
        SplashWord.length * EraseMsPerLetter + 40 + FadeMs // ≈ 3,8 s

private fun seg(t: Long, from: Long, dur: Long): Float = ((t - from).toFloat() / dur).coerceIn(0f, 1f)

/**
 * Ladebildschirm, insgesamt unter 4 Sekunden:
 * 0 – 1,9 s   die „7“ pulsiert in der Mitte (langsam, ruhig),
 * 1,9 – 2,7 s sie wandert nach oben und dann nach links an die gemessene Position des
 *             Kopfzeilen-Logos und schrumpft; dort bekommt sie die abgerundete Kachel,
 *             die Kugeln werden gegenfarbig,
 * danach      „Organiser“ wird getippt, silbern aufgeblitzt und weggeradiert,
 * zuletzt     blendet der Bildschirm aus; die Kachel liegt deckungsgleich auf dem Logo.
 *
 * Hell: silbernes Glas-Gradient, schwarze Kugeln, dann schwarze Kachel mit weißen Kugeln.
 * Dunkel: schwarz, weiße Kugeln, dann weiße Kachel mit schwarzen Kugeln.
 */
@Composable
fun OrganiSplash(dark: Boolean, onFinished: () -> Unit) {
    // Eine feste Zeitachse (ms seit dem ersten gezeichneten Frame): Ruckler auf langsamen Geräten
    // lassen Frames aus, strecken den Ablauf aber nicht über die 4 Sekunden hinaus.
    var t by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var start = -1L
        while (t < SplashTotalMs) {
            val now = withFrameMillis { it }
            if (start < 0) start = now
            t = now - start
        }
        onFinished()
    }

    val n = SplashWord.length
    val pulse = if (t < PulseEnd) {
        1f + 0.10f * (0.5f - 0.5f * cos(2.0 * PI * t / PulseCycle).toFloat())
    } else {
        1f
    }
    val up = FastOutSlowInEasing.transform(seg(t, PulseEnd, 400))
    val left = FastOutSlowInEasing.transform(seg(t, PulseEnd + 400, 400))
    val tile = seg(t, PulseEnd + 550, 250)
    val typeStart = PulseEnd + 800
    val typeEnd = typeStart + n * TypeMsPerLetter
    val flashStart = typeEnd + 100
    val eraseStart = flashStart + 260
    val eraseEnd = eraseStart + n * EraseMsPerLetter
    val typed = when {
        t < typeStart -> 0
        t < eraseStart -> minOf(n, ((t - typeStart) / TypeMsPerLetter + 1).toInt())
        else -> maxOf(0, n - ((t - eraseStart) / EraseMsPerLetter + 1).toInt())
    }
    val flash = when {
        t < flashStart -> 0f
        t < flashStart + 120 -> seg(t, flashStart, 120)
        t < eraseStart -> 1f - seg(t, flashStart + 120, 140)
        else -> 0f
    }
    val fade = 1f - seg(t, eraseEnd + 40, FadeMs)

    val ink = if (dark) Color.White else Color(0xFF0A0A0B)
    val tileColor = ink
    val inverse = if (dark) Color(0xFF0A0A0B) else Color.White
    val silver = if (dark) Color(0xFFE3E7EE) else Color(0xFF7F8794)
    val textColor = lerp(ink, silver, flash)

    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
        val density = LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val markPx = with(density) { MarkSize.toPx() }
        // Ziel: das tatsächliche Logo der Kopfzeile; solange unbekannt, die geschätzte Lage.
        val target = LogoAnchor.bounds ?: with(density) {
            Rect(
                Offset(LogoStart.toPx(), WindowInsets.statusBars.getTop(density) + 8.dp.toPx()),
                Size(LogoSize.toPx(), LogoSize.toPx()),
            )
        }
        val shrink = 0.5f * up + 0.5f * left
        val finalScale = target.width / markPx

        Canvas(Modifier.fillMaxSize()) { drawSplashBackground(dark) }

        Box(
            Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        ((target.center.x - w / 2f) * left).roundToInt(),
                        ((target.center.y - h / 2f) * up).roundToInt(),
                    )
                }
                .size(MarkSize)
                .graphicsLayer {
                    val s = pulse * (1f - (1f - finalScale) * shrink)
                    scaleX = s
                    scaleY = s
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // Kachel (Radius 12/40 wie das Logo) blendet ein, die Kugeln wechseln auf Gegenfarbe.
                drawRoundRect(
                    tileColor.copy(alpha = tile),
                    cornerRadius = CornerRadius(size.width * 0.3f),
                )
                drawOrganiSeven(lerp(ink, inverse, tile))
            }
        }

        // Wort rechts neben der Kachel, auf deren Mitte ausgerichtet.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset((target.right + 12.dp.toPx()).roundToInt(), target.top.roundToInt()) }
                .height(with(density) { target.height.toDp() }),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                SplashWord.take(typed),
                style = OrganiTheme.type.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium),
                color = textColor,
                maxLines = 1,
            )
        }
    }
}

/** Zeichnet die „7“ aus Kugeln und Stegen in das gesamte Quadrat des aktuellen Zeichenbereichs. */
fun DrawScope.drawOrganiSeven(color: Color) {
    val k = size.width / 108f
    MarkNecks.forEach { (a, b) ->
        val p = MarkDots[a]
        val q = MarkDots[b]
        drawLine(color, Offset(p.x * k, p.y * k), Offset(q.x * k, q.y * k), strokeWidth = NeckWidth * k)
    }
    MarkDots.forEach { drawCircle(color, DotRadius * k, Offset(it.x * k, it.y * k)) }
}

private fun DrawScope.drawSplashBackground(dark: Boolean) {
    if (dark) {
        drawRect(Color.Black)
        return
    }
    // Silbernes Liquid-Glass: heller Kern, kühler Rand, dazu ein weicher Glanz unten rechts.
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0xFFF8F9FB), Color(0xFFD5D9E0), Color(0xFFA7ADB7)),
            center = Offset(size.width * 0.42f, size.height * 0.36f),
            radius = size.maxDimension * 0.75f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x66FFFFFF), Color.Transparent),
            center = Offset(size.width * 0.78f, size.height * 0.82f),
            radius = size.width * 0.5f,
        ),
    )
}
