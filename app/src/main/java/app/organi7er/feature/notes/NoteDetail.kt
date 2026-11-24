package app.organi7er.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.organi7er.core.data.NoteRepository
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Spacing
import app.organi7er.core.designsystem.components.ChoiceChips
import app.organi7er.core.designsystem.components.ConfirmDialog
import app.organi7er.core.designsystem.components.DatePickerSheet
import app.organi7er.core.designsystem.components.FieldLabel
import app.organi7er.core.designsystem.components.GlassCircleButton
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.MsIcon
import app.organi7er.core.designsystem.components.PaperTextField
import app.organi7er.core.designsystem.components.PickerField
import app.organi7er.core.designsystem.components.PlainIconButton
import app.organi7er.core.designsystem.components.SoftIconButton
import app.organi7er.core.designsystem.components.TimePickerSheet
import app.organi7er.core.designsystem.components.TitleField
import app.organi7er.core.designsystem.components.knob
import app.organi7er.core.designsystem.components.rememberNotificationPermission
import app.organi7er.core.model.Dates
import app.organi7er.core.model.Dates.toLocal
import app.organi7er.core.model.Note
import app.organi7er.core.model.NoteImage
import app.organi7er.core.model.NoteSubStatus
import app.organi7er.core.model.NoteWithImages
import app.organi7er.core.model.Status
import app.organi7er.feature.editor.LocalAppActions
import coil3.compose.AsyncImage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = NoteDetailViewModel.Factory::class)
class NoteDetailViewModel @AssistedInject constructor(
    @Assisted private val id: Long,
    private val repo: NoteRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(id: Long): NoteDetailViewModel
    }

    val note: StateFlow<NoteWithImages?> = repo.observe(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Texteingaben werden entprellt gespeichert (500 ms), damit nicht jeder Tastendruck schreibt. */
    private val edits = MutableStateFlow<Pair<String, String>?>(null)

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            edits.filterNotNull().drop(0).debounce(500).collect { (title, body) -> persistText(title, body) }
        }
    }

    fun edit(title: String, body: String) {
        edits.value = title to body
    }

    private suspend fun persistText(title: String, body: String) {
        val current = repo.get(id) ?: return
        if (current.title != title || current.body != body) repo.save(current.copy(title = title, body = body))
    }

    fun setFocused(focused: Boolean) = viewModelScope.launch { repo.setFocused(id, focused) }

    fun flush() = viewModelScope.launch { edits.value?.let { (t, b) -> persistText(t, b) } }

    fun update(transform: (Note) -> Note) = viewModelScope.launch {
        edits.value?.let { (t, b) -> persistText(t, b) }
        repo.get(id)?.let { repo.save(transform(it)) }
    }

    fun removeImage(image: NoteImage) = viewModelScope.launch { repo.removeImage(image) }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        repo.delete(id)
        onDone()
    }

    suspend fun initial(): NoteWithImages? = note.filterNotNull().first()
}

