package dev.valerii.payflo.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import dev.valerii.payflo.BottomNavItem

class MainScreen : Screen {
    private val profileScreen = ProfileScreen()
    private val groupsScreen = GroupsScreen()

    @Composable
    override fun Content() {
        var selectedItem by remember { mutableStateOf<BottomNavItem>(BottomNavItem.Groups) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    listOf(BottomNavItem.Groups, BottomNavItem.Profile).forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selectedItem == item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedItem) {
                    BottomNavItem.Groups -> groupsScreen.Content()
                    BottomNavItem.Profile -> profileScreen.Content()
                }
            }
        }
    }
}