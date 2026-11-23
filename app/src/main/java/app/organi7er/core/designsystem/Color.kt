package app.organi7er.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Primitive Farbskalen aus design/designsystem.html (`:root`).
 * Kernfarben (60 / 30 / 10): Navy 900, Blue 500, Yellow 500.
 * Screens greifen nie direkt hierauf zu, sondern auf [OrganiColors].
 */
object Primitives {
    val Navy50 = Color(0xFFEEF2F8)
    val Navy100 = Color(0xFFD9E1EE)
    val Navy200 = Color(0xFFB3C2DB)
    val Navy300 = Color(0xFF8399BE)
    val Navy400 = Color(0xFF536E9C)
    val Navy500 = Color(0xFF2E4C7E)
    val Navy600 = Color(0xFF1A3665)
    val Navy700 = Color(0xFF0F2A55)
    val Navy800 = Color(0xFF0A2248)
    val Navy900 = Color(0xFF061B3A)
    val Navy950 = Color(0xFF03112A)

    val Blue50 = Color(0xFFEFF5FF)
    val Blue100 = Color(0xFFDBE8FE)
    val Blue200 = Color(0xFFBFD6FE)
    val Blue300 = Color(0xFF93BBFD)
    val Blue400 = Color(0xFF60A5FA)
    val Blue500 = Color(0xFF3B82F6)
    val Blue600 = Color(0xFF2563EB)
    val Blue700 = Color(0xFF1D4ED8)
    val Blue800 = Color(0xFF1E40AF)
    val Blue900 = Color(0xFF1E3A8A)

    val Yellow50 = Color(0xFFFFFBE0)
    val Yellow100 = Color(0xFFFFF5B8)
    val Yellow200 = Color(0xFFFFEC80)
    val Yellow300 = Color(0xFFFFE34D)
    val Yellow400 = Color(0xFFFFDB26)
    val Yellow500 = Color(0xFFFFD500)
    val Yellow600 = Color(0xFFE0B800)
    val Yellow700 = Color(0xFFB39200)
    val Yellow800 = Color(0xFF806800)
    val Yellow900 = Color(0xFF4D3F00)

    val Success = Color(0xFF16A34A)
    val Danger = Color(0xFFE5484D)
    val Green = Color(0xFF22C55E)
    val Paper = Color(0xFFF4F7FB)
}

/** Semantische Tokens. Im Code nur diese Namen verwenden. */
@Immutable
data class OrganiColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassLine: Color,
    val border: Color,
    val text: Color,
    val textMuted: Color,
    val textSoft: Color,
    val primary: Color,
    val onPrimary: Color,
    val brandBlue: Color,
    val link: Color,
    val accent: Color,
    val onAccent: Color,
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val nav: Color,
    val navIcon: Color,
    val paper: Color,
    val paperLine: Color,
    val paperMargin: Color,
    val wave: Color,
    val typeEvent: Color,
    val onTypeEvent: Color,
    val typeTask: Color,
    val onTypeTask: Color,
    val typeNote: Color,
    val onTypeNote: Color,
    val calWork: Color,
    val calPrivate: Color,
    val calHealth: Color,
    val aura1: Color,
    val aura2: Color,
    val knobTop: Color,
    val knobBottom: Color,
    val danger: Color,
    val dangerText: Color,
    val shadowSoft: Color,
    val shadowFloat: Color,
    /** Gewählte Briefing-Kachel: hell Navy, dunkel fast Weiß. */
    val tileSelected: Color,
    val onTileSelected: Color,
    val onTileSelectedMuted: Color,
)

