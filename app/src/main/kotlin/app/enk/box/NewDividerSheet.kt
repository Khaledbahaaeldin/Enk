package app.enk.box

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlin.math.roundToInt

@Composable
fun NewDividerSheet(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    existingNames: List<String> = emptyList(),
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current

    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    // The reason for opening the sheet is to type a name, so the keyboard comes up with it.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var dragY by remember { mutableFloatStateOf(0f) }

    val animatedDragY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = ShellMotion.sheet.toSpec(),
        label = "sheet_drag"
    )

    val validationResult = remember(text, existingNames) {
        val trimmed = text.trim()
        when {
            trimmed.isEmpty() -> DividerValidationResult.Blank
            trimmed.length > 40 -> DividerValidationResult.TooLong
            existingNames.any { it.equals(trimmed, ignoreCase = true) } -> DividerValidationResult.Duplicate
            else -> DividerValidationResult.Valid(trimmed)
        }
    }
    val isValid = validationResult is DividerValidationResult.Valid

    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.thick.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Sheet content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, animatedDragY.coerceAtLeast(0f).roundToInt()) }
                .testTag("new_divider_sheet")
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragY = (dragY + delta).coerceAtLeast(0f)
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        val dismissThresholdPx = with(density) { 160.dp.toPx() }
                        val projected = dragY + (velocity / 1000f) * 300f
                        if (projected > dismissThresholdPx || dragY > dismissThresholdPx || velocity > 300f) {
                            onDismiss()
                        } else {
                            dragY = 0f
                        }
                    }
                )
                .glass(GlassLevel.Thick, sheetShape, glassSource)
                .clip(sheetShape)
                // Above the keyboard when it is up, above the navigation bar when it is not.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grabber
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(material.grab)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = "New divider",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.t1,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create a divider to organize your notes.",
                    fontSize = 14.sp,
                    color = colors.t2,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Text field
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester).testTag("divider_name_field"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (validationResult is DividerValidationResult.Valid) {
                            onAdd(validationResult.name)
                            onDismiss()
                        }
                    }),
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        color = colors.t1
                    ),
                    cursorBrush = SolidColor(colors.ui),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(material.field2)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Divider name",
                                    fontSize = 17.sp,
                                    color = colors.t3
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (validationResult is DividerValidationResult.TooLong) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Divider name cannot exceed 40 characters.",
                        fontSize = 12.sp,
                        color = colors.danger,
                        modifier = Modifier.align(Alignment.Start)
                    )
                } else if (validationResult is DividerValidationResult.Duplicate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A divider with this name already exists.",
                        fontSize = 12.sp,
                        color = colors.danger,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_divider_cancel")
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.g1.copy(alpha = 0.6f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.t1
                        )
                    }

                    // Add button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_divider_add")
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isValid) colors.ui else colors.ui.copy(alpha = 0.4f))
                            .clickable(
                                enabled = isValid,
                                onClick = {
                                    if (validationResult is DividerValidationResult.Valid) {
                                        onAdd(validationResult.name)
                                        onDismiss()
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isValid) colors.uiInk else colors.uiInk.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
