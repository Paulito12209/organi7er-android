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
import androidx.compose.ui.unit.em
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
fun ProfileHeader(name: String, imagePath: String?, onBack: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier, startInset: androidx.compose.ui.unit.Dp = 0.dp) {
    val c = OrganiTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val painter: Painter? = imagePath?.let { path ->
        val request = remember(path) { coil3.request.ImageRequest.Builder(context).data(File(path)).size(1080).build() }
        rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
    }
    Box(modifier.fillMaxWidth().height(380.dp)) {
        // Riffelglas-Hintergrund aus dem Profilbild (oder neutralem Verlauf), unten abgedunkelt.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    fluted {
                        if (painter != null) {
                            with(painter) { draw(size) }
                        } else {
                            drawRect(Brush.linearGradient(listOf(Color(0xFF3A3A40), Color(0xFF8E8E96), Color(0xFF1A1A1E)), end = Offset(size.width, size.height)))
                            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width * 0.35f, size.height * 0.3f), radius = size.width * 0.6f))
                        }
                    }
                    drawRect(Color.Black.copy(alpha = 0.28f))
                    drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)))
                },
        )
        Row(Modifier.statusBarsPadding().padding(start = startInset).padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp))
            Text(
                "Organi7er",
                Modifier.weight(1f).semantics { heading() },
                style = OrganiTheme.type.title.copy(fontSize = 22.sp, letterSpacing = 0.12.em),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(Modifier.size(48.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(start = startInset, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(124.dp).clickable(role = Role.Button, onClickLabel = "Profil bearbeiten", onClick = onEdit)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(OrganiShapes.circle)
                        .background(Color(0xFF2A2A2F))
                        .border(3.dp, Color.White.copy(alpha = 0.85f), OrganiShapes.circle),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imagePath != null) {
                        AsyncImage(File(imagePath), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(name.initials(), style = OrganiTheme.type.pageTitle.copy(fontSize = 44.sp), color = Color.White)
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(38.dp)
                        .clip(OrganiShapes.circle)
                        .background(Color(0xFF1C1C20))
                        .border(2.dp, Color.White.copy(alpha = 0.85f), OrganiShapes.circle)
                        .semantics { contentDescription = "Profil bearbeiten" },
                    contentAlignment = Alignment.Center,
                ) { MsIcon(Ms.editNote, null, tint = Color.White, size = 18.dp) }
            }
            Text(
                name.ifBlank { "Dein Profil" },
                Modifier.padding(top = 14.dp),
                style = OrganiTheme.type.pageTitle.copy(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (imagePath == null) "Profilbild hinzufügen" else "Profil bearbeiten",
                Modifier.padding(top = 2.dp),
                style = OrganiTheme.type.body,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

fun String.initials(): String = trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "7" }
