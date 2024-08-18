package com.rockethat.ornaassistant

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val items = listOf(Screen.DungeonVisits, Screen.Kingdom, Screen.OrnaHub, Screen.OrnaGuide, Screen.Settings)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(items) { screen ->
                handleNavigation(screen, context)
                coroutineScope.launch { drawerState.close() }
            }
        }, content = {
            MainContent(drawerState = drawerState)
        }
    )
}

private fun handleNavigation(screen: Screen, context: Context) {
    when (screen) {
        Screen.Kingdom -> navigateToActivity(context, KingdomActivity::class.java)
        Screen.OrnaGuide -> navigateToActivity(context, OrnaGuideActivity::class.java)
        Screen.Settings -> navigateToActivity(context, SettingsActivity::class.java)
        else -> {}
    }
}

private fun navigateToActivity(context: Context, activityClass: Class<*>) {
    context.startActivity(Intent(context, activityClass))
}

@Composable
fun DrawerContent(items: List<Screen>, onItemClicked: (Screen) -> Unit) {
    LazyColumn {
        items(items) { screen ->
            NavigationDrawerItem(
                label = { Text(screen.route) },
                selected = false,
                onClick = { onItemClicked(screen) }
            )
        }
    }
}

@Composable
fun MainContent(drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = { // Now using Material 3 IconButton
            coroutineScope.launch { drawerState.open() }
        }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        // ... your other content for the main screen
    }
}

sealed class Screen(val route: String) {
    object DungeonVisits : Screen("Dungeon Visits")
    object Kingdom : Screen("Kingdom")
    object OrnaHub : Screen("Orna hub")
    object OrnaGuide : Screen("Orna Guide")
    object Settings : Screen("Settings")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppDrawer(LocalContext.current)
}