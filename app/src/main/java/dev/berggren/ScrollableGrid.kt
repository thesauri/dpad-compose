package dev.berggren

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun <T> ScrollableGrid(
    items: List<List<T>>,
    contentForItem: @Composable BoxScope.(item: T) -> Unit
) {
    val verticalScrollState = remember { ScrollState(initial = 0) }

    val bringRowsIntoViewRequesters: List<BringIntoViewRequester> = remember {
        List(items.count()) { BringIntoViewRequester() }
    }

    val coroutineScope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState)
    ) {
        items.forEachIndexed { rowIndex, rowItems ->

            val rowScrollState = remember { ScrollState(initial = 0) }

            val bringItemsIntoViewRequesters: List<BringIntoViewRequester> = remember {
                List(rowItems.count()) { BringIntoViewRequester() }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(rowScrollState)
                    .bringIntoViewRequester(bringRowsIntoViewRequesters[rowIndex])
            ) {

                rowItems.forEachIndexed { itemIndex, rowItem ->
                    Row(
                        modifier = Modifier
                            .bringIntoViewRequester(bringItemsIntoViewRequesters[itemIndex])
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringRowsIntoViewRequesters[rowIndex].bringIntoView()
                                        bringItemsIntoViewRequesters[itemIndex].bringIntoView()

                                    }
                                }
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
