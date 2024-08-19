package com.rockethat.ornaassistant

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.rememberDrawerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(
        Screen.DungeonVisits,
        Screen.Kingdom,
        Screen.OrnaHub,
        Screen.OrnaGuide,
        Screen.Settings
    )

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                items.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.route) },
                        selected = false,
                        onClick = {
                            handleNavigation(screen, context)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        content = {
            MainContent(drawerState, scope)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(drawerState: androidx.compose.material.DrawerState, scope: CoroutineScope) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Orna Assistant") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Drawer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            // ...
        }
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