val LightOrganiColors = OrganiColors(
    isDark = false,
    bg = Color(0xFFF4F7FB),
    surface = Color.White,
    surface2 = Primitives.Navy50,
    glass = Color.White.copy(alpha = 0.62f),
    glassStrong = Color.White.copy(alpha = 0.82f),
    glassLine = Color.White.copy(alpha = 0.9f),
    border = Color(0xFFD7DCE5),
    text = Primitives.Navy900,
    textMuted = Color(0xFF4A5B78),
    textSoft = Color(0xFF5E6D88),
    primary = Primitives.Blue600,
    onPrimary = Color.White,
    brandBlue = Primitives.Blue500,
    link = Primitives.Blue600,
    accent = Primitives.Yellow500,
    onAccent = Primitives.Navy900,
    hero = Primitives.Navy900,
    onHero = Color.White,
    onHeroMuted = Primitives.Navy200,
    nav = Primitives.Navy900.copy(alpha = 0.86f),
    navIcon = Primitives.Navy200,
    paper = Color.White,
    paperLine = Primitives.Blue100,
    paperMargin = Primitives.Blue300,
    wave = Primitives.Navy200,
    typeEvent = Primitives.Navy900,
    onTypeEvent = Color.White,
    typeTask = Primitives.Blue600,
    onTypeTask = Color.White,
    typeNote = Primitives.Yellow500,
    onTypeNote = Primitives.Navy900,
    calWork = Primitives.Blue500,
    calPrivate = Primitives.Yellow500,
    calHealth = Primitives.Green,
    aura1 = Primitives.Blue500.copy(alpha = 0.22f),
    aura2 = Primitives.Navy900.copy(alpha = 0.10f),
    knobTop = Color.White,
    knobBottom = Color(0xFFE9EEF6),
    danger = Primitives.Danger,
    dangerText = Color(0xFFC2272D),
    shadowSoft = Primitives.Navy900.copy(alpha = 0.08f),
    shadowFloat = Primitives.Navy900.copy(alpha = 0.18f),
    tileSelected = Primitives.Navy900,
    onTileSelected = Color.White,
    onTileSelectedMuted = Primitives.Navy200,
)

val DarkOrganiColors = OrganiColors(
    isDark = true,
    bg = Primitives.Navy950,
    surface = Primitives.Navy900,
    surface2 = Primitives.Navy700,
    glass = Primitives.Navy700.copy(alpha = 0.55f),
    glassStrong = Primitives.Navy700.copy(alpha = 0.78f),
    glassLine = Primitives.Blue300.copy(alpha = 0.16f),
    border = Primitives.Navy600,
    text = Primitives.Paper,
    textMuted = Primitives.Navy200,
    textSoft = Primitives.Navy300,
    primary = Primitives.Blue600,
    onPrimary = Color.White,
    brandBlue = Primitives.Blue400,
    link = Primitives.Blue400,
    accent = Primitives.Yellow500,
    onAccent = Primitives.Navy900,
    hero = Primitives.Navy700,
    onHero = Color.White,
    onHeroMuted = Primitives.Navy200,
    nav = Primitives.Navy800.copy(alpha = 0.82f),
    navIcon = Primitives.Navy300,
    paper = Primitives.Navy800,
    paperLine = Primitives.Navy600,
    paperMargin = Primitives.Navy500,
    wave = Primitives.Navy500,
    typeEvent = Primitives.Paper,
    onTypeEvent = Primitives.Navy900,
    typeTask = Primitives.Blue600,
    onTypeTask = Color.White,
    typeNote = Primitives.Yellow500,
    onTypeNote = Primitives.Navy900,
    calWork = Primitives.Blue500,
    calPrivate = Primitives.Yellow500,
    calHealth = Primitives.Green,
    aura1 = Primitives.Blue500.copy(alpha = 0.28f),
    aura2 = Primitives.Yellow500.copy(alpha = 0.05f),
    knobTop = Color(0xFF14305C),
    knobBottom = Primitives.Navy800,
    danger = Primitives.Danger,
    dangerText = Color(0xFFFF8A8E),
    shadowSoft = Color(0xFF010814).copy(alpha = 0.35f),
    shadowFloat = Color(0xFF010814).copy(alpha = 0.5f),
    tileSelected = Primitives.Paper,
    onTileSelected = Primitives.Navy900,
    onTileSelectedMuted = Color(0xFF4A5B78),
)

/** Standardfarben der Kalender (Hex wie in der Datenbank gespeichert). */
object CalendarPalette {
    const val WORK = "#3B82F6"
    const val PRIVATE = "#FFD500"
    const val HEALTH = "#22C55E"
    val choices = listOf("#3B82F6", "#FFD500", "#22C55E", "#2563EB", "#93BBFD", "#E5484D", "#8399BE", "#E0B800")
}

fun parseHexColor(hex: String, fallback: Color = Primitives.Blue500): Color = runCatching {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    if (clean.length == 6) Color(0xFF000000 or value) else Color(value)
}.getOrDefault(fallback)
