package kz.cashbackcases.demo

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import kz.cashbackcases.demo.ui.Tags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun demoFlowKeepsWithdrawalDisabled() {
        rule.onNodeWithTag(Tags.DEMO_BANNER).assertIsDisplayed()
        rule.onNodeWithTag(Tags.caseTab("city")).assertIsDisplayed()
        rule.onNodeWithTag(Tags.caseTab("home")).assertIsDisplayed()
        rule.onNodeWithTag(Tags.caseTab("service")).assertIsDisplayed()

        rule.onNodeWithTag(Tags.NAV_BALANCE).click()
        rule.onNodeWithTag(Tags.RESET).performScrollTo().click()
        rule.onNodeWithTag(Tags.CONFIRM_RESET).click()
        waitForAmount(Tags.BALANCE_PENDING, "840 ₸")
        waitForAmount(Tags.BALANCE_CONFIRMED, "225 ₸")
        rule.onNodeWithTag(Tags.WITHDRAWAL).performScrollTo().assertIsNotEnabled()

        rule.onNodeWithTag(Tags.NAV_CASES).click()
        rule.onNodeWithTag(Tags.caseTab("service")).click()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(Tags.OPEN_COLLECTION).fetchSemanticsNodes().any { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.contains("Сервис") } == true
            }
        }
        rule.onNodeWithTag(Tags.OPEN_COLLECTION).click()
        rule.waitUntil(timeoutMillis = 5_000) {
            try {
                rule.onNodeWithTag(Tags.offer("tulpar-travel")).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        rule.onNodeWithTag(Tags.offer("tulpar-travel")).click()
        rule.onNodeWithTag(Tags.purchase(27_500)).performScrollTo().click()
        rule.onNodeWithTag(Tags.ACTIVATE).performScrollTo().click()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(Tags.CONFIRM_ACTIVATE).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(Tags.CONFIRM_ACTIVATE).click()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(Tags.ACTIVATED).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithTag(Tags.NAV_BALANCE).click()
        waitForAmount(Tags.BALANCE_PENDING, "3 590 ₸")
        rule.onNodeWithTag(Tags.WITHDRAWAL).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag(Tags.confirmOffer("tulpar-travel")).performScrollTo().click()
        rule.onNodeWithTag(Tags.CONFIRM_STATUS).click()
        waitForAmount(Tags.BALANCE_PENDING, "840 ₸")
        waitForAmount(Tags.BALANCE_CONFIRMED, "2 975 ₸")
        rule.onNodeWithTag(Tags.WITHDRAWAL).performScrollTo().assertIsNotEnabled()

        rule.onNodeWithTag(Tags.RESET).performScrollTo().click()
        rule.onNodeWithTag(Tags.CONFIRM_RESET).click()
        waitForAmount(Tags.BALANCE_PENDING, "840 ₸")
        waitForAmount(Tags.BALANCE_CONFIRMED, "225 ₸")
        rule.onNodeWithTag(Tags.BALANCE_PENDING).assertTextEquals("840 ₸")
        rule.onNodeWithTag(Tags.BALANCE_CONFIRMED).assertTextEquals("225 ₸")
    }

    private fun SemanticsNodeInteraction.click(): SemanticsNodeInteraction {
        return performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun waitForAmount(tag: String, amount: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().any { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == amount } == true
            }
        }
    }
}
