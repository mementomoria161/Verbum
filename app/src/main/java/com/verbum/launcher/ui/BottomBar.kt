package com.verbum.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verbum.launcher.R

/**
 * The M3-style split button pinned to the bottom of the launcher:
 * a large Search segment and a small Customize segment with connected
 * corner shapes, like a Material 3 connected button group.
 *
 * In search state the bar becomes a text field; in edit mode it becomes
 * [Add folder | Done].
 */
@Composable
fun BottomBar(
    searchOpen: Boolean,
    editMode: Boolean,
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
    val containerColor = textColor.copy(alpha = 0.12f)
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
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = textColor,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            editMode -> {
                Button(
                    onClick = onAddFolder,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = bigStart,
                    colors = buttonColors,
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
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = smallEnd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Done", style = labelStyle.copy(color = MaterialTheme.colorScheme.onPrimary))
                }
            }

            searchOpen -> {
                val focusRequester = remember { FocusRequester() }
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = {
                        Text("Search apps…", style = labelStyle.copy(color = textColor.copy(alpha = 0.5f)))
                    },
                    textStyle = labelStyle,
                    shape = bigStart,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = containerColor,
                        unfocusedContainerColor = containerColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = textColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSearchSubmit() }),
                )
                Button(
                    onClick = onCloseSearch,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = smallEnd,
                    colors = buttonColors,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close search",
                        modifier = Modifier.size(20.dp),
                    )
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }

            else -> {
                Button(
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(2.8f).fillMaxHeight(),
                    shape = bigStart,
                    colors = buttonColors,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Search", style = labelStyle)
                }
                Button(
                    onClick = onOpenCustomize,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = smallEnd,
                    colors = buttonColors,
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
