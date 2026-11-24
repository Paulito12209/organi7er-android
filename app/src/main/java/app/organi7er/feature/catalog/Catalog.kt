package app.organi7er.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.BriefingTile
import app.organi7er.core.designsystem.components.CalendarEventItem
import app.organi7er.core.designsystem.components.CaptureButton
import app.organi7er.core.designsystem.components.DateDivider
import app.organi7er.core.designsystem.components.DraftRow
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.EventRow
import app.organi7er.core.designsystem.components.FilterTile
import app.organi7er.core.designsystem.components.EdgePicker
import app.organi7er.core.designsystem.components.EdgeTab
import app.organi7er.core.designsystem.components.GlassCard
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.NextEventCard
import app.organi7er.core.designsystem.components.NoteCard
import app.organi7er.core.designsystem.components.NowLine
import app.organi7er.core.designsystem.components.Overline
import app.organi7er.core.designsystem.components.PillButton
import app.organi7er.core.designsystem.components.PillVariant
import app.organi7er.core.designsystem.components.PriorityDot
import app.organi7er.core.designsystem.components.SearchField
import app.organi7er.core.designsystem.components.SegmentedControl
import app.organi7er.core.designsystem.components.SegmentedList
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.designsystem.components.SoftSwitch
import app.organi7er.core.designsystem.components.StatusTag
import app.organi7er.core.designsystem.components.TagTone
import app.organi7er.core.designsystem.components.TaskRow
import app.organi7er.core.designsystem.components.TileStats
import app.organi7er.core.designsystem.components.aura
import app.organi7er.feature.editor.LocalAppActions

/** Nur im Debug-Build erreichbar (Einstellungen → Entwicklung). Alle Komponenten, Hell und Dunkel umschaltbar. */
@Composable
fun CatalogScreen() {
    var dark by remember { mutableStateOf(false) }
    val actions = LocalAppActions.current
    app.organi7er.core.designsystem.OrganiTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize().aura()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.section),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    SoftIconButton(Ms.chevronLeft, "Zurück", actions.back)
                    Text("Katalog", Modifier.weight(1f), style = OrganiTheme.type.pageTitle, color = OrganiTheme.colors.text)
                }
                SegmentedControl(listOf("Hell", "Dunkel"), if (dark) 1 else 0, { dark = it == 1 })
                CatalogContent()
            }
        }
    }
}

@Composable
fun CatalogContent() {
    var seg by remember { mutableIntStateOf(1) }
    var sw by remember { mutableStateOf(true) }
    var check by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }

    Overline("Knöpfe")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        SoftIconButton(Ms.settings, "Einstellungen", {})
        SoftIconButton(Ms.search, "Suche", {}, size = 48.dp)
        PillButton("Primär", {})
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PillButton("Soft", {}, variant = PillVariant.Soft, small = true)
        PillButton("Ghost", {}, variant = PillVariant.Ghost, small = true)
        PillButton("Dark", {}, variant = PillVariant.Dark, small = true)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        EntryType.entries.forEach { CaptureButton(it, {}) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        SoftSwitch(sw, { sw = it })
        StatusTag("In Arbeit", TagTone.Blue)
        StatusTag("Geblockt", TagTone.Red)
        PriorityDot()
    }
    Overline("Steuerung")
    SegmentedControl(listOf("Entwürfe", "Aktiv", "Erledigt"), seg, { seg = it })
    SearchField(q, { q = it }, "Notiz oder Bild suchen")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterTile("Alle", 12, true, {}, Modifier.weight(1f))
        FilterTile("Mit Bild", 5, false, {}, Modifier.weight(1f))
        FilterTile("Nur Text", 7, false, {}, Modifier.weight(1f))
    }
    Overline("Briefing")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BriefingTile("Heute", TileStats(3, 5, 4), "in 25 Min", Ms.schedule, "Sprint-Review · 10:00", 0.58f, true, {})
        BriefingTile("Morgen", TileStats(2, 3, 1), "ab 09:00", Ms.schedule, "Daily und Planung", null, false, {})
    }
    Overline("Termine")
    NextEventCard("10:00–10:45", "in 25 Min", "Sprint-Review Organi7er", "Google Meet", "45 Min", {})
    SegmentedList {
        EventRow("13:30", "1 Std", OrganiTheme.colors.calPrivate, "Mittag mit Lena", "Café Lindner", 0, 2, {})
        EventRow("16:00", "30 Min", OrganiTheme.colors.calHealth, "Zahnarzt", "Dr. Weber", 1, 2, {})
    }
    NowLine("9:35")
    CalendarEventItem(OrganiTheme.colors.calWork, "Sprint-Review", "10:00–10:45 · Google Meet", {}, tag = "Vorläufig")
    Overline("Aufgaben")
    SegmentedList {
        TaskRow("Steuerunterlagen an Lena", "18:00 · Privat", check, { check = !check }, {}, 0, 2, trailing = { PriorityDot() })
        DraftRow("Website-Portfolio überarbeiten", "vor 3 Tagen", {}, {}, 1, 2)
    }
    Overline("Notizen")
    DateDivider("Heute")
    NoteCard("Einkauf Wochenende", "Hafermilch, Brot, Äpfel, Kaffee", null, 0, "Morgen, 09:00", {}, {}, {})
    GlassCard(Modifier.fillMaxWidth()) { Text("GlassCard", Modifier.padding(18.dp), style = OrganiTheme.type.body, color = OrganiTheme.colors.text) }
    Overline("Navigation")
    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.CenterEnd) {
        EdgePicker(
            listOf(
                EdgeTab("Übersicht", Ms.home, Ms.homeFilled),
                EdgeTab("Termine", Ms.calendarToday, Ms.calendarTodayFilled),
                EdgeTab("Aufgaben", Ms.checklist, Ms.checklistFilled),
                EdgeTab("Notizen", Ms.description, Ms.descriptionFilled),
            ),
            tab,
            { tab = it },
        )
    }
}
