package app.organi7er.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.model.Meeting
import app.organi7er.core.model.MeetingPlatform

/**
 * Weißer runder Knopf mit dem Symbol der Meeting-Plattform; öffnet den Link im Browser bzw. in der App.
 * Die Symbole sind vereinfachte Nachzeichnungen (Farben der Marken), keine Original-Logos.
 */
@Composable
fun MeetingButton(link: String, modifier: Modifier = Modifier, size: Dp = 42.dp) {
    val uri = LocalUriHandler.current
    val platform = Meeting.platform(link)
    Box(
        modifier
            .size(size)
            .clip(OrganiShapes.circle)
            .background(Color.White)
            .clickable(role = Role.Button, onClickLabel = "${platform.label} öffnen") { runCatching { uri.openUri(Meeting.normalize(link)) } }
            .semantics { contentDescription = "${platform.label} öffnen" },
        contentAlignment = Alignment.Center,
    ) {
        if (platform == MeetingPlatform.Other) {
            MsIcon(Ms.arrowOutward, null, tint = Color(0xFF0A0A0B), size = size * 0.45f)
        } else {
            Canvas(Modifier.size(size * 0.58f)) {
                when (platform) {
                    MeetingPlatform.GoogleMeet -> meet()
                    MeetingPlatform.Zoom -> zoom()
                    MeetingPlatform.Teams -> teams()
                    MeetingPlatform.Webex -> webex()
                    MeetingPlatform.Other -> Unit
                }
            }
        }
    }
}

/** Kamera aus vier Farbflächen plus Objektiv-Keil. */
private fun DrawScope.meet() {
    val w = size.width
    val h = size.height
    val top = h * 0.2f
    val bh = h * 0.6f
    val bodyW = w * 0.66f
    val r = CornerRadius(w * 0.06f)
    drawRoundRect(Color(0xFF00AC47), Offset(0f, top), Size(bodyW, bh), r)
    drawRect(Color(0xFFFFBA00), Offset(w * 0.22f, top), Size(bodyW - w * 0.22f, bh / 2))
    drawRect(Color(0xFFEA4335), Offset(0f, top), Size(w * 0.22f, bh / 2))
    drawRect(Color(0xFF2684FC), Offset(0f, top + bh / 2), Size(w * 0.22f, bh / 2))
    val lens = Path().apply {
        moveTo(bodyW, top + bh * 0.3f)
        lineTo(w, top)
        lineTo(w, top + bh)
        lineTo(bodyW, top + bh * 0.7f)
        close()
    }
    drawPath(lens, Color(0xFF00832D))
}

/** Blauer Kreis mit weißer Kamera. */
private fun DrawScope.zoom() {
    val w = size.width
    drawCircle(Color(0xFF0B5CFF))
    val camW = w * 0.36f
    val camH = w * 0.28f
    val x = w * 0.2f
    val y = (size.height - camH) / 2
    drawRoundRect(Color.White, Offset(x, y), Size(camW, camH), CornerRadius(w * 0.06f))
    val lens = Path().apply {
        moveTo(x + camW + w * 0.03f, y + camH * 0.35f)
        lineTo(x + camW + w * 0.2f, y + camH * 0.05f)
        lineTo(x + camW + w * 0.2f, y + camH * 0.95f)
        lineTo(x + camW + w * 0.03f, y + camH * 0.65f)
        close()
    }
    drawPath(lens, Color.White)
}

/** Lila Kachel mit weißem „T“. */
private fun DrawScope.teams() {
    val w = size.width
    val h = size.height
    drawCircle(Color(0xFF7B83EB), radius = w * 0.17f, center = Offset(w * 0.8f, h * 0.25f))
    drawRoundRect(Color(0xFF7B83EB), Offset(w * 0.5f, h * 0.42f), Size(w * 0.5f, h * 0.45f), CornerRadius(w * 0.12f))
    drawRoundRect(Color(0xFF5059C9), Offset(0f, h * 0.12f), Size(w * 0.68f, h * 0.76f), CornerRadius(w * 0.1f))
    drawRect(Color.White, Offset(w * 0.12f, h * 0.27f), Size(w * 0.44f, h * 0.11f))
    drawRect(Color.White, Offset(w * 0.285f, h * 0.27f), Size(w * 0.11f, h * 0.48f))
}

/** Zwei ineinandergreifende Bögen in Grün und Blau. */
private fun DrawScope.webex() {
    val w = size.width
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawArc(Color(0xFF07C160), 180f, 180f, false, Offset(w * 0.08f, w * 0.2f), Size(w * 0.5f, w * 0.6f), style = stroke)
    drawArc(Color(0xFF0A78FF), 0f, 180f, false, Offset(w * 0.42f, w * 0.2f), Size(w * 0.5f, w * 0.6f), style = stroke)
}
