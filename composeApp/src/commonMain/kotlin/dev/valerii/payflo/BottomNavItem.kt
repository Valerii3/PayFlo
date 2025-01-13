package dev.valerii.payflo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val title: String, val icon: ImageVector) {
    object Groups : BottomNavItem("Groups", Icons.Default.Menu)
    object Profile : BottomNavItem("Profile", Icons.Default.Person)
}