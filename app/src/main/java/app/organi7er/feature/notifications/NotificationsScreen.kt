package app.organi7er.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.NotificationRepository
import app.organi7er.core.data.Notifications
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.InsetCard
import app.organi7er.core.designsystem.components.LeadingCircle
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.NotificationEntry
import app.organi7er.feature.editor.LocalAppActions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class NotificationDay(val label: String, val items: List<NotificationEntry>)

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repo: NotificationRepository) : ViewModel() {
    val days: StateFlow<List<NotificationDay>?> = repo.entries.map { list ->
        val today = LocalDate.now()
        list.groupBy { it.createdAt.toLocal().toLocalDate() }.map { (d, items) -> NotificationDay(Dates.relativeDay(d, today), items) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
}

/** Was man verpasst hat: jede Erinnerung, jedes Briefing, jeder Entwurfs-Hinweis. */
@Composable
fun NotificationsScreen(vm: NotificationsViewModel = hiltViewModel()) {
    val days by vm.days.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors
    // Ungelesene dieser Sitzung merken, dann als gelesen markieren – die Punkte bleiben bis zum Verlassen sichtbar.
    var unreadIds by remember { mutableStateOf<Set<Long>?>(null) }
    LaunchedEffect(days != null) {
        val loaded = days ?: return@LaunchedEffect
        if (unreadIds == null) {
            unreadIds = loaded.flatMap { it.items }.filterNot { it.read }.map { it.id }.toSet()
            vm.markAllRead()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "head") {
            Row(Modifier.statusBarsPadding().padding(top = 6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassCircleButton(Ms.close, "Schließen", actions.back, size = 40.dp)
                Text("Benachrichtigungen", Modifier.padding(start = 6.dp).semantics { heading() }, style = OrganiTheme.type.pageTitle, color = c.text)
            }
        }
        val list = days.orEmpty()
        if (days != null && list.isEmpty()) {
            item(key = "empty") {
                Text(
                    "Noch keine Benachrichtigungen. Erinnerungen an Termine und Notizen, das Morgen-Briefing und Hinweise zu Entwürfen erscheinen hier.",
                    Modifier.padding(horizontal = 4.dp),
                    style = OrganiTheme.type.body,
                    color = c.textSoft,
                )
            }
        }
        items(list, key = { it.label }) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(day.label, Modifier.padding(horizontal = 4.dp).semantics { heading() }, style = OrganiTheme.type.title, color = c.text)
                InsetCard(padding = 10.dp) {
                    day.items.forEach { n -> Entry(n, unread = unreadIds?.contains(n.id) == true) }
                }
            }
        }
    }
}

@Composable
private fun Entry(n: NotificationEntry, unread: Boolean) {
    val c = OrganiTheme.colors
    val icon = when (n.kind) {
        Notifications.CHANNEL_BRIEFING -> Ms.lightMode
        Notifications.CHANNEL_DRAFTS -> Ms.editNote
        else -> Ms.alarm
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LeadingCircle(size = 38.dp) { MsIcon(icon, null, tint = c.text, size = 18.dp) }
        Column(Modifier.weight(1f)) {
            Text(n.title, style = OrganiTheme.type.rowTitle.copy(fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (n.text.isNotBlank()) Text(n.text, style = OrganiTheme.type.rowMeta, color = c.textSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Dates.time(n.createdAt.toLocal()), style = OrganiTheme.type.rowMeta, color = c.textSoft)
            if (unread) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(OrganiShapes.circle).background(c.danger))
        }
    }
}
