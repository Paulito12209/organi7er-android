package app.organi7er.core.designsystem.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.feature.catalog.CatalogContent

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    OrganiTheme(darkTheme = isSystemInDarkTheme()) {
        Column(Modifier.aura().padding(18.dp).width(380.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

@PreviewLightDark
@Composable
private fun ButtonsPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SoftIconButton(Ms.settings, "Einstellungen", {})
        PillButton("Primär", {})
        PillButton("Soft", {}, variant = PillVariant.Soft)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { EntryType.entries.forEach { CaptureButton(it, {}) } }
    ExtendedAddButton(EntryType.Note, expanded = true, onClick = {})
}

@PreviewLightDark
@Composable
private fun BriefingPreview() = PreviewFrame {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BriefingTile("Heute", TileStats(3, 5, 4), "in 25 Min", Ms.schedule, "Sprint-Review · 10:00", 0.6f, true, {})
        BriefingTile("Woche", TileStats(9, 12, 6), "Fr voll", Ms.calendarToday, "4 Termine · Workshop", null, false, {})
    }
}

@PreviewLightDark
@Composable
private fun EventsPreview() = PreviewFrame {
    NextEventCard("10:00–10:45", "in 25 Min", "Sprint-Review Organi7er", "Google Meet", "45 Min", {})
    SegmentedList {
        EventRow("13:30", "1 Std", OrganiTheme.colors.calPrivate, "Mittag mit Lena", "Café Lindner", 0, 2, {})
        EventRow("16:00", "30 Min", OrganiTheme.colors.calHealth, "Zahnarzt", "Dr. Weber", 1, 2, {})
    }
}

@PreviewLightDark
@Composable
private fun TasksPreview() = PreviewFrame {
    SegmentedList {
        TaskRow("Steuerunterlagen an Lena", "18:00 · Privat", false, {}, {}, 0, 3, trailing = { PriorityDot() })
        TaskRow("Komponenten-Screenshots", "Organi7er", false, {}, {}, 1, 3, trailing = { StatusTag("In Arbeit", TagTone.Blue) })
        TaskRow("Farbpalette festlegen", "erledigt heute", true, {}, {}, 2, 3)
    }
    SegmentedControl(listOf("Entwürfe", "Aktiv", "Erledigt"), 1, {})
}

@PreviewLightDark
@Composable
private fun NotesPreview() = PreviewFrame {
    DateDivider("Heute")
    NoteCard("Einkauf Wochenende", "Hafermilch, Brot, Äpfel, Kaffee, Batterien AA", null, 0, "Morgen, 09:00", {}, {}, {})
    PaperTextField("Erste Zeile\nZweite Zeile", {}, "Schreib los …")
}

@PreviewLightDark
@Composable
private fun CatalogPreview() = PreviewFrame { CatalogContent() }
