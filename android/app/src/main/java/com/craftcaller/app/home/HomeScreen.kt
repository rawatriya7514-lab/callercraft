package com.craftcaller.app.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.craftcaller.app.block.BlockScreen
import com.craftcaller.app.calls.CallsScreen
import com.craftcaller.app.contacts.ContactsScreen
import com.craftcaller.app.messages.MessagesScreen
import com.craftcaller.app.premium.PremiumScreen
import com.craftcaller.app.ui.theme.Blue

private data class TabSpec(val title: String, val icon: ImageVector)

private val tabs = listOf(
    TabSpec("Calls", Icons.Filled.Call),
    TabSpec("Contacts", Icons.Filled.People),
    TabSpec("Messages", Icons.Filled.Sms),
    TabSpec("Block", Icons.Filled.Block),
    TabSpec("Premium", Icons.Filled.Star)
)

/** Truecaller-style home shell: Calls / Contacts / Messages / Block / Premium. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAudio: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("CraftCaller", fontWeight = FontWeight.ExtraBold, color = Blue)
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search any number")
                    }
                    IconButton(
                        onClick = onOpenSettings
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Open settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> CallsScreen()
                1 -> ContactsScreen()
                2 -> MessagesScreen()
                3 -> BlockScreen()
                else -> PremiumScreen(onOpenProfile, onOpenAudio)
            }
        }
    }
}
