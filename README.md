# dpad-compose
D-pad navigation in Jetpack Compose

![Navigating and clicking on grid items](https://media.giphy.com/media/QxXT07irVKDZwuq18S/giphy.gif)

## The problem
While Android is mostly used on touch devices, the operating system can also be used with arrow keys and d-pads (directional pads).
At the time of writing, the upcoming UI toolkit for writing native Android apps, [Jetpack Compose](https://developer.android.com/jetpack/compose), only has partial support for such navigation in its latest release `1.0.0-rc01`.
The library supports adding `focusable()` [modifiers](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) to elements and is able to move focus between items based on directional key presses, but it's unable to handle clicking nor scrolling lists yet.

As this has been asked for multiple times in the kotlinlang Slack channel, the purpose of this tutorial is to demonstrate how this functionality can be implemented in the current version of Jetpack Compose.
The tutorial in its current revision includes clicking.
Scrolling will, hopefully, be added a later date.

Feel free to suggest improvements by creating an issue or a pull request.

## TL;DR
- [ScrollableGrid.kt](app/src/main/java/dev/berggren/ScrollableGrid.kt)
- [DpadFocusable.kt](app/src/main/java/dev/berggren/DpadFocusable.kt)

## Clicking
Clicking involves invoking an action when the center key of the d-pad or the enter key is pressed.
But before diving into the details of handling clicks, let's create a demo scene with items to navigate and click.

### Creating a scrollable grid

In this case, we'll create a grid with colored boxes that expand beyond the screen both vertically and horizontally.
Each row is individually scrollable horizontally while the whole grid can be scrolled vertically.
Think Netflix and TV series.

![Netflix-like grid](https://media.giphy.com/media/dve4CrRGK01RHVj2H6/giphy.gif)

First, we need main `Column` that positions its children vertically that is scrollable:

```kotlin
Column(
    Modifier
        .fillMaxSize()
        .verticalScroll(verticalScrollState)
) { /* Children here */ }
```

Secondly, for each row we add a `Row` that positions its children horizontally and that's also scrollable:

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
To help us out we put our existing code inside a component that allows us to pass generic items `T`.
These items are passed as a list of list, a list of row items, of any type `T`.
In addition to this, we pass a function that maps any item `T` to a composable, i.e. what that item should look like.
Using this component we can create scrollable grids of this kind with any type of items:

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
To make this look decent I picked a palette from [flatuicolors.com](https://flatuicolors.com/palette/defo) and copied some of the colors to a list:

```kotlin
val rowColors = listOf(
    Color(0xff1abc9c),
    Color(0xff2ecc71),
    Color(0xff3498db),
    Color(0xff9b59b6),
    Color(0xff34495e)
)
```

These are the primary colors for each row, but to further distinguish them horizontally let's darkened them progressively:

```kotlin
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
      .padding(start = 24.dp, top = 24.dp)
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

### Adding d-pad navigation
The next step is to make the grid items focusable using the d-pad and to invoke click actions when clicking the center key or enter.
Similar to how adding out-of-the-box focus is done using the `focusable()` modifier, we'll create a custom modifier `dpadFocusable()` that we can attach to grid items.
This modifier has the responsibility of showing a border if focused and appropriately responding to key events.

![Dpad navigation](https://media.giphy.com/media/hzCzKy4ccr5zeg06P2/giphy.gif)

First, let's add a dummy modifier and add some arguments for configuring its behavior:

```kotlin
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12)
) = composed { /* Content here */ }
```

Next, we need a way to visualize what item is currently focused.
In this case, we use a border that smoothly transitions between the focused color and the unfocused color (the default argument is in this case the focused color with alpha 0):

```kotlin
... = compose {
  val boxInteractionSource = remember { MutableInteractionSource() }
  val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
  val animatedBorderColor by animateColorAsState(
      targetValue =
      if (isItemFocused) focusedBorderColor
      else unfocusedBorderColor
  )

  this.
    border(
      width = borderWidth,
      color = animatedBorderColor
    )
}
```

In the last step, we used an interaction source (`boxInteractionSource`) to listen to whether the item is focused or not.
To receive these events, we need to make the item focusable and attach the interaction source:

```kotlin
... = compose {
  /* [...] */
  this.
    /* [...] */
    .focusable(interactionSource = boxInteractionSource)
```

Now the items are navigatable using a d-pad and the currently focused item is visualized with a border.
Next, we'll add clicking.

D-pad navigation is sometimes used together with touch navigation.
For this reason, we need to make the items touchable too.
We'll do this by adding the `.clickable()` modifier and attaching the interaction source and click handler
This also has the added effect of adding indications (by default ripples) whenever it is interacted with, a feature we'll use for d-pad clicks too:

```kotlin
... = compose {
  /* [...] */
  this.
    /* [...] */
    .clickable(
      interactionSource = boxInteractionSource,
      indication = rememberRipple()
    ) {
      onClick()
    }
}
```

With these elements in place, let's listen for key events.
Listening to key events has two main goals: invoke the on-click handler whenever the center key is pressed and visualizing press and release events.
For good user experience, we need to make it possible to cancel click events too.
This is done by pressing and holding the center key (or enter) and then navigating to another item before releasing the center key.
As for visualizing presses and releases, we'll use the default ripples.

We add a `.keyEvent()` modifier with a block that is run for key events, but we ignore any other key than the center or enter key by returning early:

```kotlin
/* [...] */
this.
/* [...] */
  .onKeyEvent {
    if (!listOf(Key.DirectionCenter, Key.Enter).contains(it.key)) {
      return@onKeyEvent false
    }
  /* [...] */
  }
```

Then we check whether the event was a key down or key up event.
For key down events, we don't invoke the click handler yet, but we want to indicate to the user that the click has been registered.
This is done by emiting a [PressInteraction.Press](https://developer.android.com/reference/kotlin/androidx/compose/foundation/interaction/PressInteraction) event to the interaction source:

```kotlin
... = compose {
  val scope = rememberCoroutineScope()
  this.
  /* [...] */
    .onKeyEvent {
    /* [...] */
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
        KeyEventType.KeyUp -> { /* [...] */ }
        else -> false
      }
    }
}
```

In the snippet above, there are two variables that we haven't declared yet: `boxSize` and `previousPress`.

Ripple indications grow from the point where the user pressed the item, for this reason we need to specify a position for the interaction even though a d-pad click has no inherent position.
One option which I found to look good is to have the ripple grow from the center of the item.
To achieve this we need to know the width and height of the element in question.
This can be done by adding a [onGloballyPositioned](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/OnGloballyPositionedModifier) modifier that is called whenever the element's global position has changed.
We keep track of the size and update it whenever the modifier's block is called:

```kotlin
... = compose {
  /* [...] */
  var boxSize by remember {
    mutableStateOf(IntSize(0, 0))
  }
  this.
    /* [...] */
    .onGloballyPositioned {
      boxSize = it.size
    }
}
```

After the ripple indication has finished the background of the pressed item remains slightly dimmed to indicate that it is still being pressed.
To release the dimming we need to emit a `PressInteraction.Release` event with the press event as an argument (this has to be specified for supporting pressing the same item multiple times at different positions), which was the reason why we saved the press event in a variable in the previous snippet.
Now let's actually declare the variable and use it to release the press on key ups.
We also invoke the click handler here:

```kotlin
... = compose {
  /* [...] */
  var previousPress: PressInteraction.Press? by remember {
    mutableStateOf(null)
  }
  this.
    /* [...] */
    .onKeyEvent {
      /* [...] */
      when (it.type) {
        /* [...] */
        KeyEventType.KeyUp -> {
          previousPress?.let { previousPress ->
            onClick()
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
      }
    }
}
```

As the `.onKeyEvent` is only called if the item is focused, clicks can be canceled by navigating to another item and hence unfocusing the item before releasing the center key.
This also means, however, that the item will remain in a pressed state.
To ensure the presses are released whenever the item is unfocused, we add [LaunchedEffect](https://developer.android.com/jetpack/compose/side-effects#launchedeffect) that is run every time the `isItemFocused` state changes.
We use this to release any potentially present press whenever the item is unfocused:

```kotlin
this.
  /* [...] */
  LaunchedEffect(isItemFocused) {
    previousPress?.let {
      if (!isItemFocused) {
        boxInteractionSource.emit(
          PressInteraction.Release(
            press = it
          )
        )
      }
    }
  }
```

With all these parts, we end up with a modifier that looks like this:

![Press interactions on a box](https://media.giphy.com/media/SurFCuZaFLoagcuIbr/giphy.gif)

```kotlin
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
  onClick: () -> Unit,
  borderWidth: Dp = 4.dp,
  unfocusedBorderColor: Color = Color(0x00f39c12),
  focusedBorderColor: Color = Color(0xfff39c12)
) = composed {
  val boxInteractionSource = remember { MutableInteractionSource() }
  val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
  val animatedBorderColor by animateColorAsState(
    targetValue =
      if (isItemFocused) focusedBorderColor
      else unfocusedBorderColor
  )
  var previousPress: PressInteraction.Press? by remember {
    mutableStateOf(null)
  }
  val scope = rememberCoroutineScope()
  var boxSize by remember {
    mutableStateOf(IntSize(0, 0))
  }

  LaunchedEffect(isItemFocused) {
    previousPress?.let {
      if (!isItemFocused) {
        boxInteractionSource.emit(
          PressInteraction.Release(
            press = it
          )
        )
      }
    }
  }

  this
    .onGloballyPositioned {
      boxSize = it.size
    }
    .clickable(
      interactionSource = boxInteractionSource,
      indication = rememberRipple()
    ) {
      onClick()
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
            onClick()
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
      width = borderWidth,
      color = animatedBorderColor
    )
}
```
[DpadFocusable.kt](app/src/main/java/dev/berggren/DpadFocusable.kt)

## Reacting to click events in the grid
With a scrollable grid and a modifier for reacting to click events, let's put the two parts together to react to clicks on items in the grid.

First, we'll create a banner component with a text and a circle indicating for visualizing what color has been clicked last:

```kotlin
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
```

We then place the banner above the grid and add some state for keeping track of what color has been  clicked.
We update this state by adding our newly created `.dpadFocusable` modifier to the colored box item:

```kotlin
var colorClicked: Color by remember { mutableStateOf(Color.Transparent) }

MaterialTheme {
  Column(
    Modifier
      .fillMaxSize()
      .background(Color(0xffecf0f1))
      .padding(start = 24.dp, top = 24.dp)
  ) {
    ColorClickedBanner(color = colorClicked)
    Spacer(Modifier.height(24.dp))
    ScrollableGrid(
      items = boxColors,
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
```
[MainActivity.kt](app/src/main/java/dev/berggren/MainActivity.kt)

Voilà!
We now have a grid that is scrollable, where items can be navigated using the d-pad, and where center clicks cause and indication and the banner to update based on the item clicked.

![Navigating and clicking on grid items](https://media.giphy.com/media/QxXT07irVKDZwuq18S/giphy.gif)

## Next steps: scrolling
As you may notice, items that are outside of the screen are currently inaccessible.
The expected behavior is that the grid scrolls when reaching the edges to expose further content, but this doesn't happen at the moment.
In the next part of this tutorial we will add support for this, stay tuned.
