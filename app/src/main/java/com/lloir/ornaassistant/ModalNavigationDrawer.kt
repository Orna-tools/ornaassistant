import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lloir.ornaassistant.KingdomActivity
import com.lloir.ornaassistant.OrnaGuideActivity
import com.lloir.ornaassistant.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(context: Context) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { DrawerContent(context, drawerState, scope) },
        content = { MainContent(drawerState, scope) },
        modifier = Modifier.widthIn(max = 320.dp) // Set a max width for the drawer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    context: Context,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val items = listOf(
        Screen.DungeonVisits,
        Screen.Kingdom,
        Screen.OrnaHub,
        Screen.OrnaGuide,
        Screen.Settings
    )

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
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .fillMaxWidth() // Ensure items fill the drawer width
            )
        }
    }
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
fun MainContent(drawerState: DrawerState, scope: CoroutineScope) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content goes here...
        }
    }
}

sealed class Screen(val route: String) {
    object DungeonVisits : Screen("Dungeon Visits")
    object Kingdom : Screen("Kingdom")
    object OrnaHub : Screen("Orna Hub")
    object OrnaGuide : Screen("Orna Guide")
    object Settings : Screen("Settings")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppDrawer(LocalContext.current)
}
