package dev.berggren

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12),
    indication: Indication? = null,
    visibilityPadding: Rect = Rect.Zero,
    isDefault: Boolean = false
) = composed {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val boxInteractionSource = remember { MutableInteractionSource() }
    val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
    val animatedBorderColor by animateColorAsState(
        targetValue =
        if (isItemFocused) focusedBorderColor
        else unfocusedBorderColor
    )
    var previousFocus: FocusInteraction.Focus? by remember {
        mutableStateOf(null)
    }
    var previousPress: PressInteraction.Press? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    var boxSize by remember {
        mutableStateOf(IntSize(0, 0))
    }
    val inputMode = LocalInputModeManager.current

    LaunchedEffect(inputMode.inputMode) {
        when (inputMode.inputMode) {
            InputMode.Keyboard -> {
                if (isDefault) {
                    focusRequester.requestFocus()
                }
            }
            InputMode.Touch -> {}
        }
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

    if (inputMode.inputMode == InputMode.Touch)
        this.clickable(
            interactionSource = boxInteractionSource,
            indication = indication ?: rememberRipple()
        ) {
            onClick()
        }
    else
        this
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged {
                boxSize = it
            }
            .indication(
                interactionSource = boxInteractionSource,
                indication = indication ?: rememberRipple()
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    val newFocusInteraction = FocusInteraction.Focus()
                    scope.launch {
                        boxInteractionSource.emit(newFocusInteraction)
                    }
                    scope.launch {
                        val visibilityBounds = Rect(
                            left = -1f * visibilityPadding.left,
                            top = -1f * visibilityPadding.top,
                            right = boxSize.width + visibilityPadding.right,
                            bottom = boxSize.height + visibilityPadding.bottom
                        )
                        bringIntoViewRequester.bringIntoView(visibilityBounds)
                    }
                    previousFocus = newFocusInteraction
                } else {
                    previousFocus?.let {
                        scope.launch {
                            boxInteractionSource.emit(FocusInteraction.Unfocus(it))
                        }
                    }
                }
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
            .focusRequester(focusRequester)
            .focusTarget()
            .border(
                width = borderWidth,
                color = animatedBorderColor
            )
}
