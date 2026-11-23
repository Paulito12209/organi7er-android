package app.organi7er.core.designsystem.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.organi7er.core.data.Notifications
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Einheitliches Bottom Sheet zum Anlegen und Bearbeiten. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganiSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = OrganiTheme.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.bg,
        contentColor = c.text,
        shape = OrganiShapes.sheet,
        scrimColor = (if (c.isDark) Color(0xFF010814) else c.hero).copy(alpha = 0.32f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content,
        )
    }
}

/** Kopf eines Sheets: Typ-Punkt, Titel, Schließen. */
@Composable
fun SheetHeader(title: String, type: EntryType, onClose: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val c = OrganiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Box(Modifier.size(36.dp).clip(OrganiShapes.circle).background(type.fill()), contentAlignment = Alignment.Center) {
            MsIcon(type.icon, null, tint = type.onFill(), size = 20.dp)
        }
        Text(title, Modifier.weight(1f), style = OrganiTheme.type.title, color = c.text)
        trailing()
        PlainIconButton(Ms.close, "Schließen", onClose, tint = c.textMuted, iconSize = 22.dp)
    }
}

/** Großes Titelfeld ohne Rahmen (wie ein Blatt). */
@Composable
fun TitleField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, autoFocus: Boolean = false) {
    val c = OrganiTheme.colors
    val focus = remember { FocusRequester() }
    Box(modifier.fillMaxWidth()) {
        if (value.isEmpty()) Text(placeholder, style = OrganiTheme.type.headline, color = c.textSoft)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = OrganiTheme.type.headline.copy(color = c.text),
            cursorBrush = SolidColor(c.brandBlue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = placeholder },
        )
    }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** Eingabefeld als Fläche (surface, Radius 14). */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    style: TextStyle = OrganiTheme.type.body,
) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(OrganiShapes.small)
            .background(c.surface)
            .border(1.dp, c.border.copy(alpha = 0.6f), OrganiShapes.small)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) MsIcon(icon, null, tint = c.textSoft, size = 20.dp)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = style, color = c.textSoft)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = style.copy(color = c.text),
                cursorBrush = SolidColor(c.brandBlue),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
            )
        }
    }
}

/** Zeile „Datum · Mo, 23. Nov.“ als tippbare Fläche. */
@Composable
fun PickerField(@DrawableRes icon: Int, label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, onClear: (() -> Unit)? = null) {
    val c = OrganiTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(OrganiShapes.small)
            .background(c.surface)
            .border(1.dp, c.border.copy(alpha = 0.6f), OrganiShapes.small)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(start = 14.dp, end = if (onClear != null) 0.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MsIcon(icon, null, tint = c.textSoft, size = 20.dp)
        Text(label, Modifier.weight(1f), style = OrganiTheme.type.body, color = c.textMuted)
        Text(value, style = OrganiTheme.type.rowTitle, color = c.text)
        if (onClear != null) PlainIconButton(Ms.close, "$label entfernen", onClear, iconSize = 16.dp)
    }
}

/** Einfachauswahl als Pillen (gewählt = Primary). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, color: (T) -> Color? = { null }) {
    val c = OrganiTheme.colors
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { option ->
            val isSel = option == selected
            val dot = color(option)
            Row(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(OrganiShapes.pill)
                    .background(if (isSel) c.primary else c.surface)
                    .border(1.dp, if (isSel) Color.Transparent else c.border, OrganiShapes.pill)
                    .selectable(isSel, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (dot != null) Box(Modifier.size(8.dp).clip(OrganiShapes.circle).background(dot))
                Text(label(option), style = OrganiTheme.type.buttonSmall, color = if (isSel) c.onPrimary else c.text)
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) = Overline(text)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit, minDate: LocalDate? = null) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDate == null || !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(minDate)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = OrganiTheme.colors.bg),
    ) {
        DatePicker(state, showModeToggle = false, colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = OrganiTheme.colors.bg))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OrganiTheme.colors.bg,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OrganiTheme.colors.bg,
        title = { Text(title, style = OrganiTheme.type.title) },
        text = { Text(text, style = OrganiTheme.type.body, color = OrganiTheme.colors.textMuted) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(confirm, color = OrganiTheme.colors.dangerText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

/** Fragt POST_NOTIFICATIONS erst bei Bedarf (API 33+). */
@Composable
fun rememberNotificationPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return remember(context) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/** Kompakte Zeile zweier Picker nebeneinander. */
@Composable
fun PickerPair(left: @Composable (Modifier) -> Unit, right: @Composable (Modifier) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        left(Modifier.weight(1f))
        right(Modifier.weight(1f))
    }
}

@Composable
fun SheetActions(primary: String, onPrimary: () -> Unit, enabled: Boolean, secondary: String? = null, onSecondary: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (secondary != null && onSecondary != null) {
            PillButton(secondary, onSecondary, Modifier.weight(1f), variant = PillVariant.Ghost, icon = Ms.delete)
        }
        PillButton(primary, onPrimary, Modifier.weight(1f), variant = PillVariant.Primary, enabled = enabled)
    }
}
