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

class MainActivity : ComponentActivity() {
    @ExperimentalComposeUiApi
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
                        .padding(24.dp)
                ) {
                    ColorClickedBanner(color = colorClicked)
                    Spacer(Modifier.height(24.dp))
                    ScrollableGrid(
                        items = boxColors,
                        onClick = {
                            colorClicked = it
                        }
                    ) { color ->
                        CardContent(color = color)
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
            Spacer(Modifier.width(24.dp))
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
fun CardContent(color: Color) {
    Box(
        Modifier
            .size(128.dp)
            .background(color)
    )
}
