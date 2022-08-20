package dev.berggren

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> ScrollableGrid(
    items: List<List<T>>,
    contentForItem: @Composable BoxScope.(item: T, position: GridPosition) -> Unit
) {
    val verticalScrollState = remember { ScrollState(initial = 0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        items.forEachIndexed { rowIndex, rowItems ->
            val rowScrollState = remember { ScrollState(initial = 0) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(rowScrollState)
            ) {
                rowItems.forEachIndexed { columnIndex, rowItem ->
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        contentForItem(
                            rowItem,
                            GridPosition(
                                rowIndex = rowIndex,
                                columnIndex = columnIndex
                            )
                        )
                    }
                }
            }
        }
    }
}

@Stable
data class GridPosition(val rowIndex: Int, val columnIndex: Int)
