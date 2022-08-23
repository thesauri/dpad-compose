# dpad-compose
D-pad navigation in Jetpack Compose

![Navigating and clicking on grid items](https://media.giphy.com/media/QxXT07irVKDZwuq18S/giphy.gif)

## The problem
While Android is mostly used on touch devices, the operating system can also be used with arrow keys and d-pads (directional pads).
At the time of writing, the new UI toolkit for writing native Android apps, [Jetpack Compose](https://developer.android.com/jetpack/compose), only has partial support for such navigation in its latest stable release `1.2.0`.
The library supports focus and click handling using the `clickable()` [modifier](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) to move focus between items based on directional key presses, but there are usability issues with the out-of-the-box behavior:

### No scroll padding
When moving focus to an element that is at the edge of the screen in a scrollable container, the focused element is brought visible without any padding or offset.
The consequence is that there is no indication to the user that there might be more elements accessible by scrolling further.

Consider a row consisting of four focusable boxes on a row, but where only three boxes fit in the viewport.
When focusing the third item, the viewport will look as following for the user:

![Viewport when the third element is focused, the fourth element is not visible at all to the user](images/scroll-offset-1-actual.png)

In other words, there is no indication to the user hinting that there is a fourth item as well:

![A zoomed out view that visualizes how there is actually a fourth element outside the viewport](images/scroll-offset-2-context.png)

A more desirable behavior would be that a part of the next focusable item is visible already when focusing the third element:

![Viewport when the third element is focused, but where half of the fourth element is exposed](images/scroll-offset-3-desired.png)

### No way to cancel clicks
When tapping a button, clicks can be canceled by dragging the finger out from the element before releasing the touch.
Similar behavior could be expected from a button that is clicked using the enter key, if a directional key is pressed before releasing the enter key the click could be canceled.
Out of the box, with the `.clickable()` modifier, this is no the case however.
The click handler of the newly focused element will be called instead.

### No way to specify default focusable items
When pressing a directional key, the item in the upper corner in the layout start direction is typically focused by default.
In many cases this might make sense, but in other cases it may be preferable to jump directly to the primary button on a screen.
This could, for instance, be a media play button.
Furthermore, whenever navigating to a new screen, focus will be lost and the user will have to press a d-pad key to bring it back.

### Tutorial goal
The goal of this tutorial is to explain how the aforementioned usability issues can be addressed by implementing a custom `.dpadFocusable()` modifier.

Feel free to suggest improvements by creating an issue or a pull request.

## TL;DR, just show me the code
- [DpadFocusable.kt](app/src/main/java/dev/berggren/DpadFocusable.kt)
- [ScrollableGrid.kt](app/src/main/java/dev/berggren/ScrollableGrid.kt)
- [MainActivity.kt](app/src/main/java/dev/berggren/MainActivity.kt)

## Tutorial

First, we'll create a grid with colored boxes that expand beyond the screen both vertically and horizontally.
Each row is individually scrollable horizontally while the whole grid can be scrolled vertically.
Think Netflix and TV series.

![Netflix-like grid](https://media.giphy.com/media/dve4CrRGK01RHVj2H6/giphy.gif)

First, we need a main `Column` that positions its children vertically that is scrollable:

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

Finally, we need to compose the row items.
To make our solution generic we put our existing code inside a component that allows us to pass generic items `T`.
These items are passed as a list of lists, a list of row items, of any type `T`.
In addition to this, we pass a function that maps any item `T` to a composable, i.e. what that item should look like.
Using this component we can create scrollable grids of this kind with any type of items:

```kotlin
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

These are the primary colors for each row, but to further distinguish them horizontally let's darken them progressively:

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

Next, let's add a component that is a grid item.
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
      .padding(top = 24.dp)
  ) {
    ScrollableGrid(
      items = boxColors,
    ) { color, _ ->
      ColoredBox(
        color = color
      )
    }
  }
}
```

At this stage, you should have a Netflix-like grid that is row-wise scrollable and vertically scrollable as a whole.

### Focus and click handling
The next step is to make the grid items focusable using the d-pad and to invoke click actions when clicking the center key or enter.
Similar to how adding out-of-the-box click and focus handling is done using the `clickable()` modifier, we'll create a custom modifier `dpadFocusable()` that we can attach to grid items.
This modifier has the responsibility of showing a border if focused and responding to key events appropriately.

![Dpad navigation](https://media.giphy.com/media/hzCzKy4ccr5zeg06P2/giphy.gif)

First, let's add a dummy modifier and add some arguments for configuring its behavior:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12),
    indication: Indication? = null,
    scrollPadding: Rect = Rect.Zero,
    isDefault: Boolean = false

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

In addition to visualizing whether an element is focused, we also want to visualize whether it is clicked or not.
This can be done by using the `.indication()` modifier.
In case the user did not specify a particular indication, we fall back to the default ripple indication:

```kotlin
... = compose {
  /* [...] */
  this.
    /* [...] */
    .indication(
      interactionSource = boxInteractionSource,
      indication = indication ?: rememberRipple()
    )
}
```

In the last steps, we used an interaction source (`boxInteractionSource`) to determine whether the item is focused or not.
To receive use events, we need to emit them whenever the box gains or releases focus.
Normally, `boxInteractionSource` could simply have been passed to the clickable modifier and it would have emitted the events for us.
As we want to override the default behavior, however, we need to emit these events ourselves.

When we gain focus, we emit a focus event and store it as a state variable.
When focus is lost, we emit an unfocus event that refers to the last focus event:

```kotlin
... = compose {
  /* [...] */
  var previousFocus: FocusInteraction.Focus? by remember {
      mutableStateOf(null)
  }
  this.
    /* [...] */
    .onFocusChanged { focusState ->
        if (focusState.isFocused) {
            val newFocusInteraction = FocusInteraction.Focus()
            scope.launch {
                boxInteractionSource.emit(newFocusInteraction)
            }
            previousFocus = newFocusInteraction
        } else {
            previousFocus?.let {
                scope.launch {
                    boxInteractionSource.emit(FocusInteraction.Unfocus(it))
                }
            }
        }
    }
    .focusTarget()
```

Now the items will have a border whenever they are focused.
Next, we'll add clicking.

There are two main goals when handling key events: invoke the on-click handler whenever the center key is pressed and visualizing press and release events.
But for good user experience we need to make it possible to cancel click events too.
This is done by pressing and holding the center key (or enter) and then navigating to another item before releasing the center key (similar to how a tap can be canceled by dragging the finger away from the touched element before releasing).

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
This is done by emitting a [PressInteraction.Press](https://developer.android.com/reference/kotlin/androidx/compose/foundation/interaction/PressInteraction) event to the interaction source:

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

In the snippet above, two state variables are used that haven't declared yet: `boxSize` and `previousPress`.

The default indication, ripple indications, grows from the point where the user pressed the item.
For this reason we need to specify a position for the interaction, even though a d-pad click has no inherent position.
One option that I found to look good is to have the ripple grow from the center of the item.
To achieve this we need to know the width and height of the element in question.
This can be done by adding a [onSizeChanged](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/package-summary#(androidx.compose.ui.Modifier).onSizeChanged(kotlin.Function1)) modifier that is called whenever the element's size changes.
We keep track of the size and update it whenever the modifier's block is called:

```kotlin
... = compose {
  /* [...] */
  var boxSize by remember {
    mutableStateOf(IntSize(0, 0))
  }
  this.
    /* [...] */
    .onSizeChanged {
        boxSize = it
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
... = compose {
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

With all these parts, we end up with a box that can capture focus and handle clicks:

![Press interactions on a box](https://media.giphy.com/media/SurFCuZaFLoagcuIbr/giphy.gif)

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

We then place the banner above the grid and add some state for keeping track of what color has been clicked.
We update this state by adding our newly created `.dpadFocusable` modifier to the colored box item:

```kotlin
var colorClicked: Color by remember { mutableStateOf(Color.Transparent) }

MaterialTheme {
  Column(
    Modifier
      .fillMaxSize()
      .background(Color(0xffecf0f1))
      .padding(top = 24.dp)
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

### Scroll padding
Items outside of the scroll viewport are currently visible, but there is nothing that guarantees that the next item is exposed to hint the user that there are more elements in the list.
In this part of the tutorial, we will use the `BringIntoViewRequester()` modifier to achieve this behavior.

`BringIntoViewRequester()` can be used to request an item to be made visible.
When called, the parent scroll containers will scroll to ensure that it is made visible.
It also supports passing a rectangle that specifies exactly what local coordinates should be made visible.
We will use this to add "scroll padding" and expose parts of the next item.

First, we will create a `BringIntoViewRequester()` and attach it to the item:

```kotlin
... = compose {
  /* [...] */
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  this
    .bringIntoViewRequester(bringIntoViewRequester)
    /* [...] */
```

Then, whenever we gain focus, we call the bring into view requester with the user-passed padding added to the item's inherent size:

```kotlin
fun Modifier.dpadFocusable(
  /* [...] */
  scrollPadding: Rect = Rect.Zero,
  /* [...] */
) = composed {
  this
    .onFocusChanged { focusState ->
        if (focusState.isFocused) {
            /* [...] */
            scope.launch {
                val visibilityBounds = Rect(
                    left = -1f * scrollPadding.left,
                    top = -1f * scrollPadding.top,
                    right = boxSize.width + scrollPadding.right,
                    bottom = boxSize.height + scrollPadding.bottom
                )
                bringIntoViewRequester.bringIntoView(visibilityBounds)
            }
            /* [...] */
        } else {
          /* [...] */
        }
    }
```

Finally, we pass a visibility padding that corresponds to the spacing between the boxes and half of a box width.
Before passing this value, we need to convert it from density pixels to on-screen pixels:

```kotlin
/* [...] */
ScrollableGrid(
    items = boxColors
) { color, position ->
    val elementPaddingAndHalfOfNextBox = with(LocalDensity.current) {
        (boxPadding + boxSize.div(2)).toPx()
    }
    ColoredBox(
        Modifier.dpadFocusable(
            /* [...] */
            scrollPadding = Rect(
                left = elementPaddingAndHalfOfNextBox,
                top = elementPaddingAndHalfOfNextBox,
                right = elementPaddingAndHalfOfNextBox,
                bottom = elementPaddingAndHalfOfNextBox
            ),
            /* [...] */
        ),
        /* [...] */
    )
}
```

Now when scrolling towards the edge of the viewport, half of the next element is exposed.

![Navigating towards the edge of the screen with scroll padding](https://media.giphy.com/media/HNzstQp3Bbm5TyqsJU/giphy.gif)

### Supporting touch
As we aren't using the `.clickable()` modifier, we have lost support for tapping the element using touch.
In an Android TV environment that is purely d-pad based, this may not matter much.
But on other environments that support both modes this is essential.

We'll add support for touch by listening to whether we're in keyboard or touch mode.
Whenever we're in touch mode, we'll simply attach a `.clickable()` modifier and let it handle touch events for us:

```kotlin
... = compose {
  /* [...] */
  val inputMode = LocalInputModeManager.current

  if (inputMode.inputMode == InputMode.Touch)
      this.clickable(
          interactionSource = boxInteractionSource,
          indication = indication ?: rememberRipple()
      ) {
          onClick()
      }
  } else {
    /* D-pad modifier code */
  }
```

### Specifying a default focusable item

Next we'll add a mechanism for specifying whether an item is the default focusable item on a screen.
This solves two problems.
First, we can ensure that the primary item on a screen is always focused first (for instance, a media play button).
Secondly, we can ensure that some element is focused automatically after navigating to a new screen (by default, no item is focused at all).

We implement this by passing a `isDefault` parameter and requesting focus whenever the item is composed, as long as we are the default item and in keyboard mode.
To do this we'll attach a focus requester:

```kotlin
fun Modifier.dpadFocusable(
  /* [...] */
    isDefault: Boolean = false
) = composed {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(inputMode.inputMode) {
      when (inputMode.inputMode) {
          InputMode.Keyboard -> {
              if (isDefault) {
                  focusRequester.requestFocus()
              }
          }
          InputMode.Touch -> {}
      }
  }
  /* [...] */

  this
    /* [...] */
    .focusRequester(focusRequester)
    /* [...] */
```

Note that the focus requester has to be attached _before_ the focus target.

In the main activity, we now specify the item in the upper-left corner to be default element:

```kotlin
ScrollableGrid(
    items = boxColors
) { color, position ->
    /* [...] */
    ColoredBox(
        Modifier.dpadFocusable(
            /* [...] */
            isDefault = position.rowIndex == 0 && position.columnIndex == 0
        ),
        /* [...] */
    )
}
```

Now the item in the upper-left corner will be focused automatically whenever the grid appears and we previously have been navigating using the keyboard or d-pad.

## Summary
With all these changes in place we now have a custom d-pad modifier that solves some of the usability problems with using `.clickable` directly.
More specifically, there is now support for scroll padding and helping the user find subsequent items even at the edge of the viewport.
The user can also cancel clicks similar to how its done in a touch environment by moving focus away from the pressed item before the center or enter key is released.
Finally, default focus items can also be specified to direct users to the primary action button on a given screen.

[DpadFocusable.kt](app/src/main/java/dev/berggren/DpadFocusable.kt)
```kotlin
package dev.berggren

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalComposeUiApi
fun Modifier.dpadFocusable(
    onClick: () -> Unit,
    borderWidth: Dp = 4.dp,
    unfocusedBorderColor: Color = Color(0x00f39c12),
    focusedBorderColor: Color = Color(0xfff39c12),
    indication: Indication? = null,
    scrollPadding: Rect = Rect.Zero,
    isDefault: Boolean = false
) = composed {
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val boxInteractionSource = remember { MutableInteractionSource() }
    val isItemFocused by boxInteractionSource.collectIsFocusedAsState()
    val animatedBorderColor by animateColorAsState(
        targetValue =
        if (isItemFocused) focusedBorderColor
        else unfocusedBorderColor
    )
    var previousFocus: FocusInteraction.Focus? by remember {
        mutableStateOf(null)
    }
    var previousPress: PressInteraction.Press? by remember {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    var boxSize by remember {
        mutableStateOf(IntSize(0, 0))
    }
    val inputMode = LocalInputModeManager.current

    LaunchedEffect(inputMode.inputMode) {
        when (inputMode.inputMode) {
            InputMode.Keyboard -> {
                if (isDefault) {
                    focusRequester.requestFocus()
                }
            }
            InputMode.Touch -> {}
        }
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

    if (inputMode.inputMode == InputMode.Touch)
        this.clickable(
            interactionSource = boxInteractionSource,
            indication = indication ?: rememberRipple()
        ) {
            onClick()
        }
    else
        this
            .bringIntoViewRequester(bringIntoViewRequester)
            .onSizeChanged {
                boxSize = it
            }
            .indication(
                interactionSource = boxInteractionSource,
                indication = indication ?: rememberRipple()
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    val newFocusInteraction = FocusInteraction.Focus()
                    scope.launch {
                        boxInteractionSource.emit(newFocusInteraction)
                    }
                    scope.launch {
                        val visibilityBounds = Rect(
                            left = -1f * scrollPadding.left,
                            top = -1f * scrollPadding.top,
                            right = boxSize.width + scrollPadding.right,
                            bottom = boxSize.height + scrollPadding.bottom
                        )
                        bringIntoViewRequester.bringIntoView(visibilityBounds)
                    }
                    previousFocus = newFocusInteraction
                } else {
                    previousFocus?.let {
                        scope.launch {
                            boxInteractionSource.emit(FocusInteraction.Unfocus(it))
                        }
                    }
                }
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
            .focusRequester(focusRequester)
            .focusTarget()
            .border(
                width = borderWidth,
                color = animatedBorderColor
            )
}
```
