package com.craftcaller.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftcaller.app.data.CraftState
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.data.Repo
import com.craftcaller.app.ui.screens.BlockScreen
import com.craftcaller.app.ui.screens.CallsScreen
import com.craftcaller.app.ui.screens.ContactsScreen
import com.craftcaller.app.ui.screens.MessagesScreen
import com.craftcaller.app.ui.screens.Onboarding
import com.craftcaller.app.ui.screens.PremiumScreens
import com.craftcaller.app.util.CallerHub

enum class Tab(val label: String, val glyph: String) {
    Calls("Calls", "📞"),
    Contacts("Contacts", "👥"),
    Messages("Messages", "✉️"),
    Block("Block", "🚫"),
    Premium("Premium", "✨")
}

/**
 * Home shell: CraftCaller blue accents + tab layout like Truecaller.
 * Data refresh runs whenever one of the service/watch components bumps
 * CallerHub.refreshTick (and whenever the tab changes / after resumes).
 */
@Composable
fun Root() {
    var selected by remember { mutableIntStateOf(0) }
    var onboarded by remember { mutableStateOf(Prefs.onboarded) }

    if (!onboarded) {
        Onboarding(onDone = {
            onboarded = true
            Repo.refresh()
        })
        return
    }

    LaunchedEffect(Unit) {
        Repo.refresh()
        // background threads nudge refreshTick; re-render on every bump
        CallerHub.refreshTick.collect {
            Repo.refresh()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Tab.entries.forEachIndexed { i, tab ->
                    val tonal = i == selected
                    NavigationBarItem(
                        selected = tonal,
                        onClick = { selected = i },
                        icon = { Text(tab.glyph) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CraftPalette.Blue,
                            selectedTextColor = CraftPalette.Blue,
                            unselectedIconColor = CraftPalette.muted(Prefs.darkMode),
                            unselectedTextColor = CraftPalette.muted(Prefs.darkMode)
                        )
                    )
                }
            }
        }
    ) { pad ->
        val state by Repo.state.collectAsStateWithLifecycle(initialValue = Repo.state.value)
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .fillMaxSize()
        ) {
            Crossfade(targetState = selected, label = "tabcross") { t ->
                when (Tab.entries[t]) {
                    Tab.Calls -> CallsScreen(state)
                    Tab.Contacts -> ContactsScreen(state)
                    Tab.Messages -> MessagesScreen(state)
                    Tab.Block -> BlockScreen(state)
                    Tab.Premium -> PremiumScreens()
                }
            }
        }
    }
}
