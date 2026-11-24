package app.organi7er.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme
import app.organi7er.core.designsystem.Primitives
import app.organi7er.core.designsystem.components.CaptureDock
import app.organi7er.core.designsystem.components.DockMinHeight
import app.organi7er.core.designsystem.components.EdgePicker
import app.organi7er.core.designsystem.components.EdgeBlue
import app.organi7er.core.designsystem.components.EdgeGold
import app.organi7er.core.designsystem.components.EdgeNavy
import app.organi7er.core.designsystem.components.EdgeSilver
import app.organi7er.core.designsystem.components.EdgeTab
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.aura
import app.organi7er.core.designsystem.components.rest
import app.organi7er.core.designsystem.components.shortLabel
import app.organi7er.core.designsystem.components.title
import app.organi7er.feature.calendar.CalendarScreen
import app.organi7er.feature.catalog.CatalogScreen
import app.organi7er.feature.common.LocalShell
import app.organi7er.feature.editor.AppActions
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.EditorViewModel
import app.organi7er.feature.editor.EventSheet
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.feature.editor.NoteSheet
import app.organi7er.feature.editor.TaskSheet
import app.organi7er.feature.notes.NoteDetailScreen
import app.organi7er.feature.notes.NotesScreen
import app.organi7er.feature.notifications.NotificationsScreen
import app.organi7er.feature.overview.OverviewScreen
import app.organi7er.feature.search.SearchScreen
import app.organi7er.feature.search.SearchViewModel
import app.organi7er.feature.settings.SettingsScreen
import app.organi7er.feature.tasks.TasksScreen
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object OverviewRoute

@Serializable data object CalendarRoute

@Serializable data object TasksRoute

@Serializable data object NotesRoute

@Serializable data object CatalogRoute

@Serializable data class NoteDetailRoute(val id: Long)

@Serializable data object NotificationsRoute

private val Tabs = listOf(
    EdgeTab("Übersicht", Ms.home, Ms.homeFilled, EdgeSilver),
    EdgeTab("Termine", Ms.calendarToday, Ms.calendarTodayFilled, EdgeNavy),
    EdgeTab("Aufgaben", Ms.checklist, Ms.checklistFilled, EdgeBlue),
    EdgeTab("Notizen", Ms.description, Ms.descriptionFilled, EdgeGold),
)
private val TabRoutes: List<Any> = listOf(OverviewRoute, CalendarRoute, TasksRoute, NotesRoute)

/** Platz, den Inhalte unten freihalten müssen (angedocktes Eingabefeld, beim Scrollen ohne Begrüßung). */
val ContentBottomPadding: Dp = 160.dp

