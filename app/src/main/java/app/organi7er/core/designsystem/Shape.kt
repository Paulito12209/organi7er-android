package app.organi7er.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Radien aus der Vorlage: 10 · 14 · 20 · 22 · 28 · 34 · Pille. */
object OrganiShapes {
    val paper = RoundedCornerShape(10.dp)
    val small = RoundedCornerShape(14.dp)
    val medium = RoundedCornerShape(20.dp)
    val tile = RoundedCornerShape(22.dp)
    val card = RoundedCornerShape(28.dp)
    val large = RoundedCornerShape(34.dp)
    val pill = RoundedCornerShape(percent = 50)
    val circle = CircleShape
    val fab = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
