package dev.dettmer.simplenotes.ui.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Star
import dev.dettmer.simplenotes.R

internal val folderIcons = linkedMapOf(
    "folder" to (Icons.Default.Folder to R.string.folder_icon_folder),
    "person" to (Icons.Default.Person to R.string.folder_icon_person),
    "home" to (Icons.Default.Home to R.string.folder_icon_home),
    "work" to (Icons.Default.Work to R.string.folder_icon_work),
    "favorite" to (Icons.Default.Favorite to R.string.folder_icon_favorite),
    "school" to (Icons.Default.School to R.string.folder_icon_school),
    "travel" to (Icons.Default.Flight to R.string.folder_icon_travel),
    "shopping" to (Icons.Default.ShoppingCart to R.string.folder_icon_shopping),
    "photo" to (Icons.Default.Image to R.string.folder_icon_photo),
    "audio" to (Icons.Default.Mic to R.string.folder_icon_audio),
    "calendar" to (Icons.Default.Event to R.string.folder_icon_calendar),
    "star" to (Icons.Default.Star to R.string.folder_icon_star)
)

internal fun folderIcon(key: String?) = folderIcons[key]?.first ?: Icons.Default.Folder
