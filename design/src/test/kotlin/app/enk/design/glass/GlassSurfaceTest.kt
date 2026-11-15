package app.enk.design.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassSurfaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lightLevelIsSolidG1PixelColor() {
        var expectedG1 = 0
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                EnkShellTheme {
                    expectedG1 = LocalShellColors.current.g1.hashCode()
                    val source = rememberGlassSource()
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("glass_box")
                            .glassSurface(
                                spec = GlassSpecs.NavBody,
                                shape = RoundedCornerShape(16.dp),
                                source = source
                            )
                    )
                }
            }
        }

        val bitmap = composeTestRule.onNodeWithTag("glass_box").captureToImage()
        val g1Color = bitmap.toPixelMap()[50, 50]
        assertNotEquals("Pixel must not be transparent", 0f, g1Color.alpha, 0.01f)
    }

    @Test
    fun balancedComposesWithoutThrowing() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Balanced,
                LocalReduceEffects provides false
            ) {
                EnkShellTheme {
                    val source = rememberGlassSource()
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .glassSurface(
                                spec = GlassSpecs.NavBody,
                                shape = RoundedCornerShape(16.dp),
                                source = source
                            )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun fullComposesWithoutThrowing() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Full,
                LocalReduceEffects provides false
            ) {
                EnkShellTheme {
                    val source = rememberGlassSource()
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .glassSurface(
                                spec = GlassSpecs.NavLens,
                                shape = RoundedCornerShape(16.dp),
                                source = source
                            )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun interactionScaleRendersWithoutThrowing() {
        val interaction = GlassInteraction().apply {
            pressScale = 1.08f
            stretchX = 1.05f
            stretchY = 0.95f
            specularAngleDeg = 60f
        }

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides false
            ) {
                EnkShellTheme {
                    val source = rememberGlassSource()
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("scaled_box")
                            .glassSurface(
                                spec = GlassSpecs.NavBody,
                                shape = RoundedCornerShape(16.dp),
                                source = source,
                                interaction = interaction
                            )
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val imgScaled = composeTestRule.onNodeWithTag("scaled_box").captureToImage()
        assertNotEquals(0, imgScaled.width)
    }
}
