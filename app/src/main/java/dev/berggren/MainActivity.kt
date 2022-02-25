package dev.berggren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@ExperimentalComposeUiApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rowColors = listOf(
            Color(0xff1abc9c),
            Color(0xff2ecc71),
            Color(0xff3498db),
            Color(0xff9b59b6),
            Color(0xff34495e)
        )
        val itemsPerRow = 10
        val boxColors = rowColors.map { rowColor ->
            (0..itemsPerRow).map { rowIndex ->
                val fraction = (1 - rowIndex.toFloat() / itemsPerRow)
                Color(
                    red = fraction * rowColor.red,
                    green = fraction * rowColor.green,
                    blue = fraction * rowColor.blue
                )
            }
        }
        setContent {
            var colorClicked: Color by remember { mutableStateOf(Color.Transparent) }

            MaterialTheme {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xffecf0f1))
                        .padding(top = spacing)
                ) {
                    Box(Modifier.padding(start = spacing)) {
                        ColorClickedBanner(color = colorClicked)
                    }
                    Spacer(Modifier.height(spacing))
                    ScrollableGrid(
                        items = boxColors,
                        spacing = spacing
                    ) { color ->
                        ColoredBox(
                            Modifier.dpadFocusable(
                                onClick = {
                                    colorClicked = color
                                }
                            ),
                            color = color
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorClickedBanner(color: Color) {
    Row {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Text(text = "Clicked color: ", style = MaterialTheme.typography.h3)
            Spacer(Modifier.width(spacing))
            Box(
                Modifier
                    .background(color, CircleShape)
                    .aspectRatio(1f)
                    .fillMaxSize()
            )
        }
    }
}

@Composable
fun ColoredBox(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier
            .size(itemSize)
            .background(color)
    )
}

private val itemSize = 128.dp
private val spacing = itemSize / 4
