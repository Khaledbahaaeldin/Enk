package app.enk.audio

import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SoundPlayerTest {

    private fun testSoundSet(): SoundSet {
        return SoundSet(
            id = "test",
            name = "Test",
            keys = listOf(Sample("key1.ogg")),
            space = Sample("space.ogg"),
            enter = Sample("enter.ogg"),
            backspace = Sample("backspace.ogg"),
            bell = Sample("bell.ogg")
        )
    }

    @Test
    fun doubleReleaseIsSafe() {
        val context: Context = RuntimeEnvironment.getApplication()
        val player = SoundPoolPlayer(context)

        // First release
        player.release()

        // Second release must be idempotent and safe
        player.release()
    }

    @Test
    fun playAfterReleaseIsSafeNoOp() {
        val context: Context = RuntimeEnvironment.getApplication()
        val player = SoundPoolPlayer(context)
        val set = testSoundSet()

        player.release()

        // Calling play or playTick on a released player should safely do nothing
        player.play(SoundEvent.Key, set, 1.0f)
        player.playTick(50, 1.0f)
    }

    @Test
    fun prepareAndPlayDoesNotCrash() {
        val context: Context = RuntimeEnvironment.getApplication()
        val player = SoundPoolPlayer(context)
        val set = testSoundSet()

        player.prepare(set)
        player.play(SoundEvent.Key, set, 0.8f)
        player.play(SoundEvent.Space, set, 0.8f)
        player.play(SoundEvent.Enter, set, 0.8f)
        player.play(SoundEvent.Backspace, set, 0.8f)
        player.play(SoundEvent.Bell, set, 0.8f)
        player.play(SoundEvent.LampClick, set, 0.8f)
        player.play(SoundEvent.FaderTick, set, 0.8f)
        player.playTick(25, 0.8f)

        player.release()
    }

    @Test
    fun playTickMultiplePositionsDoesNotCrash() {
        val context: Context = RuntimeEnvironment.getApplication()
        val player = SoundPoolPlayer(context)

        val testPositions = listOf(-10, 0, 50, 100, 200)
        val testVolumes = listOf(0.0f, 0.5f, 1.0f, 1.5f)

        for (pos in testPositions) {
            for (vol in testVolumes) {
                player.playTick(position = pos, volume = vol)
            }
        }

        player.release()
    }

    @Test
    fun multipleReleasesIdempotent() {
        val context: Context = RuntimeEnvironment.getApplication()
        val player = SoundPoolPlayer(context)
        val set = testSoundSet()

        player.release()
        player.release()
        player.release()

        // Calls after multiple releases must safely no-op
        player.play(SoundEvent.Key, set, 1.0f)
        player.playTick(50, 1.0f)
    }
}
