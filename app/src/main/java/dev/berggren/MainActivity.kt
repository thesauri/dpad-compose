package dev.berggren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.berggren.ui.theme.DpadComposeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val colors = listOf(
            Color(0xff1abc9c),
            Color(0xff2ecc71),
            Color(0xff3498db),
            Color(0xff9b59b6),
            Color(0xff34495e)
        )
        val itemsPerRow = 10
        val boxSize = 128.dp
        setContent {
            val verticalScrollState = remember { ScrollState(initial = 0) }
            val boxSizePx = with(LocalDensity.current) { boxSize.value }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .background(Color(0xffecf0f1))
            ) {
                Spacer(Modifier.height(24.dp))
                colors.forEach { color ->
                    val rowScrollState = remember { ScrollState(initial = 0) }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .horizontalScroll(rowScrollState)
                    ) {
                        Spacer(Modifier.width(24.dp))
                        (0..itemsPerRow).forEach { rowIndex ->
                            val boxInteractionSource = remember { MutableInteractionSource() }
                            val isBoxFocused by boxInteractionSource.collectIsFocusedAsState()
                            val borderColor = Color(0xfff39c12)
                            val animatedBorderColor by animateColorAsState(
                                targetValue =
                                    if (isBoxFocused) borderColor
                                    else borderColor.copy(alpha = 0.0f)
                            )
                            val elevation by animateDpAsState(
                                targetValue = if (isBoxFocused) 4.dp else 0.dp
                            )
                            var previousPress: PressInteraction.Press? = null
                            Row {
                                Surface(
                                    Modifier
                                        .clickable(
                                            interactionSource = boxInteractionSource,
                                            indication = rememberRipple()
                                        ) {}
                                        .onKeyEvent {
                                           when (it.type) {
                                                KeyEventType.KeyDown -> {
                                                    if (!listOf(Key.DirectionCenter, Key.Enter).contains(it.key)) {
                                                        return@onKeyEvent false
                                                    }
                                                    val press =
                                                        PressInteraction.Press(
                                                            pressPosition = Offset(
                                                                x = boxSizePx / 2f,
                                                                y = boxSizePx / 2f
                                                            )
                                                        )
                                                    lifecycleScope.launch {
                                                        boxInteractionSource.emit(press)
                                                    }
                                                    previousPress = press
                                                    true
                                                }
                                               KeyEventType.KeyUp -> {
                                                   previousPress?.let {
                                                       PressInteraction.Release(
                                                           press = it
                                                       )
                                                   }
                                                   true
                                               }
                                               else -> {
                                                   false
                                               }
                                           }
                                        }
                                        .focusable(interactionSource = boxInteractionSource)
                                        .size(boxSize)
                                        .background(color.run {
                                            val fraction = (1 - rowIndex.toFloat() / itemsPerRow)
                                            Color(
                                                red = fraction * red,
                                                green = fraction * green,
                                                blue = fraction * blue
                                            )
                                        })
                                        .border(
                                            width = 4.dp,
                                            color = animatedBorderColor
                                        ),
                                    color = color.run {
                                        val fraction = (1 - rowIndex.toFloat() / itemsPerRow)
                                        Color(
                                            red = fraction * red,
                                            green = fraction * green,
                                            blue = fraction * blue
                                        )
                                    },
                                    elevation = elevation
                                ) {}
                                Spacer(Modifier.width(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DpadComposeTheme {
        Greeting("Android")
    }
}