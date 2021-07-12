package dev.berggren

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12)
) = composed {
    val boxInteractionSource = remember { MutableInteractionSource() }
    val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
    val animatedBorderColor by animateColorAsState(
        targetValue =
        if (isItemFocused) focusedBorderColor
        else unfocusedBorderColor
    )
    var previousPress: PressInteraction.Press? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    var boxSize by remember {
        mutableStateOf(IntSize(0, 0))
    }

    LaunchedEffect(isItemFocused) {
        previousPress?.let {
            if (!isItemFocused) {
                boxInteractionSource.emit(
                    PressInteraction.Release(
                        press = it
                    )
                )
            }
        }
    }

    this
        .onGloballyPositioned {
            boxSize = it.size
        }
        .clickable(
            interactionSource = boxInteractionSource,
            indication = rememberRipple()
        ) {
            onClick()
        }
        .onKeyEvent {
            if (!listOf(Key.DirectionCenter, Key.Enter).contains(it.key)) {
                return@onKeyEvent false
            }
            when (it.type) {
                KeyEventType.KeyDown -> {
                    val press =
                        PressInteraction.Press(
                            pressPosition = Offset(
                                x = boxSize.width / 2f,
                                y = boxSize.height / 2f
                            )
                        )
                    scope.launch {
                        boxInteractionSource.emit(press)
                    }
                    previousPress = press
                    true
                }
                KeyEventType.KeyUp -> {
                    previousPress?.let { previousPress ->
                        onClick()
                        scope.launch {
                            boxInteractionSource.emit(
                                PressInteraction.Release(
                                    press = previousPress
                                )
                            )
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
        .focusable(interactionSource = boxInteractionSource)
        .border(
            width = borderWidth,
            color = animatedBorderColor
        )
}
