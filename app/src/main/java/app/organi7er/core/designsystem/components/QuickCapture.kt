package app.organi7er.core.designsystem.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import dev.chrisbanes.haze.blur.hazeBlur
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import kotlin.math.cos
import kotlin.math.sin

private val ArcSize = 264.dp
private val ArcRadius = 160.dp

/**
 * „Neuer Eintrag“: Bogen über dem Plus-Knopf (Hintergrund unscharf), Aufgabe · Notiz · Termin auf einem Bogen.
 * [plusEnd]/[plusBottom] = Abstand der Plus-Mitte vom rechten bzw. unteren Rand.
 */
@Composable
fun QuickCaptureOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (EntryType) -> Unit,
    plusEnd: Dp,
    plusBottom: Dp,
    modifier: Modifier = Modifier,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
) {
    val c = OrganiTheme.colors
    val haptics = LocalHapticFeedback.current
    BackHandler(visible, onDismiss)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeBlur(
                                dev.chrisbanes.haze.HazeInput.Sources(hazeState),
                                remember { dev.chrisbanes.haze.blur.HazeBlurStyle { blurRadius(28.dp); noiseFactor(0f) } },
                            )
                        } else {
                            Modifier
                        },
                    )
                    .background(if (c.isDark) Color(0x80000000) else Color(0x59FFFFFF))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        val origin = TransformOrigin(1f, 1f)
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = plusEnd - 32.dp, bottom = plusBottom - 32.dp),
            enter = scaleIn(tween(220), initialScale = 0.4f, transformOrigin = origin) + fadeIn(tween(160)),
            exit = scaleOut(tween(180), targetScale = 0.4f, transformOrigin = origin) + fadeOut(tween(140)),
        ) {
            Box(
                Modifier
                    .size(ArcSize),
            ) {
                // Beschriftung folgt dem Bogen um den Plus-Knopf, außerhalb der drei Knöpfe.
                ArcLabel("NEUER EINTRAG", c.textMuted, Modifier.size(ArcSize))
                // Plus-Mitte liegt in der rechten unteren Ecke (29 dp eingerückt).
                val items = listOf(EntryType.Task to 168.0, EntryType.Note to 129.0, EntryType.Event to 92.0)
                items.forEachIndexed { i, (type, angle) ->
                    val rad = Math.toRadians(angle)
                    val cx = ArcSize - 32.dp + ArcRadius * cos(rad).toFloat()
                    val cy = ArcSize - 32.dp - ArcRadius * sin(rad).toFloat()
                    val appear by animateFloatAsState(if (visible) 1f else 0f, tween(200, delayMillis = 40 * i), label = "qc$i")
                    Column(
                        Modifier
                            .offset(x = cx - 36.dp, y = cy - 27.dp)
                            .size(width = 72.dp, height = 84.dp)
                            .graphicsLayer {
                                alpha = appear
                                scaleX = 0.6f + 0.4f * appear
                                scaleY = 0.6f + 0.4f * appear
                            }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(role = Role.Button, onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                onPick(type)
                            })
                            .semantics(mergeDescendants = true) { contentDescription = type.newLabel },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .softShadow(OrganiShapes.circle, c.shadowSoft)
                                .clip(OrganiShapes.circle)
                                .background(c.glassSolid)
                                .border(1.dp, c.glassEdge, OrganiShapes.circle),
                            contentAlignment = Alignment.Center,
                        ) { MsIcon(type.icon, null, tint = c.onGlassSolid, size = 24.dp) }
                        Text(type.shortLabel, style = OrganiTheme.type.rowMeta, color = c.text, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Text auf einem Kreisbogen um die Plus-Mitte (rechts unten, 32 dp eingerückt), Mitte bei 130°,
 * von unten links nach oben rechts lesbar.
 */
@Composable
private fun ArcLabel(text: String, color: Color, modifier: Modifier) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = OrganiTheme.type.overline.copy(color = color, fontSize = 13.sp, letterSpacing = 0.em)
    androidx.compose.foundation.Canvas(modifier.semantics { contentDescription = text }) {
        val cx = size.width - 32.dp.toPx()
        val cy = size.height - 32.dp.toPx()
        val radius = 206.dp.toPx()
        val gap = 3.dp.toPx()
        val glyphs = text.map { measurer.measure(it.toString(), style) }
        val total = glyphs.sumOf { it.size.width.toDouble() + gap } - gap
        // Winkel in Bogenmaß; Text läuft von großem zu kleinem Winkel (im Uhrzeigersinn).
        var angle = Math.toRadians(130.0) + total / 2 / radius
        glyphs.forEach { g ->
            val half = (g.size.width + gap) / 2.0 / radius
            angle -= half
            val x = cx + radius * kotlin.math.cos(angle).toFloat()
            val y = cy - radius * kotlin.math.sin(angle).toFloat()
            rotate(degrees = (90.0 - Math.toDegrees(angle)).toFloat(), pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                drawText(g, topLeft = androidx.compose.ui.geometry.Offset(x - g.size.width / 2f, y - g.size.height / 2f))
            }
            angle -= half
        }
    }
}