@Composable
fun NoteDetailScreen(id: Long) {
    val vm = hiltViewModel<NoteDetailViewModel, NoteDetailViewModel.Factory>(key = "note-$id") { it.create(id) }
    val data by vm.note.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val c = OrganiTheme.colors
    val askNotifications = rememberNotificationPermission()
    var title by remember { mutableStateOf<String?>(null) }
    var body by remember { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<NoteImage?>(null) }
    var removeImage by remember { mutableStateOf<NoteImage?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(id) {
        vm.initial()?.let {
            title = it.note.title
            body = it.note.body
        }
    }
    val back = {
        vm.flush()
        actions.back()
    }
    BackHandler(onBack = back)

    val note = data?.note
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(Ms.chevronLeft, "Zurück", back)
            Text(
                note?.let { "Notiz · ${Dates.relativeDay(it.createdAt.toLocal().toLocalDate(), LocalDate.now())}, ${Dates.time(it.createdAt.toLocal())}" } ?: "",
                Modifier.weight(1f).padding(horizontal = Spacing.sm),
                style = OrganiTheme.type.label,
                color = c.textSoft,
            )
            val focused = note?.focused == true
            GlassCircleButton(Ms.flag, if (focused) "Nicht mehr fokussieren" else "Fokussieren", { vm.setFocused(!focused) }, size = 40.dp, filled = focused)
            SoftIconButton(Ms.delete, "Notiz löschen", { confirmDelete = true })
        }

        // Bilder oben als horizontaler Stapel
        val images = data?.sortedImages.orEmpty()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(images, key = { it.id }) { img ->
                AsyncImage(
                    File(img.filePath),
                    "Bild öffnen",
                    Modifier
                        .size(width = 120.dp, height = 156.dp)
                        .clip(OrganiShapes.medium)
                        .clickable(role = Role.Image, onClickLabel = "Vollbild") { viewer = img },
                    contentScale = ContentScale.Crop,
                )
            }
            item(key = "add") {
                Column(
                    Modifier.size(width = 120.dp, height = 156.dp).clip(OrganiShapes.medium).background(c.brandBlue.copy(alpha = 0.07f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    Box(
                        Modifier.size(42.dp).knob(OrganiShapes.circle).clickable(role = Role.Button) { actions.takePhoto(id) }.semantics { contentDescription = "Foto aufnehmen" },
                        contentAlignment = Alignment.Center,
                    ) { MsIcon(Ms.photoCamera, null, tint = c.text, size = 20.dp) }
                    Box(
                        Modifier.size(42.dp).knob(OrganiShapes.circle).clickable(role = Role.Button) { actions.pickImages(id) }.semantics { contentDescription = "Bild aus Galerie" },
                        contentAlignment = Alignment.Center,
                    ) { MsIcon(Ms.image, null, tint = c.text, size = 20.dp) }
                }
            }
        }

        val t = title
        val b = body
        if (t != null && b != null && note != null) {
            TitleField(t, {
                title = it
                vm.edit(it, body.orEmpty())
            }, "Titel")
            PaperTextField(b, {
                body = it
                vm.edit(title.orEmpty(), it)
            }, "Schreib los …", minHeight = 320.dp)

            PickerField(
                Ms.notifications,
                "Erinnerung",
                note.reminderAt?.let { "${Dates.relativeDay(it.toLocalDate(), LocalDate.now())}, ${Dates.time(it)}" } ?: "Keine",
                { picker = "date" },
                onClear = if (note.reminderAt != null) ({ vm.update { it.copy(reminderAt = null) } }) else null,
            )
            FieldLabel("Status")
            ChoiceChips(
                listOf("draft", "active", "progress", "done"),
                when {
                    note.status == Status.DRAFT -> "draft"
                    note.status == Status.DONE -> "done"
                    note.subStatus == NoteSubStatus.IN_PROGRESS -> "progress"
                    else -> "active"
                },
                { mapOf("draft" to "Entwurf", "active" to "Aktiv", "progress" to "In Arbeit", "done" to "Erledigt")[it]!! },
                { key ->
                    vm.update {
                        when (key) {
                            "draft" -> it.copy(status = Status.DRAFT, subStatus = null)
                            "progress" -> it.copy(status = Status.ACTIVE, subStatus = NoteSubStatus.IN_PROGRESS)
                            "done" -> it.copy(status = Status.DONE, subStatus = null)
                            else -> it.copy(status = Status.ACTIVE, subStatus = null)
                        }
                    }
                },
            )
        }
    }

    viewer?.let { img ->
        ImageViewer(img.filePath, onClose = { viewer = null }, onRemove = {
            removeImage = img
            viewer = null
        })
    }
    removeImage?.let { img ->
        ConfirmDialog("Bild entfernen?", "Das Bild wird aus der Notiz gelöscht.", "Entfernen", { removeImage = null }) { vm.removeImage(img) }
    }
    if (confirmDelete) {
        ConfirmDialog("Notiz löschen?", "Die Notiz und ihre Bilder werden endgültig entfernt.", "Löschen", { confirmDelete = false }) {
            vm.delete { actions.back() }
        }
    }
    when (picker) {
        "date" -> DatePickerSheet(note?.reminderAt?.toLocalDate() ?: LocalDate.now(), { if (picker == "date") picker = null }, { d ->
            pendingDate = d
            picker = "time"
        }, minDate = LocalDate.now())
        "time" -> TimePickerSheet(note?.reminderAt?.toLocalTime() ?: LocalTime.of(9, 0), { picker = null }) { time ->
            val at: LocalDateTime = (pendingDate ?: LocalDate.now()).atTime(time)
            vm.update { it.copy(reminderAt = at) }
            askNotifications()
        }
    }
}

/** Vollbild mit Zoom (Pinch) und Verschieben. */
@Composable
private fun ImageViewer(path: String, onClose: () -> Unit, onRemove: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xF2030A18))) {
            AsyncImage(
                File(path),
                "Bild",
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit,
            )
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                PlainIconButton(Ms.delete, "Bild entfernen", onRemove, tint = Color.White, iconSize = 22.dp)
                PlainIconButton(Ms.close, "Schließen", onClose, tint = Color.White, iconSize = 24.dp)
            }
        }
    }
}