@Composable
fun OrganiAppRoot() {
    val nav = rememberNavController()
    val editor: EditorViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contentHaze = rememberHazeState()
    var request by remember { mutableStateOf<EditorRequest?>(null) }
    // Suche liegt als eigene Ebene über dem Inhalt (samt Dock), damit das Feld die ganze Breite bekommt.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val search: SearchViewModel = hiltViewModel()

    // Einstellungen als Panel von rechts; links bleibt ein Streifen der Übersicht sichtbar.
    val settings = remember { Animatable(0f) }
    val openSettingsPanel: () -> Unit = { scope.launch { settings.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) } }
    val closeSettingsPanel: () -> Unit = { scope.launch { settings.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) } }

    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination
    val tabIndex = when {
        dest == null -> 0
        dest.hasRoute(OverviewRoute::class) -> 0
        dest.hasRoute(CalendarRoute::class) -> 1
        dest.hasRoute(TasksRoute::class) -> 2
        dest.hasRoute(NotesRoute::class) -> 3
        else -> -1
    }
    // Schnelles Erfassen: Dock unten, Eingabe direkt im Dock.
    // Der Typ des Schnell-Eintrags folgt dem Reiter.
    val dockType = when (tabIndex) {
        1 -> EntryType.Event
        2 -> EntryType.Task
        else -> EntryType.Note
    }
    val shell by editor.shell.collectAsStateWithLifecycle()
    // Hintergrund-Glow je Reiter: Übersicht neutral, Termine Navy, Aufgaben Blau, Notizen Gold.
    val glowTarget = when (tabIndex) {
        1 -> EdgeNavy.mid
        2 -> EdgeBlue.inner
        3 -> EdgeGold.mid
        else -> EdgeSilver.inner
    }
    val glow by animateColorAsState(glowTarget, tween(400), label = "tabGlow")

    // Bilder für Notizen direkt aus dem Feed (Platzhalter „Foto aufnehmen / oder aus Galerie“).
    var imageTarget by remember { mutableStateOf<Long?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        imageTarget?.let { id -> if (uris.isNotEmpty()) editor.addImages(id, uris) }
        imageTarget = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val id = imageTarget
        val file = cameraFile
        if (ok && id != null && file != null) editor.addImageFile(id, file.absolutePath) else file?.let { editor.discardFile(it.absolutePath) }
        imageTarget = null
        cameraFile = null
    }

    val actions = remember(nav, editor) {
        AppActions(
            openEditor = { r ->
                when (r) {
                    is EditorRequest.EditEvent -> editor.markOpened(EntryType.Event, r.id)
                    is EditorRequest.EditTask -> editor.markOpened(EntryType.Task, r.id)
                    else -> Unit
                }
                request = r
            },
            toggleTask = { task ->
                val done = task.status != app.organi7er.core.model.Status.DONE
                editor.setTaskDone(task.id, done)
                if (done) {
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val r = snackbar.showSnackbar("„${task.title}“ erledigt", "Rückgängig", duration = SnackbarDuration.Short)
                        if (r == SnackbarResult.ActionPerformed) editor.setTaskDone(task.id, false)
                    }
                }
            },
            openNote = { id ->
                editor.markOpened(EntryType.Note, id)
                nav.navigate(NoteDetailRoute(id))
            },
            openTab = { i ->
                nav.navigate(TabRoutes[i]) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            openSettings = { openSettingsPanel() },
            openNotifications = { nav.navigate(NotificationsRoute) { launchSingleTop = true } },
            openSearch = { searchOpen = true },
            openCatalog = {
                scope.launch { settings.snapTo(0f) }
                nav.navigate(CatalogRoute)
            },
            back = { nav.popBackStack() },
            takePhoto = { id ->
                val (file, uri) = editor.newCameraTarget()
                imageTarget = id
                cameraFile = file
                camera.launch(uri)
            },
            pickImages = { id ->
                imageTarget = id
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            showMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
        )
    }

    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val showBar = tabIndex >= 0

    CompositionLocalProvider(LocalAppActions provides actions, LocalShell provides shell) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val travel = with(LocalDensity.current) { (screenWidth - SettingsPeek).toPx() }
            // Menü liegt hinten; der Inhalt rutscht darüber nach links weg (nicht verkleinert, nicht abgedunkelt).
            SettingsPanel(settings, closeSettingsPanel)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = settings.value
                        translationX = -p * travel
                        // Rechte Kante des Inhalts oben und unten abgerundet, mit Schatten aufs Menü.
                        shape = RoundedCornerShape(topEnd = 40.dp, bottomEnd = 40.dp)
                        clip = p > 0f
                        shadowElevation = 24.dp.toPx() * p
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .hazeSource(contentHaze),
                ) {
                    Box(Modifier.fillMaxSize().aura(glow))
                    NavHost(
                        navController = nav,
                        startDestination = OverviewRoute,
                        enterTransition = { fadeIn(tween(220)) },
                        exitTransition = { fadeOut(tween(180)) },
                    ) {
                        composable<OverviewRoute> { OverviewScreen() }
                        composable<CalendarRoute> { CalendarScreen() }
                        composable<TasksRoute> { TasksScreen() }
                        composable<NotesRoute> { NotesScreen() }
                        composable<NotificationsRoute>(
                            enterTransition = { slideInVertically(tween(280)) { -it } },
                            popExitTransition = { slideOutVertically(tween(240)) { -it } },
                        ) { NotificationsScreen() }
                        composable<CatalogRoute> { CatalogScreen() }
                        composable<NoteDetailRoute> { NoteDetailScreen(it.toRoute<NoteDetailRoute>().id) }
                    }
                }

                // Fester Blur-Bereich hinter Status- und Navigationsleiste, ohne Verlauf.
                SystemBarBlur(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), contentHaze, Modifier.align(Alignment.TopCenter))
                SystemBarBlur(navInset, contentHaze, Modifier.align(Alignment.BottomCenter))

                // Edge-Picker: Navigation am rechten Rand, liegt im Inhalt und rutscht mit ihm weg.
                // Immer sichtbar, auf allen Reiterseiten. Das Fenster passt sich der Tastatur an (adjustResize),
                // der Picker wandert mit dem Dock nach oben.
                if (showBar) {
                    EdgePicker(
                        // Alle Reiter, der aktive steht in der Mitte.
                        items = Tabs,
                        selected = tabIndex,
                        onSelect = { i -> actions.openTab(i) },
                        onSwipeLeft = openSettingsPanel,
                        // Hängt am Dock: das untere Ende der Kerbe läuft an dessen Oberkante in den Rand aus. Bleibt beim Composer stehen.
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = navInset + DockMinHeight - 8.dp),
                    )
                }

                AnimatedVisibility(
                    showBar,
                    Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(240)) { it } + fadeIn(tween(160)),
                    exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(120)),
                ) {
                    CaptureDock(
                        type = dockType,
                        onSave = { e ->
                            editor.quickAdd(e)
                            actions.showMessage("${e.type.shortLabel} gespeichert")
                        },
                        onCamera = { request = EditorRequest.NewNote(startCamera = true) },
                        onMessage = actions.showMessage,
                    )
                }

                // Suche: blendet über allem ein; Logo und Menüknopf weichen, das Feld läuft über die ganze Breite.
                AnimatedVisibility(
                    searchOpen,
                    Modifier.fillMaxSize(),
                    enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { -it / 12 },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 12 },
                ) {
                    val closeSearch = {
                        searchOpen = false
                        search.query.value = ""
                    }
                    SearchScreen(
                        vm = search,
                        onOpen = { hit ->
                            closeSearch()
                            when (hit.type) {
                                EntryType.Event -> actions.openEditor(EditorRequest.EditEvent(hit.id))
                                EntryType.Task -> actions.openEditor(EditorRequest.EditTask(hit.id))
                                EntryType.Note -> actions.openNote(hit.id)
                            }
                        },
                        onClose = closeSearch,
                    )
                }

                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = if (showBar) navInset + DockMinHeight + 12.dp else navInset + 16.dp),
                ) { data ->
                    val c = OrganiTheme.colors
                    val actionColor = if (c.isDark) androidx.compose.ui.graphics.Color(0xFF0A0A0B) else c.accent
                    val label = data.visuals.actionLabel
                    Snackbar(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = OrganiShapes.medium,
                        containerColor = if (c.isDark) Primitives.Paper else c.hero,
                        contentColor = if (c.isDark) c.onAccent else c.onHero,
                        actionContentColor = actionColor,
                        action = if (label == null) {
                            null
                        } else {
                            {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(label, style = OrganiTheme.type.buttonSmall, color = actionColor)
                                }
                            }
                        },
                    ) { Text(data.visuals.message, style = OrganiTheme.type.body) }
                }
                // Offenes Menü: Inhalt nicht bedienbar; Antippen schließt, Ziehen nach rechts auch.
                if (settings.value > 0f || settings.isRunning) {
                    // Erst wenn das Menü fast ganz offen ist, wird der Inhalt ausgegraut.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = ((settings.value - 0.88f) / 0.12f).coerceIn(0f, 1f) * 0.5f }
                            .background(Color.Black),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .draggable(
                                rememberDraggableState { dx -> scope.launch { settings.snapTo((settings.value - dx / travel).coerceIn(0f, 1f)) } },
                                Orientation.Horizontal,
                                onDragStopped = { v -> if (settings.value < 0.7f || v > 1200f) closeSettingsPanel() else openSettingsPanel() },
                            )
                            .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClickLabel = "Menü schließen", onClick = closeSettingsPanel)
                            .semantics { contentDescription = "Menü schließen" },
                    )
                }
            }
        }

        when (val r = request) {
            null -> Unit
            is EditorRequest.NewEvent, is EditorRequest.EditEvent, is EditorRequest.PlanEvent -> EventSheet(r, editor) { request = null }
            is EditorRequest.NewTask, is EditorRequest.EditTask, is EditorRequest.PlanTask -> TaskSheet(r, editor) { request = null }
            is EditorRequest.NewNote -> NoteSheet(r, editor) { request = null }
        }
    }
}

