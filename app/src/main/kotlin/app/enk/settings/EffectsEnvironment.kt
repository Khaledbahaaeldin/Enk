package app.enk.settings

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.enk.design.effects.EffectsEnvironment
import app.enk.design.effects.ResolvedEffects
import app.enk.design.isHighContrastTextEnabled
import app.enk.design.isReducedMotion

fun readEffectsEnvironment(
    context: Context,
    slowFrames: ResolvedEffects? = null
): EffectsEnvironment {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val lowRam = am?.isLowRamDevice == true
    val batterySaver = pm?.isPowerSaveMode == true
    val reducedMotion = isReducedMotion(context)
    val highContrast = isHighContrastTextEnabled(context)
    return EffectsEnvironment(
        sdkInt = Build.VERSION.SDK_INT,
        systemReducedMotion = reducedMotion,
        highContrastText = highContrast,
        lowRamDevice = lowRam,
        batterySaver = batterySaver,
        slowFrames = slowFrames
    )
}

@Composable
fun rememberEffectsEnvironment(slowFrames: ResolvedEffects? = null): EffectsEnvironment {
    val context = LocalContext.current
    var env by remember(context, slowFrames) {
        mutableStateOf(readEffectsEnvironment(context, slowFrames))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, lifecycleOwner, slowFrames) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                env = readEffectsEnvironment(context, slowFrames)
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        context.registerReceiver(receiver, filter)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                env = readEffectsEnvironment(context, slowFrames)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return env
}
