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
    /** Glas-Bausteine (Glass.kt). */
    val glassSolid: Color,
    val onGlassSolid: Color,
    val glassChip: Color,
    val glassEdge: Color,
    val insetFill: Color,
    val leadingFill: Color,
    val statSelectedA: Color,
    val statSelectedB: Color,
    val aura3: Color,
    val success: Color,
)

val LightOrganiColors = OrganiColors(
    isDark = false,
    bg = Color(0xFFF2F2F4),
    surface = Color.White,
    surface2 = Color(0xFFE6E6EA),
    glass = Color.White.copy(alpha = 0.5f),
    glassStrong = Color.White.copy(alpha = 0.82f),
    glassLine = Color.White.copy(alpha = 0.9f),
    border = Color(0xFFDADADF),
    text = Color(0xFF0A0A0B),
    textMuted = Color(0xFF55555C),
    textSoft = Color(0xFF75757D),
    primary = Color(0xFF0A0A0B),
    onPrimary = Color.White,
    brandBlue = Color(0xFF0A0A0B),
    link = Color(0xFF0A0A0B),
    accent = Primitives.Yellow500,
    onAccent = Primitives.Navy900,
    hero = Color(0xFF0A0A0B),
    onHero = Color.White,
    onHeroMuted = Color(0xFFB8B8BF),
    nav = Color(0xFF0A0A0B).copy(alpha = 0.86f),
    navIcon = Color(0xFFB8B8BF),
    paper = Color.White,
    paperLine = Color(0xFFE6E6EA),
    paperMargin = Color(0xFFCFCFD5),
    wave = Color(0xFFCFCFD5),
    typeEvent = Color(0xFF0A0A0B),
    onTypeEvent = Color.White,
    typeTask = Color(0xFF0A0A0B),
    onTypeTask = Color.White,
    typeNote = Primitives.Yellow500,
    onTypeNote = Primitives.Navy900,
    calWork = Primitives.Blue500,
    calPrivate = Primitives.Yellow500,
    calHealth = Primitives.Green,
    aura1 = Color.Transparent,
    aura2 = Color.Transparent,
    knobTop = Color.White,
    knobBottom = Color(0xFFF4F4F6),
    danger = Primitives.Danger,
    dangerText = Color(0xFFC2272D),
    shadowSoft = Color.Black.copy(alpha = 0.06f),
    shadowFloat = Color.Black.copy(alpha = 0.14f),
    tileSelected = Color(0xFF0A0A0B),
    onTileSelected = Color.White,
    onTileSelectedMuted = Color(0xFFB8B8BF),
    glassSolid = Color.White,
    onGlassSolid = Color(0xFF0A0A0B),
    glassChip = Color.White.copy(alpha = 0.55f),
    glassEdge = Color.White.copy(alpha = 0.85f),
    insetFill = Color.White.copy(alpha = 0.9f),
    leadingFill = Color.White,
    statSelectedA = Primitives.Navy800,
    statSelectedB = Primitives.Blue700,
    aura3 = Color.White.copy(alpha = 0.9f),
    success = Color(0xFF3F8F55),
)

val DarkOrganiColors = OrganiColors(
    isDark = true,
    bg = Color(0xFF080809),
    surface = Color(0xFF151517),
    surface2 = Color(0xFF1E1E21),
    glass = Color.White.copy(alpha = 0.07f),
    glassStrong = Color.White.copy(alpha = 0.16f),
    glassLine = Color.White.copy(alpha = 0.18f),
    border = Color(0xFF2A2A2E),
    text = Color(0xFFF5F5F7),
    textMuted = Color(0xFFB4B4BB),
    textSoft = Color(0xFF8A8A92),
    primary = Color.White,
    onPrimary = Color(0xFF0A0A0B),
    brandBlue = Color.White,
    link = Color.White,
    accent = Primitives.Yellow500,
    onAccent = Primitives.Navy900,
    hero = Color(0xFF1E1E21),
    onHero = Color.White,
    onHeroMuted = Color(0xFFB4B4BB),
    nav = Color(0xFF151517).copy(alpha = 0.82f),
    navIcon = Color(0xFF8A8A92),
    paper = Color(0xFF151517),
    paperLine = Color(0xFF26262A),
    paperMargin = Color(0xFF3A3A40),
    wave = Color(0xFF3A3A40),
    typeEvent = Color(0xFFF5F5F7),
    onTypeEvent = Primitives.Navy900,
    typeTask = Color(0xFFF5F5F7),
    onTypeTask = Color(0xFF0A0A0B),
    typeNote = Primitives.Yellow500,
    onTypeNote = Primitives.Navy900,
    calWork = Primitives.Blue500,
    calPrivate = Primitives.Yellow500,
    calHealth = Primitives.Green,
    aura1 = Color.Transparent,
    aura2 = Color.Transparent,
    knobTop = Color(0xFF2E2E33),
    knobBottom = Color(0xFF232327),
    danger = Primitives.Danger,
    dangerText = Color(0xFFFF8A8E),
    shadowSoft = Color.Black.copy(alpha = 0.4f),
    shadowFloat = Color.Black.copy(alpha = 0.55f),
    tileSelected = Color(0xFFF5F5F7),
    onTileSelected = Color(0xFF0A0A0B),
    onTileSelectedMuted = Color(0xFF55555C),
    glassSolid = Color.White.copy(alpha = 0.95f),
    onGlassSolid = Color(0xFF0A0A0B),
    glassChip = Color.White.copy(alpha = 0.08f),
    glassEdge = Color.White.copy(alpha = 0.14f),
    insetFill = Color.White.copy(alpha = 0.07f),
    leadingFill = Color.White.copy(alpha = 0.08f),
    statSelectedA = Primitives.Navy700,
    statSelectedB = Color(0xFF2B4C9A),
    aura3 = Color.Transparent,
    success = Color(0xFF6EE7A0),
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