/** Unscharfes, getöntes Band hinter einer Systemleiste: feste Kante, kein Verlauf. */
@Composable
private fun SystemBarBlur(height: Dp, haze: HazeState, modifier: Modifier = Modifier) {
    if (height <= 0.dp) return
    val bg = OrganiTheme.colors.bg
    val style = remember {
        HazeBlurStyle {
            blurRadius(20.dp)
            noiseFactor(0f)
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .hazeBlur(HazeInput.Sources(haze), style)
            .background(bg.copy(alpha = 0.55f)),
    )
}

private val SettingsPeek = 64.dp

/**
 * Menü (Profil + Einstellungen) liegt hinter dem Inhalt. Sichtbar ist es rechts von [SettingsPeek],
 * dem Streifen, auf dem der weggeschobene Inhalt liegen bleibt.
 */
@Composable
private fun SettingsPanel(progress: Animatable<Float, AnimationVector1D>, close: () -> Unit) {
    if (progress.value <= 0f && !progress.isRunning) return
    BackHandler(progress.targetValue > 0f, close)
    // Das Menü ist immer hell, auch wenn die App dunkel ist.
    OrganiTheme(darkTheme = false) {
        Box(Modifier.fillMaxSize().background(OrganiTheme.colors.bg)) {
            SettingsScreen(onBack = close, startInset = SettingsPeek)
        }
    }
}
