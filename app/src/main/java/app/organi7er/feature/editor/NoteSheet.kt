package app.organi7er.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.DatePickerSheet
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.OrganiSheet
import app.organi7er.core.designsystem.components.PaperTextField
import app.organi7er.core.designsystem.components.PickerField
import app.organi7er.core.designsystem.components.PillButton
import app.organi7er.core.designsystem.components.PillVariant
import app.organi7er.core.designsystem.components.PlainIconButton
import app.organi7er.core.designsystem.components.SheetActions
import app.organi7er.core.designsystem.components.SheetHeader
import app.organi7er.core.designsystem.components.SoftSwitch
import app.organi7er.core.designsystem.components.TimePickerSheet
import app.organi7er.core.designsystem.components.TitleField
import app.organi7er.core.designsystem.components.rememberNotificationPermission
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Note
import app.organi7er.core.model.Status
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private sealed interface PendingImage {
    data class FromUri(val uri: Uri) : PendingImage
    data class FromFile(val path: String) : PendingImage
}

/** Neue Notiz: Titel, Text auf Papier, Fotos, optionale Erinnerung. */
@Composable
fun NoteSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    val c = OrganiTheme.colors
    val askNotifications = rememberNotificationPermission()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf<LocalDateTime?>(null) }
    var picker by remember { mutableStateOf<String?>(null) }
    val images = remember { mutableStateListOf<PendingImage>() }
    var cameraTarget by remember { mutableStateOf<File?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        images += uris.map { PendingImage.FromUri(it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = cameraTarget
        if (ok && file != null) images += PendingImage.FromFile(file.absolutePath) else file?.let { vm.discardFile(it.absolutePath) }
        cameraTarget = null
    }
    val dismiss = {
        images.filterIsInstance<PendingImage.FromFile>().forEach { vm.discardFile(it.path) }
        onDismiss()
    }

    OrganiSheet(dismiss) {
        SheetHeader("Neue Notiz", EntryType.Note, dismiss)
        TitleField(title, { title = it }, "Titel", autoFocus = true)
        PaperTextField(body, { body = it }, "Gedanken, Listen, Ideen …", minHeight = 168.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            PillButton("Foto", {
                val (file, uri) = vm.newCameraTarget()
                cameraTarget = file
                camera.launch(uri)
            }, variant = PillVariant.Soft, small = true, icon = Ms.photoCamera)
            PillButton("Galerie", {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, variant = PillVariant.Soft, small = true, icon = Ms.image)
        }
        if (images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(images) { img ->
                    Box(Modifier.size(84.dp).clip(OrganiShapes.small).background(c.surface2)) {
                        val model: Any = when (img) {
                            is PendingImage.FromUri -> img.uri
                            is PendingImage.FromFile -> File(img.path)
                        }
                        AsyncImage(model, "Bild", Modifier.size(84.dp), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd)) {
                            PlainIconButton(Ms.close, "Bild entfernen", {
                                images.remove(img)
                                if (img is PendingImage.FromFile) vm.discardFile(img.path)
                            }, tint = androidx.compose.ui.graphics.Color.White, iconSize = 16.dp)
                        }
                    }
                }
            }
        }

        PickerField(
            Ms.notifications,
            "Erinnerung",
            reminder?.let { "${Dates.relativeDay(it.toLocalDate(), LocalDate.now())}, ${Dates.time(it)}" } ?: "Keine",
            { picker = "date" },
            onClear = if (reminder != null) ({ reminder = null }) else null,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Schneller Gedanke", style = OrganiTheme.type.rowTitle, color = c.text)
                Text("Als Entwurf ablegen, ohne Ordnung", style = OrganiTheme.type.rowMeta, color = c.textSoft)
            }
            SoftSwitch(draft, { draft = it }, contentDescription = "Als Entwurf ablegen")
        }

        SheetActions(
            primary = "Speichern",
            enabled = title.isNotBlank() || body.isNotBlank() || images.isNotEmpty(),
            onPrimary = {
                val note = Note(title = title.trim(), body = body.trim(), reminderAt = reminder, status = if (draft) Status.DRAFT else Status.ACTIVE)
                vm.createNote(
                    note,
                    images.filterIsInstance<PendingImage.FromUri>().map { it.uri },
                    images.filterIsInstance<PendingImage.FromFile>().map { it.path },
                )
                onDismiss()
            },
        )
    }

    when (picker) {
        "date" -> DatePickerSheet(reminder?.toLocalDate() ?: LocalDate.now(), { if (picker == "date") picker = null }, { d ->
            reminder = d.atTime(reminder?.toLocalTime() ?: LocalTime.of(9, 0))
            picker = "time"
        }, minDate = LocalDate.now())
        "time" -> TimePickerSheet(reminder?.toLocalTime() ?: LocalTime.of(9, 0), { picker = null }) { t ->
            reminder = (reminder?.toLocalDate() ?: LocalDate.now()).atTime(t)
            askNotifications()
        }
    }
}
