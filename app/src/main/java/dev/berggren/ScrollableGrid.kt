package dev.berggren

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> ScrollableGrid(
    items: List<List<T>>,
    spacing: Dp,
    contentForItem: @Composable BoxScope.(item: T) -> Unit
) {
    val verticalScrollState = remember { ScrollState(initial = 0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
            .padding(end = spacing, bottom = spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        items.forEach { rowItems ->
            val rowScrollState = remember { ScrollState(initial = 0) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rowScrollState),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowItems.forEach { rowItem ->
                    Box {
                        contentForItem(rowItem)
                    }
                }
            }
        }
    }
}