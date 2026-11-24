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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Kopf der Hauptseiten: oben klein Logo + „Organi7er“, rechts Glocke (nur Icon) und
 * Profilbild bzw. Einstellungen. Darunter groß der Seitentitel.
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
    Column(modifier.statusBarsPadding().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Text("Organi7er", Modifier.padding(start = 8.dp).weight(1f), style = OrganiTheme.type.label.copy(fontSize = 15.sp), color = c.text)
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
                        "Einstellungen",
                        Modifier
                            .size(40.dp)
                            .clip(OrganiShapes.circle)
                            .border(1.5.dp, c.glassEdge, OrganiShapes.circle)
                            .clickable(role = Role.Button, onClick = app.openSettings),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                GlassCircleButton(Ms.settings, "Einstellungen", app.openSettings, size = 40.dp)
            }
        }
        if (eyebrow != null) Box(Modifier.padding(top = 10.dp)) { eyebrow() }
        Row(Modifier.fillMaxWidth().padding(top = if (eyebrow != null) 0.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f).semantics { heading() }, style = OrganiTheme.type.pageTitle.copy(fontSize = 32.sp, lineHeight = 38.sp), color = c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

@Composable
private fun Logo() {
    val c = OrganiTheme.colors
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (c.isDark) Primitives.Paper else Primitives.Navy900),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ic_stat_organi7er),
            null,
            Modifier.fillMaxSize().padding(3.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(if (c.isDark) Primitives.Navy900 else c.accent),
        )
    }
}
