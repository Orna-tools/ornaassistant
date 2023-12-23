package com.rockethat.ornaassistant

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CustomModalDrawer(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope() // Create a coroutine scope
    val items = listOf("Dungeon Visits", "Kingdom", "Orna hub", "Orna Guide", "Settings")

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(items = items) { selectedItem ->
                handleNavigation(selectedItem, context)
                coroutineScope.launch {
                    drawerState.close()
                }
            }
        }
    ) {
        MainContent(drawerState = drawerState)
    }
}

private fun handleNavigation(item: String, context: Context) {
    when (item) {
        "Kingdom" -> navigateToActivity(context, KingdomActivity::class.java)
        "Orna hub" -> navigateToActivity(context, OrnaHubActivity::class.java)
        "Orna Guide" -> navigateToActivity(context, OrnaGuideActivity::class.java)
        "Settings" -> navigateToActivity(context, SettingsActivity::class.java)
    }
}
private fun navigateToActivity(context: Context, activityClass: Class<*>) {
    context.startActivity(Intent(context, activityClass))
}

@Composable
fun DrawerContent(items: List<String>, onItemClicked: (String) -> Unit) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    items.forEach { item ->
        Text(
            text = item,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onItemClicked(item) },
            color = onSurfaceColor,
        )
    }
}

@Composable
fun MainContent(drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = {
            coroutineScope.launch {
                drawerState.open()
            }
        }) {
            val iconColor = MaterialTheme.colorScheme.onSurface
            val isDarkMode = isSystemInDarkTheme()

            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = if (isDarkMode) Color.White else iconColor
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CustomModalDrawer(LocalContext.current)
}