package app.enk.design.golden

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Shared Roborazzi snapshot rule for golden tests in :design.
 * Owns the Roborazzi setup: native graphics, screen size qualifiers, output directory, and comparison threshold.
 */
class EnkSnapshots(
    private val testClass: Class<*>,
    private val screenWidthDp: Int = 400,
    private val screenHeightDp: Int = 820,
    private val threshold: Float = DEFAULT_THRESHOLD
) : TestRule {

    val composeRule = createComposeRule()
    private var currentDescription: Description? = null

    override fun apply(base: Statement, description: Description): Statement {
        currentDescription = description
        val statementWithQualifiers = object : Statement() {
            override fun evaluate() {
                RuntimeEnvironment.setQualifiers("w${screenWidthDp}dp-h${screenHeightDp}dp-hdpi")
                base.evaluate()
            }
        }
        return composeRule.apply(statementWithQualifiers, description)
    }

    fun snapshot(name: String? = null, content: @Composable () -> Unit) {
        val testName = name ?: currentDescription?.methodName ?: "snapshot"
        val fileName = "${testClass.packageName}_${testClass.simpleName}_$testName.png"
        val targetFile = File("src/test/snapshots/images/$fileName")
        targetFile.parentFile?.mkdirs()

        composeRule.setContent(content)
        composeRule.waitForIdle()

        composeRule.onRoot().captureRoboImage(
            filePath = targetFile.path,
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = threshold
                )
            )
        )
    }

    companion object {
        /** Share of pixels allowed to differ: none. Rendering is software and deterministic; a 2 dp caret that goes missing must fail. */
        const val DEFAULT_THRESHOLD = 0f
    }
}
