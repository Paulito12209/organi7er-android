package app.organi7er

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    @Test
    fun reiterUndEinstellungenSindErreichbar() {
        compose.onNodeWithContentDescription("Termine").performClick()
        compose.onAllNodesWithText("Termine").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Aufgaben").performClick()
        compose.onAllNodesWithText("Aufgaben").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Notizen").performClick()
        compose.onAllNodesWithText("Notizen").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Übersicht").performClick()
        compose.onNodeWithContentDescription("Einstellungen").performClick()
        compose.onAllNodesWithText("Einstellungen").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onAllNodesWithText("Erfassen".uppercase()).onFirst().assertIsDisplayed()
    }

    @Test
    fun erfassenOeffnetDasPassendeSheet() {
        compose.onAllNodesWithText("Neue Aufgabe").onFirst().performClick()
        compose.onAllNodesWithText("Was ist zu tun?").onFirst().assertIsDisplayed()
    }
}
