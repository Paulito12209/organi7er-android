package app.organi7er.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import app.organi7er.core.designsystem.components.LogoAnchor
import app.organi7er.core.designsystem.components.drawOrganiSeven
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.liquidGlass
import app.organi7er.feature.editor.LocalAppActions
import coil3.compose.AsyncImage
import java.io.File

/** Was der Kopf jeder Seite aus der App-Hülle braucht. */
@Immutable
data class ShellInfo(val unread: Int = 0, val profileImage: String? = null)

val LocalShell = staticCompositionLocalOf { ShellInfo() }

/**
 * Kopf der Hauptseiten: Logo links, rechts Such-Pille und Profilbild bzw. Einstellungen;
 * darunter groß der Seitentitel.
 */
@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = OrganiTheme.colors
    val shell = LocalShell.current
    val app = LocalAppActions.current
    Column(modifier.statusBarsPadding().padding(top = 8.dp)) {
        // Obere Zeile: Logo links, Suche und Profil/Einstellungen rechts.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            // Kein App-Name mehr im Kopf: der Name erscheint nur im Ladebildschirm.
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
            // Suche als Glas-Pille (wie in der Vorlage). Tippen öffnet die Suche: das Feld läuft über die
            // ganze Breite, Logo und Menüknopf weichen. Benachrichtigungen stehen nicht mehr im Kopf,
            // sondern als Zeile in der Übersicht (nur wenn es ungelesene gibt).
            SearchPill(app.openSearch, Modifier.padding(horizontal = Spacing.xs))
            val image = shell.profileImage
            if (image != null) {
                Box(Modifier.size(Spacing.touch), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        File(image),
                        "Menü",
                        Modifier
                            .size(42.dp)
                            .clip(OrganiShapes.circle)
                            .border(1.5.dp, c.glassEdge, OrganiShapes.circle)
                            .clickable(role = Role.Button, onClick = app.openSettings),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                GlassCircleButton(Ms.settings, "Menü", app.openSettings, size = 42.dp)
            }
        }
        // Darunter groß und kräftig der Seitentitel; optional eine Zeile darüber (z. B. Monat), bündig links.
        if (eyebrow != null) Box(Modifier.padding(top = 26.dp)) { eyebrow() }
        Text(
            title,
            Modifier.padding(top = if (eyebrow != null) 4.dp else 26.dp).semantics { heading() },
            style = OrganiTheme.type.pageTitle.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = (-0.035).em),
            color = c.text,
            maxLines = 1,
        )
    }
}

/** Geschlossene Suche: Lupe und „Suchen“ in einer Glas-Pille. */
@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .height(42.dp)
            // Gleiches Glas wie der Einstellungsknopf daneben.
            .liquidGlass(OrganiShapes.pill)
            .clickable(role = Role.Button, onClickLabel = "Suche öffnen", onClick = onClick)
            .semantics { contentDescription = "Suchen" }
            .padding(start = 14.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MsIcon(Ms.search, null, tint = c.text, size = 18.dp)
        Text("Suchen", style = OrganiTheme.type.body.copy(fontSize = 15.sp), color = c.textMuted, maxLines = 1)
    }
}

@Composable
private fun Logo() {
    val c = OrganiTheme.colors
    Box(
        Modifier
            .size(40.dp)
            // Meldet die Position an den Ladebildschirm; ändert die Lage nicht.
            .onGloballyPositioned { LogoAnchor.bounds = it.boundsInRoot() }
            .clip(RoundedCornerShape(12.dp))
            .background(if (c.isDark) Color.White else Color(0xFF0A0A0B)),
        contentAlignment = Alignment.Center,
    ) {
        // Dieselbe „7“ wie im Ladebildschirm, damit dessen Kachel nahtlos hier landet.
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawOrganiSeven(if (c.isDark) Color(0xFF0A0A0B) else Color.White)
        }
    }
}
