package app.organi7er.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.organi7er.core.designsystem.OrganiShapes
import app.organi7er.core.designsystem.OrganiTheme

/**
 * Text auf liniertem Papier: 24-dp-Zeilen, die Schrift sitzt auf den Linien.
 * Die Linien folgen den echten Grundlinien des Textlayouts – auch bei großer Schrift.
 */
@Composable
fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 220.dp,
    readOnly: Boolean = false,
) {
    val c = OrganiTheme.colors
    val style = OrganiTheme.type.paper
    val density = LocalDensity.current
    val linePx = with(density) { style.lineHeight.toPx() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val marginX = 28.dp
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(OrganiShapes.paper)
            .background(c.paper)
            .border(1.dp, c.border, OrganiShapes.paper)
            .drawBehind {
                val top = 12.dp.toPx()
                val strokeW = 1.dp.toPx()
                val descent = 5.dp.toPx()
                val l = layout
                var lastY: Float
                if (l != null && l.lineCount > 0 && value.isNotEmpty()) {
                    for (i in 0 until l.lineCount) {
                        val y = top + l.getLineBaseline(i) + descent
                        drawLine(c.paperLine, Offset(0f, y), Offset(size.width, y), strokeW)
                    }
                    lastY = top + l.getLineBaseline(l.lineCount - 1) + descent
                } else {
                    lastY = top + linePx * 0.75f + descent - linePx
                }
                var y = lastY + linePx
                while (y < size.height) {
                    drawLine(c.paperLine, Offset(0f, y), Offset(size.width, y), strokeW)
                    y += linePx
                }
                val x = marginX.toPx() - 8.dp.toPx()
                drawLine(c.paperMargin, Offset(x, 0f), Offset(x, size.height), strokeW)
            }
            .padding(start = marginX, end = 14.dp, top = 12.dp, bottom = 12.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = style, color = c.textSoft)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            textStyle = style.copy(color = c.text),
            cursorBrush = SolidColor(c.brandBlue),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        )
    }
}
