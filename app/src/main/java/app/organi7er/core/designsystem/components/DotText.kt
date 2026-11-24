package app.organi7er.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiTheme

/**
 * Zahlen im Punktraster (5×7 Punkte je Ziffer). Einheitliche Darstellung für alle Zahlen der App:
 * Zähler, Datum, Wochen, Uhrzeiten. Gezeichnet statt als Schrift, damit die Punkte überall
 * gleich groß und exakt ausgerichtet sind.
 */
private const val COLS = 5
private const val ROWS = 7

/** Abstand zwischen zwei Zeichen in Punktzellen. */
private const val GAP = 2f

/** Punktdurchmesser im Verhältnis zur Zelle (1 = Punkte berühren sich). */
private const val DOT = 0.92f

private val Glyphs: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    ':' to listOf("0", "0", "1", "0", "1", "0", "0"),
    '.' to listOf("0", "0", "0", "0", "0", "0", "1"),
    '-' to listOf("000", "000", "000", "111", "000", "000", "000"),
    '–' to listOf("000", "000", "000", "111", "000", "000", "000"),
    '+' to listOf("000", "000", "010", "111", "010", "000", "000"),
    '/' to listOf("00001", "00001", "00010", "00100", "01000", "10000", "10000"),
)

/** True, wenn [text] nur aus Zeichen besteht, die das Punktraster kennt (Ziffern, Zeichen, Leerraum). */
fun isDotable(text: String): Boolean = text.isNotEmpty() && text.all { it == ' ' || Glyphs.containsKey(it) }

/**
 * @param height Höhe einer Ziffer.
 * @param glow optionaler weicher Schein hinter den Punkten (z. B. Weiß auf hellem Silber).
 */
@Composable
fun DotText(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    color: Color = OrganiTheme.colors.text,
    glow: Color? = null,
) {
    val cell = with(LocalDensity.current) { height.toPx() } / ROWS
    val layout = remember(text) { layoutOf(text) }
    val width = with(LocalDensity.current) { (layout.cells * cell).toDp() }
    Canvas(modifier.size(width, height).semantics { contentDescription = text }) {
        val r = cell * DOT / 2f
        if (glow != null) layout.dots.forEach { (cx, cy) -> drawCircle(glow, r * 2.1f, Offset((cx + 0.5f) * cell, (cy + 0.5f) * cell)) }
        layout.dots.forEach { (cx, cy) -> drawCircle(color, r, Offset((cx + 0.5f) * cell, (cy + 0.5f) * cell)) }
    }
}

private class DotLayout(val dots: List<Pair<Float, Float>>, val cells: Float)

private fun layoutOf(text: String): DotLayout {
    val dots = ArrayList<Pair<Float, Float>>()
    var x = 0f
    text.forEachIndexed { i, ch ->
        if (ch == ' ') {
            x += COLS
        } else {
            val g = Glyphs[ch] ?: return@forEachIndexed
            g.forEachIndexed { row, bits -> bits.forEachIndexed { col, b -> if (b == '1') dots += (x + col) to row.toFloat() } }
            x += g[0].length + (if (i < text.lastIndex) GAP else 0f)
        }
    }
    return DotLayout(dots, x.coerceAtLeast(1f))
}
