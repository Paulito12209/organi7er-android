package app.organi7er.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.softShadow
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import java.io.File
import kotlin.math.sin

private const val STRIPS = 22

/**
 * Riffelglas: das Bild wird in senkrechte Streifen geschnitten, jeder Streifen leicht versetzt
 * und mit einer hellen und einer dunklen Kante versehen – wie hinter geriffeltem Glas.
 */
private fun DrawScope.fluted(drawImage: DrawScope.() -> Unit) {
    val w = size.width / STRIPS
    for (i in 0 until STRIPS) {
        val left = i * w
        val shift = (sin(i * 1.3f) * w * 0.55f)
        clipRect(left, 0f, left + w + 0.5f, size.height) {
            // Jeder Streifen zeigt einen etwas breiteren, verschobenen Ausschnitt (Linsenwirkung).
            translate(left = shift) { drawImage() }
            drawRect(
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.28f),
                    0.18f to Color.Transparent,
                    0.75f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.22f),
                    startX = left,
                    endX = left + w,
                ),
                topLeft = Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
            )
        }
    }
}

@Composable
fun ProfileHeader(name: String, imagePath: String?, onBack: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val painter: Painter? = imagePath?.let { path ->
        val request = remember(path) { coil3.request.ImageRequest.Builder(context).data(File(path)).size(1080).build() }
        rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
    }
    Box(modifier.fillMaxWidth().height(360.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    fluted {
                        if (painter != null) {
                            with(painter) { draw(size) }
                        } else {
                            drawRect(Brush.linearGradient(listOf(Primitives.Navy200, Primitives.Blue400, Primitives.Navy700), end = Offset(size.width, size.height)))
                            drawCircle(Brush.radialGradient(listOf(Primitives.Blue200, Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.25f), radius = size.width * 0.6f))
                            drawCircle(Brush.radialGradient(listOf(Primitives.Yellow300.copy(alpha = 0.7f), Color.Transparent), center = Offset(size.width * 0.72f, size.height * 0.5f), radius = size.width * 0.45f))
                        }
                    }
                    // Unten in den Seitenhintergrund auslaufen.
                    drawRect(Brush.verticalGradient(0.45f to Color.Transparent, 1f to c.bg))
                },
        )
        if (painter == null) {
            Text(
                name.initials(),
                Modifier.align(Alignment.Center).padding(bottom = 40.dp),
                style = OrganiTheme.type.pageTitle.copy(fontSize = 72.sp, lineHeight = 80.sp),
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlassCircleButton(Ms.chevronLeft, "Zurück", onBack)
            Text(
                "Einstellungen",
                Modifier.weight(1f).semantics { heading() },
                style = OrganiTheme.type.title,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(Modifier.size(48.dp))
        }
        ProfileCard(name, imagePath, onEdit, Modifier.align(Alignment.BottomCenter).padding(horizontal = 14.dp))
    }
}

@Composable
private fun ProfileCard(name: String, imagePath: String?, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .fillMaxWidth()
            .softShadow(shape, c.shadowFloat)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.glassStrong, c.glass)))
            .background(if (c.isDark) Color(0xCC0A1224) else Color.White.copy(alpha = 0.55f))
            .border(1.dp, c.glassEdge, shape)
            .clickable(role = Role.Button, onClickLabel = "Profil bearbeiten", onClick = onEdit)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(Primitives.Blue500).border(1.dp, c.glassEdge, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (imagePath != null) {
                AsyncImage(File(imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text(name.initials(), style = OrganiTheme.type.title, color = Color.White)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "Dein Profil" }, style = OrganiTheme.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (imagePath == null) "Profilbild hinzufügen" else "Profil bearbeiten", style = OrganiTheme.type.rowMeta, color = c.textSoft)
        }
        Box(
            Modifier
                .size(50.dp)
                .clip(OrganiShapes.circle)
                .background(c.glassSolid)
                .semantics { contentDescription = "Profil bearbeiten" },
            contentAlignment = Alignment.Center,
        ) { MsIcon(Ms.editNote, null, tint = c.onGlassSolid, size = 20.dp) }
    }
}

fun String.initials(): String = trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "7" }
