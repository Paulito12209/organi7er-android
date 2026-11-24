package app.organi7er.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import app.organi7er.core.designsystem.components.EntryType
import app.organi7er.core.designsystem.components.FusedTabBar
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.QuickCaptureOverlay
import app.organi7er.core.designsystem.components.TabBarHeight
import app.organi7er.core.designsystem.components.TabItem
import app.organi7er.core.designsystem.components.aura
import app.organi7er.core.designsystem.components.floatShadow
import app.organi7er.core.designsystem.components.plusCenterFromEnd
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
    TabItem("Übersicht", Ms.home, Ms.homeFilled),
    TabItem("Termine", Ms.calendarToday, Ms.calendarTodayFilled),
    TabItem("Aufgaben", Ms.checklist, Ms.checklistFilled),
    TabItem("Notizen", Ms.description, Ms.descriptionFilled),
)
private val TabRoutes: List<Any> = listOf(OverviewRoute, CalendarRoute, TasksRoute, NotesRoute)

/** Platz, den Inhalte unten freihalten müssen (Leiste + Add-Button). */
val ContentBottomPadding: Dp = TabBarHeight + 16.dp + 48.dp

@Composable
fun OrganiAppRoot() {
    val nav = rememberNavController()
    val editor: EditorViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contentHaze = rememberHazeState()
    var request by remember { mutableStateOf<EditorRequest?>(null) }

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
    var captureOpen by remember { mutableStateOf(false) }
    LaunchedEffect(dest?.id) { captureOpen = false }
    val shell by editor.shell.collectAsStateWithLifecycle()

    // Einklappen beim Scrollen: Leiste weg, nur der Anlege-Knopf bleibt (Schwelle ~8 dp gegen Flackern).
    var collapsed by remember { mutableStateOf(false) }
    LaunchedEffect(dest?.id) { collapsed = false }
    val threshold = with(LocalDensity.current) { 8.dp.toPx() }
    val scrollConnection = remember(threshold) {
        object : NestedScrollConnection {
            var acc = 0f
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f && (acc == 0f || (acc > 0) == (available.y > 0))) acc += available.y else acc = available.y
                if (acc < -threshold) {
                    collapsed = true
                    acc = 0f
                } else if (acc > threshold) {
                    collapsed = false
                    acc = 0f
                }
                return Offset.Zero
            }

            // Ganz oben angekommen (Restbewegung nach unten wird nicht mehr verbraucht): Leiste zurückholen.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) collapsed = false
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (available.y > 0f) collapsed = false
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

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
    val barVisible = showBar && !collapsed

    CompositionLocalProvider(LocalAppActions provides actions, LocalShell provides shell) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = settings.value
                        scaleX = 1f - 0.06f * p
                        scaleY = 1f - 0.06f * p
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        // Hauptinhalt wird beim Öffnen des Menüs zur abgerundeten Karte.
                        shape = RoundedCornerShape((36f * p).coerceAtLeast(0f).dp)
                        clip = p > 0f
                    },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .hazeSource(contentHaze)
                        .nestedScroll(scrollConnection),
                ) {
                    Box(Modifier.fillMaxSize().aura())
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

                QuickCaptureOverlay(
                    visible = captureOpen,
                    onDismiss = { captureOpen = false },
                    onPick = { type ->
                        captureOpen = false
                        request = when (type) {
                            EntryType.Event -> EditorRequest.NewEvent()
                            EntryType.Task -> EditorRequest.NewTask()
                            EntryType.Note -> EditorRequest.NewNote
                        }
                    },
                    plusEnd = plusCenterFromEnd(screenWidth - 32.dp, Tabs.size) + 16.dp,
                    plusBottom = navInset + 16.dp + TabBarHeight / 2,
                    hazeState = contentHaze,
                )

                if (showBar) {
                    FusedTabBar(
                        items = Tabs,
                        selected = tabIndex,
                        onSelect = { i ->
                            captureOpen = false
                            if (i == tabIndex) collapsed = false else actions.openTab(i)
                        },
                        onAdd = { captureOpen = !captureOpen },
                        addOpen = captureOpen,
                        showTabs = barVisible && !captureOpen,
                        hazeState = contentHaze,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = navInset + 16.dp),
                    )
                }

                SnackbarHost(
                    snackbar,
                    Modifier.align(Alignment.BottomCenter).padding(bottom = if (showBar) navInset + TabBarHeight + 28.dp else navInset + 16.dp),
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
                // Abdunkeln der Übersicht hinter dem Einstellungs-Panel.
                if (settings.value > 0f) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = settings.value }.background(Color.Black.copy(alpha = 0.35f)))
                }
            }
            SettingsPanel(settings, screenWidth, openSettingsPanel, closeSettingsPanel)
        }

        when (val r = request) {
            null -> Unit
            is EditorRequest.NewEvent, is EditorRequest.EditEvent, is EditorRequest.PlanEvent -> EventSheet(r, editor) { request = null }
            is EditorRequest.NewTask, is EditorRequest.EditTask, is EditorRequest.PlanTask -> TaskSheet(r, editor) { request = null }
            EditorRequest.NewNote -> NoteSheet(editor) { request = null }
        }
    }
}

/** Gemeinsame Innenabstände der Reiterseiten. */
@Composable
fun screenPadding(top: Dp = 8.dp): PaddingValues {
    val status = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 18.dp, end = 18.dp, top = top, bottom = ContentBottomPadding + status)
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
 * Einstellungen als Panel: schiebt sich von rechts herein und lässt links [SettingsPeek] der Übersicht frei.
 * Antippen des Streifens oder Wischen nach rechts schließt es.
 */
@Composable
private fun SettingsPanel(progress: Animatable<Float, AnimationVector1D>, screenWidth: Dp, open: () -> Unit, close: () -> Unit) {
    if (progress.value <= 0f && !progress.isRunning) return
    val density = LocalDensity.current
    val travel = with(density) { (screenWidth - SettingsPeek).toPx() }
    val scope = rememberCoroutineScope()
    BackHandler(progress.targetValue > 0f, close)
    val shape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(SettingsPeek)
                .clickable(onClickLabel = "Zur Übersicht", onClick = close)
                .semantics { contentDescription = "Einstellungen schließen" },
        )
        Box(
            Modifier
                .padding(start = SettingsPeek)
                .fillMaxSize()
                .graphicsLayer { translationX = (1f - progress.value) * travel }
                .draggable(
                    rememberDraggableState { dx -> scope.launch { progress.snapTo((progress.value - dx / travel).coerceIn(0f, 1f)) } },
                    Orientation.Horizontal,
                    onDragStopped = { v -> if (progress.value < 0.7f || v > 1200f) close() else open() },
                )
                .floatShadow(shape, OrganiTheme.colors.shadowFloat)
                .clip(shape)
                .background(OrganiTheme.colors.bg),
        ) {
            SettingsScreen(onBack = close)
        }
    }
}
