package com.verbum.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.launcher.R

/** Total height the bottom bar occupies: 56dp buttons + 12dp vertical padding. */
val BottomBarHeight = 80.dp

/**
 * The M3-style split button pinned to the bottom of the launcher. The button
 * group is centered and only as wide as its content. Its content color is the
 * launcher's text color; the button backgrounds are transparent while resting
 * on the homescreen and fade in only while pressed. In search state the bar
 * becomes a text field; in edit mode it becomes [Folder | Names | Done].
 */
@Composable
fun BottomBar(
    searchOpen: Boolean,
    editMode: Boolean,
    customizeOpen: Boolean,
    query: String,
    textColor: Color,
    fontFamily: FontFamily?,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchSubmit: () -> Unit,
    onOpenCustomize: () -> Unit,
    onAddFolder: () -> Unit,
    onExitEditMode: () -> Unit,
) {
    // The background that fades in (matches the settings menu pill color).
    val visibleColor = MaterialTheme.colorScheme.surfaceVariant
    val labelStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        color = textColor,
    )
    val bigStart = RoundedCornerShape(
        topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp
    )
    val smallEnd = RoundedCornerShape(
        topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            editMode -> {
                Row(
                    Modifier.height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BarButton(
                        onClick = onAddFolder,
                        shape = bigStart,
                        contentColor = textColor,
                        visibleColor = visibleColor,
                        // Edit-mode controls stay visible.
                        alwaysVisible = true,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_create_new_folder),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Folder", style = labelStyle)
                    }

                    Button(
                        onClick = onExitEditMode,
                        modifier = Modifier.fillMaxHeight(),
                        shape = smallEnd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        elevation = null,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Done",
                            style = labelStyle.copy(color = MaterialTheme.colorScheme.onPrimary),
                        )
                    }
                }
            }

            searchOpen -> {
                val focusRequester = remember { FocusRequester() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Search apps…",
                                style = labelStyle.copy(color = textColor.copy(alpha = 0.5f)),
                            )
                        },
                        textStyle = labelStyle,
                        shape = bigStart,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = visibleColor,
                            unfocusedContainerColor = visibleColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = textColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSearchSubmit() }),
                    )
                    BarButton(
                        onClick = onCloseSearch,
                        shape = smallEnd,
                        contentColor = textColor,
                        visibleColor = visibleColor,
                        alwaysVisible = true,
                        square = true,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close search",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }

            else -> {
                Row(
                    Modifier.height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Resting homescreen: transparent, fades in on press; stays
                    // visible while the settings menu is open.
                    BarButton(
                        onClick = onOpenSearch,
                        shape = bigStart,
                        contentColor = textColor,
                        visibleColor = visibleColor,
                        alwaysVisible = customizeOpen,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Search", style = labelStyle)
                    }
                    BarButton(
                        onClick = onOpenCustomize,
                        shape = smallEnd,
                        contentColor = textColor,
                        visibleColor = visibleColor,
                        alwaysVisible = customizeOpen,
                        square = true,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Customize",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * A bar button whose background is transparent at rest and fades in to
 * [visibleColor] while pressed (unless [alwaysVisible]). Content is tinted
 * with [contentColor].
 */
@Composable
private fun BarButton(
    onClick: () -> Unit,
    shape: Shape,
    contentColor: Color,
    visibleColor: Color,
    alwaysVisible: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    square: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container by animateColorAsState(
        targetValue = if (alwaysVisible || pressed) visibleColor else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "barButtonBackground",
    )

    // Icon-only buttons are a fixed square with zero padding, so the glyph is
    // perfectly centered (matching the 56dp bar height).
    val sizeModifier =
        if (square) Modifier.fillMaxHeight().width(56.dp) else Modifier.fillMaxHeight()

    Button(
        onClick = onClick,
        modifier = sizeModifier,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
        ),
        // No shadow, so a transparent button leaves nothing behind.
        elevation = null,
        contentPadding = if (square) PaddingValues(0.dp) else contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
