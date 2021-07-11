package dev.berggren

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
@Composable
fun <T> ScrollableGrid(
    items: List<List<T>>,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12),
    contentForItem: @Composable BoxScope.(T) -> Unit
) {
    val verticalScrollState = remember { ScrollState(initial = 0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        Spacer(Modifier.height(24.dp))
        items.forEach { rowItems ->
            val rowScrollState = remember { ScrollState(initial = 0) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(rowScrollState)
            ) {
                Spacer(Modifier.width(24.dp))
                rowItems.forEach { rowItem ->
                    val boxInteractionSource = remember { MutableInteractionSource() }
                    val isBoxFocused by boxInteractionSource.collectIsFocusedAsState()
                    val animatedBorderColor by animateColorAsState(
                        targetValue =
                            if (isBoxFocused) focusedBorderColor
                            else unfocusedBorderColor
                    )
                    var previousPress: PressInteraction.Press? by remember {
                        mutableStateOf(null)
                    }
                    val scope = rememberCoroutineScope()
                    var boxSize by remember {
                        mutableStateOf(IntSize(0, 0))
                    }

                    Row {
                        Box(
                            Modifier
                                .onGloballyPositioned {
                                    boxSize = it.size
                                }
                                .clickable(
                                    interactionSource = boxInteractionSource,
                                    indication = rememberRipple()
                                ) {
                                    println("DDD: clicked!")
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
                                    width = 4.dp,
                                    color = animatedBorderColor
                                ),
                        ) {
                            contentForItem(rowItem)
                        }
                        Spacer(Modifier.width(24.dp))
                    }
                    LaunchedEffect(isBoxFocused) {
                        previousPress?.let {
                            if (!isBoxFocused) {
                                boxInteractionSource.emit(
                                    PressInteraction.Release(
                                        press = it
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
