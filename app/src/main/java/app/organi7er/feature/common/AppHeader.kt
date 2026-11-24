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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import app.organi7er.R
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.feature.editor.LocalAppActions
import coil3.compose.AsyncImage
import java.io.File

/** Was der Kopf jeder Seite aus der App-Hülle braucht. */
@Immutable
data class ShellInfo(val unread: Int = 0, val profileImage: String? = null)

val LocalShell = staticCompositionLocalOf { ShellInfo() }

/**
 * Kopf der Hauptseiten: Logo und daneben groß der Seitentitel, rechts Glocke (nur Icon)
 * und Profilbild bzw. Einstellungen.
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
        // Obere Zeile: Logo links, Glocke und Profil/Einstellungen rechts.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            // App-Name dezent neben dem Logo.
            Text(
                "Organi7er",
                Modifier.padding(start = 12.dp).weight(1f),
                style = OrganiTheme.type.title.copy(fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = c.textSoft.copy(alpha = 0.7f),
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically, content = actions)
            Box(
                Modifier
                    .size(Spacing.touch)
                    .clip(OrganiShapes.circle)
                    .clickable(role = Role.Button, onClick = app.openNotifications)
                    .semantics { contentDescription = if (shell.unread > 0) "Benachrichtigungen, ${shell.unread} ungelesen" else "Benachrichtigungen" },
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(Ms.notifications, null, tint = c.text, size = 23.dp)
                if (shell.unread > 0) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .padding(start = 13.dp, bottom = 13.dp)
                            .size(9.dp)
                            .clip(OrganiShapes.circle)
                            .background(c.danger)
                            .border(1.5.dp, c.bg, OrganiShapes.circle),
                    )
                }
            }
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

@Composable
private fun Logo() {
    val c = OrganiTheme.colors
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (c.isDark) Color.White else Color(0xFF0A0A0B)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ic_stat_organi7er),
            null,
            Modifier.fillMaxSize().padding(5.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (c.isDark) Color(0xFF0A0A0B) else Color.White),
        )
    }
}
