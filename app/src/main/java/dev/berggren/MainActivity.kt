package dev.berggren

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
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
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xffecf0f1))
            ) {
                ScrollableGrid(items = boxColors) { color ->
                    CardContent(color = color)
                }
            }
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
