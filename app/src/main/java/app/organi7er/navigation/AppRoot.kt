package app.organi7er.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import app.organi7er.core.designsystem.components.ExtendedAddButton
import app.organi7er.core.designsystem.components.FloatingTabBar
import app.organi7er.core.designsystem.components.Ms
import app.organi7er.core.designsystem.components.TabBarFade
import app.organi7er.core.designsystem.components.TabBarHeight
import app.organi7er.core.designsystem.components.TabItem
import app.organi7er.core.designsystem.components.aura
import app.organi7er.feature.calendar.CalendarScreen
import app.organi7er.feature.catalog.CatalogScreen
import app.organi7er.feature.editor.AppActions
import app.organi7er.feature.editor.EditorRequest
import app.organi7er.feature.editor.EditorViewModel
import app.organi7er.feature.editor.EventSheet
import app.organi7er.feature.editor.LocalAppActions
import app.organi7er.feature.editor.NoteSheet
import app.organi7er.feature.editor.TaskSheet
import app.organi7er.feature.notes.NoteDetailScreen
import app.organi7er.feature.notes.NotesScreen
import app.organi7er.feature.overview.OverviewScreen
import app.organi7er.feature.settings.SettingsScreen
import app.organi7er.feature.tasks.TasksScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object OverviewRoute

@Serializable data object CalendarRoute

@Serializable data object TasksRoute

@Serializable data object NotesRoute

@Serializable data object SettingsRoute

@Serializable data object CatalogRoute

@Serializable data class NoteDetailRoute(val id: Long)

private val Tabs = listOf(
    TabItem("Übersicht", Ms.home, Ms.homeFilled),
    TabItem("Termine", Ms.calendarToday, Ms.calendarTodayFilled),
    TabItem("Aufgaben", Ms.checklist, Ms.checklistFilled),
    TabItem("Notizen", Ms.description, Ms.descriptionFilled),
)
private val TabRoutes: List<Any> = listOf(OverviewRoute, CalendarRoute, TasksRoute, NotesRoute)

/** Platz, den Inhalte unten freihalten müssen (Leiste + Add-Button). */
val ContentBottomPadding: Dp = TabBarHeight + 16.dp + 56.dp + 40.dp

@Composable
fun OrganiAppRoot() {
    val nav = rememberNavController()
    val editor: EditorViewModel = hiltViewModel()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val contentHaze = rememberHazeState()
    var request by remember { mutableStateOf<EditorRequest?>(null) }

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
    val fabType = when (tabIndex) {
        1 -> EntryType.Event
        2 -> EntryType.Task
        3 -> EntryType.Note
        else -> null
    }

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
            openEditor = { request = it },
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
            openNote = { id -> nav.navigate(NoteDetailRoute(id)) },
            openTab = { i ->
                nav.navigate(TabRoutes[i]) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            openSettings = { nav.navigate(SettingsRoute) },
            openCatalog = { nav.navigate(CatalogRoute) },
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
    val fabBottom by animateDpAsState(
        if (barVisible) navInset + 16.dp + TabBarHeight + 16.dp else navInset + 16.dp,
        tween(250),
        label = "fabBottom",
    )

    CompositionLocalProvider(LocalAppActions provides actions) {
        Box(Modifier.fillMaxSize()) {
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
                    composable<SettingsRoute> { SettingsScreen() }
                    composable<CatalogRoute> { CatalogScreen() }
                    composable<NoteDetailRoute> { NoteDetailScreen(it.toRoute<NoteDetailRoute>().id) }
                }
            }

            StatusBarScrim(Modifier.align(Alignment.TopCenter))

            AnimatedVisibility(
                visible = barVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                TabBarFade(navInset + 16.dp + TabBarHeight + 16.dp)
            }

            AnimatedVisibility(
                visible = barVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(250)) { it * 2 } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(250)) { it * 2 } + fadeOut(tween(200)),
            ) {
                FloatingTabBar(
                    items = Tabs,
                    selected = tabIndex.coerceAtLeast(0),
                    onSelect = { i -> if (i == tabIndex) collapsed = false else actions.openTab(i) },
                    hazeState = contentHaze,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = navInset + 16.dp),
                )
            }

            if (fabType != null) {
                ExtendedAddButton(
                    type = fabType,
                    expanded = !collapsed,
                    onClick = {
                        request = when (fabType) {
                            EntryType.Event -> EditorRequest.NewEvent()
                            EntryType.Task -> EditorRequest.NewTask()
                            EntryType.Note -> EditorRequest.NewNote
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = fabBottom),
                )
            }

            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (showBar) navInset + TabBarHeight + 28.dp else navInset + 16.dp),
            ) { data ->
                val c = OrganiTheme.colors
                val actionColor = if (c.isDark) Primitives.Blue600 else c.accent
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

/** Weicher Verlauf hinter der Statusleiste, damit scrollender Inhalt nicht mit der Uhr kollidiert. */
@Composable
private fun StatusBarScrim(modifier: Modifier = Modifier) {
    val bg = OrganiTheme.colors.bg
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier
            .fillMaxWidth()
            .height(top + 12.dp)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to bg.copy(alpha = 0.92f), 0.7f to bg.copy(alpha = 0.75f), 1f to androidx.compose.ui.graphics.Color.Transparent)),
    )
}
