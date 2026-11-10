package app.enk.box

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.data.NoteSummary
import app.enk.design.LocalMonoFont
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow
import app.enk.design.motion.ShellMotion
import app.enk.design.paperGrain
import app.enk.design.theme.LocalShellMaterial
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val NoteCardLiftedKey = SemanticsPropertyKey<Boolean>("NoteCardLifted")
var SemanticsPropertyReceiver.noteCardLifted by NoteCardLiftedKey

val PaperColor = Color(0xFFF2ECDF)
val CardTextColor = Color(0xFF14110D)
val CardSubtextColor = Color(0xFF5C574F)

@Composable
fun NoteCard(
    summary: NoteSummary,
    onClick: () -> Unit,
    onHold: () -> Unit,
    lifted: Boolean,
    modifier: Modifier = Modifier,
    onMoveToDivider: (() -> Unit)? = null,
    onMoveToTrash: (() -> Unit)? = null
) {
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val mono = LocalMonoFont.current

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnHold by rememberUpdatedState(onHold)

    var isPressed by remember { mutableStateOf(false) }

    val targetScale = when {
        lifted -> 1.03f
        isPressed -> 0.985f
        else -> 1.0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 120),
        label = "card_scale"
    )

    val shape = RoundedCornerShape(14.dp)

    val shadowLayers = remember(lifted, material.cardShadow) {
        if (lifted) {
            listOf(
                ShadowLayer(0.dp, 0.5.dp, 0.dp, 0.dp, Color.White.copy(alpha = 0.60f), inset = true),
                ShadowLayer(0.dp, 24.dp, 50.dp, (-10).dp, Color.Black.copy(alpha = 0.60f))
            )
        } else {
            listOf(
                ShadowLayer(0.dp, 0.5.dp, 0.dp, 0.dp, Color.White.copy(alpha = 0.60f), inset = true),
                ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, Color.Black.copy(alpha = 0.25f)),
                ShadowLayer(0.dp, 8.dp, 18.dp, (-8).dp, material.cardShadow)
            )
        }
    }

    val lines = remember(summary.firstLines) { summary.firstLines.split('\n') }
    val title = lines.firstOrNull()?.ifBlank { "Untitled" } ?: "Untitled"
    val body = lines.drop(1).joinToString("\n").trim()

    val dateFormatted = remember(summary.modifiedAt) {
        val sdf = SimpleDateFormat("d MMM", Locale.US)
        sdf.format(Date(summary.modifiedAt))
    }

    Box(
        modifier = modifier
            .testTag("note_card_${summary.id}")
            .semantics {
                set(NoteCardLiftedKey, lifted)
                customActions = buildList {
                    if (onMoveToDivider != null) {
                        add(CustomAccessibilityAction("Move to divider") {
                            onMoveToDivider()
                            true
                        })
                    }
                    if (onMoveToTrash != null) {
                        add(CustomAccessibilityAction("Move to trash") {
                            onMoveToTrash()
                            true
                        })
                    }
                }
            }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .enkShadow(shadowLayers, shape)
            .clip(shape)
            .background(PaperColor)
            .paperGrain(0.3f)
            .pointerInput(summary.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position
                    val downTime = down.uptimeMillis
                    isPressed = true
                    var slopExceeded = false
                    var heldTriggered = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        val dist = (change.position - downPos).getDistance()
                        if (dist > with(density) { ShellMotion.DRAG_SLOP_DP.dp.toPx() }) {
                            slopExceeded = true
                        }

                        val elapsed = change.uptimeMillis - downTime
                        if (!slopExceeded && !heldTriggered && elapsed >= ShellMotion.LONG_PRESS_MS) {
                            heldTriggered = true
                            isPressed = false
                            currentOnHold()
                        }

                        if (!change.pressed) {
                            isPressed = false
                            if (!heldTriggered && !slopExceeded && !change.isConsumed) {
                                currentOnClick()
                            }
                            break
                        }
                    }
                }
            }
            .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontFamily = mono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CardTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (body.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    fontFamily = mono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = CardTextColor.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormatted,
                    fontFamily = mono,
                    fontSize = 11.sp,
                    color = CardSubtextColor
                )

                if (summary.pageCount > 1) {
                    Text(
                        text = "p.${summary.pageCount}",
                        fontFamily = mono,
                        fontSize = 11.sp,
                        color = CardSubtextColor
                    )
                }
            }
        }
    }
}
