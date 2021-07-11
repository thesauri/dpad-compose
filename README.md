# dpad-compose
D-pad navigation in compose

## The problem
While Android is mostly used on touch devices, the operating system can also be used with arrow keys and d-pads (directional pads).
At the time of writing, the upcoming UI toolkit for writing native Android apps, [Jetpack Compose](https://developer.android.com/jetpack/compose), only has partial support for such navigation in its latest release `1.0.0-rc01`.
The library supports adding `focusable()` [modifiers](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) to elements and is able to move focus between items on key presses, but it's not able to handle clicks nor scroll lists yet.

As this has been asked for multiple times in the kotlinlang Slack channel, the purpose of this demo is to demonstrate how this functionality can be implemented in the current version of Jetpack Compose.
The tutorial in its current revision includes clicking.
Scrolling will be added a later date.

## Clicking
Clicking involves invoking an action when the center key of the d-pad or the enter key is pressed.
But before diving into the details of handling clicks, let's create a demo scene with items to navigate.

### Creating a scrollable grid

In this case, we'll create a grid with colored boxes that expand beyond the screen both vertically and horizontally.
Each row is individually scrollable horizontally while the whole grid can be scrolled vertically.
Think Netflix and movies.

[!Netflix-like grid](https://media.giphy.com/media/dve4CrRGK01RHVj2H6/giphy.gif)

First, we need main `Column` that positions its children vertically that is scrollable:

```kotlin
Column(
    Modifier
        .fillMaxSize()
        .verticalScroll(verticalScrollState)
) { /* Children here */ }
```

Then, for each row we add a `Row` that positions its children horizontally and that is also scrollable:

```kotlin
val rowScrollState = remember { ScrollState(initial = 0) }
Row(
    Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp)
        .horizontalScroll(rowScrollState)
) { /* Row content here */ }
```

Finally, we need to map out the row items.
To help us out we put our existing code inside a component that allows us to pass generic items.
These items are passed as a list of list, a list of row items, of any type `T`.
In addition to this, we pass a function that maps any item `T` to a composable, i.e. what that item should look like:

```kotlin
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                rowItems.forEach { rowItem ->
                    Row {
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
```
[ScrollableGrid.kt](app/src/main/java/dev/berggren/ScrollableGrid.kt)

Next, we'll use our newly created component to create a grid of colored boxes.
First, let's prepare list of lists containing the colors we want to display.
To make my life simple I picked a palette from [flatuicolors.com](https://flatuicolors.com/palette/defo) and copied the hexadecimal values to a list of colors:

```kotlin
val rowColors = listOf(
    Color(0xff1abc9c),
    Color(0xff2ecc71),
    Color(0xff3498db),
    Color(0xff9b59b6),
    Color(0xff34495e)
)
```

These are the primary colors for each row, but to further distinguish them horizontally I darkened them progressively:

```
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
```

Next, let's add component that represent a grid item.
In this case the item is a rectangular box with the given color as background:

```kotlin
@Composable
fun ColoredBox(
    modifier: Modifier = Modifier,
    color: Color
) {
    Box(
        modifier
            .size(128.dp)
            .background(color)
    )
}
```

Finally, let's pass the colors to the scrollable grid and the colored box component:

```kotlin
MaterialTheme {
		Column(
				Modifier
						.fillMaxSize()
						.background(Color(0xffecf0f1))
						.padding(start = 24.dp)
		) {
				ScrollableGrid(
						items = boxColors,
				) { color ->
						ColoredBox(
								color = color
						)
				}
		}
}
```

At this stage, you should have a Netflix-like grid that is row-wise scrollable and vertically scrollable as a whole.

