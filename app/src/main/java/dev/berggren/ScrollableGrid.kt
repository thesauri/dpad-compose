package dev.berggren

import android.view.KeyEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun <T> ScrollableGrid(
    items: List<List<T>>,
    contentForItem: @Composable BoxScope.(item: T) -> Unit
) {
    val verticalScrollState = remember { ScrollState(initial = 0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        items.forEach { rowItems ->

            val rowScrollState = remember { ScrollState(initial = 0) }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(rowScrollState)
            ) {

                rowItems.forEachIndexed { rowItemIndex, rowItem ->
                    Row(
                        modifier = Modifier
                            .onKeyEvent {
                                it.nativeKeyEvent
                                var bool = false
                                if (it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                        if (rowItemIndex == 0)
                                            bool = true
                                    }
                                    if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                        if (rowItemIndex == rowItems.count() - 1)
                                            bool = true
                                    }
                                }
                                bool
                            }
                    ) {
                        Box {
                            contentForItem(rowItem)
                        }
                        Spacer(Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}
