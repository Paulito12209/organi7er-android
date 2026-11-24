package app.organi7er.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.organi7er.R

val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
)

private val Centered = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(weight: FontWeight, size: Float, line: Float, tracking: Float = 0f) = TextStyle(
    fontFamily = Outfit,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = Centered,
)

/** Typografie aus der Vorlage. Kein Gewicht über 600. */
@Immutable
object OrganiType {
    val wordmark = style(FontWeight.SemiBold, 28f, 34f, -0.03f)
    val pageTitle = style(FontWeight.Light, 30f, 34f, -0.03f)
    val headline = style(FontWeight.Normal, 22f, 28f, -0.01f)
    val title = style(FontWeight.Medium, 17f, 24f, -0.01f)
    val body = style(FontWeight.Normal, 15f, 22f)
    val label = style(FontWeight.Medium, 13f, 18f)
    val caption = style(FontWeight.Normal, 12f, 16f)
    val overline = style(FontWeight.Medium, 12f, 16f, 0.08f)

    /** Listeneinträge wie Gmail: erste Zeile 15/500, zweite 12,5 soft. */
    val rowTitle = style(FontWeight.Medium, 15f, 21f)
    val rowMeta = style(FontWeight.Normal, 12.5f, 17f)
    val tileTitle = style(FontWeight.Medium, 14.5f, 18f, -0.01f)
    val tileBig = style(FontWeight.Normal, 19f, 22f, -0.02f)
    val tileStat = style(FontWeight.Medium, 11.5f, 14f)
    val tileCaption = style(FontWeight.Normal, 11f, 14f)
    val button = style(FontWeight.Medium, 15f, 20f)
    val buttonSmall = style(FontWeight.Medium, 13.5f, 18f)
    val tag = style(FontWeight.SemiBold, 11.5f, 14f, 0.01f)
    val paper = style(FontWeight.Normal, 15f, 24f)
    val filterCount = style(FontWeight.Medium, 22f, 26f)
    val panelTitle = style(FontWeight.Normal, 20f, 26f, -0.01f)
    val statValue = style(FontWeight.Light, 26f, 30f, -0.02f)
    val heroValue = style(FontWeight.Light, 30f, 36f, -0.02f)
}

/** Material-Komponenten (Picker, Snackbar …) bekommen ebenfalls Outfit. */
val OrganiMaterialTypography: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Outfit),
        displayMedium = t.displayMedium.copy(fontFamily = Outfit),
        displaySmall = t.displaySmall.copy(fontFamily = Outfit),
        headlineLarge = t.headlineLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.Light),
        headlineMedium = t.headlineMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.Light),
        headlineSmall = t.headlineSmall.copy(fontFamily = Outfit),
        titleLarge = t.titleLarge.copy(fontFamily = Outfit),
        titleMedium = t.titleMedium.copy(fontFamily = Outfit),
        titleSmall = t.titleSmall.copy(fontFamily = Outfit),
        bodyLarge = t.bodyLarge.copy(fontFamily = Outfit),
        bodyMedium = t.bodyMedium.copy(fontFamily = Outfit),
        bodySmall = t.bodySmall.copy(fontFamily = Outfit),
        labelLarge = t.labelLarge.copy(fontFamily = Outfit),
        labelMedium = t.labelMedium.copy(fontFamily = Outfit),
        labelSmall = t.labelSmall.copy(fontFamily = Outfit),
    )
}
