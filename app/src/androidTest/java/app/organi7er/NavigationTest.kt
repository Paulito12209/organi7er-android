package app.organi7er

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before fun init() = hilt.inject()

    /** Edge-Picker: eingeklappt antippen (klappt auf), dann ein Icon wählen. Nach 2 s klappt er von allein wieder zu. */
    private fun pick(label: String) {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Navigation").performClick()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithContentDescription(label).performClick()
        // Einrasten der Feder abwarten.
        compose.mainClock.advanceTimeBy(1000)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun reiterSindUeberDenEdgePickerErreichbar() {
        pick("Termine")
        compose.onAllNodesWithText("Termine").onFirst().assertIsDisplayed()
        pick("Aufgaben")
        compose.onAllNodesWithText("Aufgaben").onFirst().assertIsDisplayed()
        pick("Notizen")
        compose.onAllNodesWithText("Notizen").onFirst().assertIsDisplayed()
    }

    @Test
    fun dockOeffnetDenComposerMitTypWahl() {
        compose.onAllNodesWithText("Neue Notiz").onFirst().performClick()
        // Termin · Aufgabe · Notiz stehen im Composer zur Wahl.
        compose.onNodeWithContentDescription("Aufgabe").performClick()
        compose.onAllNodesWithText("Neue Aufgabe").onFirst().assertIsDisplayed()
    }
}
