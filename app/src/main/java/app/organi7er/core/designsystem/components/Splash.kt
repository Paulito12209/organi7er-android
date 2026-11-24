package app.organi7er.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiTheme
import kotlinx.coroutines.delay

/** Wort, das nach der „7“ eingetippt und danach silbern weggeradiert wird. */
private const val SplashWord = "Organiser"

/** Größe der großen „7“ beim Start (Zeichenfläche 108 × 108 wie das Icon). */
private val MarkSize = 200.dp

// Geometrie der „7“ im 108er-Raster des Icons (identisch zu ic_launcher_foreground.xml).
private const val DotRadius = 5.34f
private const val NeckWidth = 3.64f
private val MarkDots = listOf(
    Offset(41.11f, 29.58f), Offset(53.89f, 29.58f), Offset(66.84f, 29.58f),
    Offset(62.97f, 41.84f), Offset(59.4f, 54f),
    Offset(56.61f, 66.27f), Offset(56.1f, 78.42f),
)
private val MarkNecks = listOf(0 to 1, 3 to 4, 5 to 6)

/**
 * Ladebildschirm: die „7“ pulsiert in der Mitte, wandert nach oben (Kopfzeile), schrumpft
 * und rutscht nach links. Dort wird „Organiser“ getippt, silbern aufgeblitzt und wieder
 * weggeradiert. Danach blendet der Bildschirm aus und gibt die App frei.
 *
 * Hell: silbernes Glas-Gradient mit schwarzen Kugeln. Dunkel: schwarz mit weißen Kugeln.
 */
@Composable
fun OrganiSplash(dark: Boolean, onFinished: () -> Unit) {
    val pulse = remember { Animatable(1f) }
    val up = remember { Animatable(0f) }
    val left = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var typed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repeat(2) {
            pulse.animateTo(1.14f, tween(260, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        }
        up.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        left.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        for (i in 1..SplashWord.length) {
            typed = i
            delay(60)
        }
        delay(200)
        flash.animateTo(1f, tween(140))
        flash.animateTo(0f, tween(200))
        for (i in SplashWord.length - 1 downTo 0) {
            typed = i
            delay(45)
        }
        delay(120)
        fade.animateTo(0f, tween(320))
        onFinished()
    }

    val ink = if (dark) Color.White else Color(0xFF0A0A0B)
    val silver = if (dark) Color(0xFFE3E7EE) else Color(0xFF7F8794)
    val textColor = lerp(ink, silver, flash.value)

    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // Endposition: Mitte der „7“ liegt links in der Kopfzeile, dort wo das Logo sitzt.
        val targetX = -maxWidth / 2f + 40.dp
        val targetY = -maxHeight / 2f + top + 28.dp
        val shrink = 0.5f * up.value + 0.5f * left.value

        Canvas(Modifier.fillMaxSize()) { drawSplashBackground(dark) }

        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = targetX * left.value, y = targetY * up.value)
                .size(MarkSize)
                .graphicsLayer {
                    val s = pulse.value * (1f - 0.82f * shrink)
                    scaleX = s
                    scaleY = s
                },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawSeven(ink) }
        }

        Text(
            SplashWord.take(typed),
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 70.dp, top = top + 14.dp),
            style = OrganiTheme.type.title.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium),
            color = textColor,
            maxLines = 1,
        )
    }
}

private fun DrawScope.drawSeven(color: Color) {
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
