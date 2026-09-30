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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Flag

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
    "star" to (Icons.Default.Star to R.string.folder_icon_star),
    "book" to (Icons.Default.MenuBook to R.string.folder_icon_book),
    "health" to (Icons.Default.MedicalServices to R.string.folder_icon_health),
    "fitness" to (Icons.Default.FitnessCenter to R.string.folder_icon_fitness),
    "music" to (Icons.Default.MusicNote to R.string.folder_icon_music),
    "movie" to (Icons.Default.Movie to R.string.folder_icon_movie),
    "games" to (Icons.Default.SportsEsports to R.string.folder_icon_games),
    "restaurant" to (Icons.Default.Restaurant to R.string.folder_icon_restaurant),
    "coffee" to (Icons.Default.LocalCafe to R.string.folder_icon_coffee),
    "pets" to (Icons.Default.Pets to R.string.folder_icon_pets),
    "nature" to (Icons.Default.Park to R.string.folder_icon_nature),
    "car" to (Icons.Default.DirectionsCar to R.string.folder_icon_car),
    "bike" to (Icons.Default.DirectionsBike to R.string.folder_icon_bike),
    "train" to (Icons.Default.Train to R.string.folder_icon_train),
    "beach" to (Icons.Default.BeachAccess to R.string.folder_icon_beach),
    "money" to (Icons.Default.AccountBalanceWallet to R.string.folder_icon_money),
    "bank" to (Icons.Default.AccountBalance to R.string.folder_icon_bank),
    "receipt" to (Icons.Default.ReceiptLong to R.string.folder_icon_receipt),
    "gift" to (Icons.Default.CardGiftcard to R.string.folder_icon_gift),
    "idea" to (Icons.Default.Lightbulb to R.string.folder_icon_idea),
    "tools" to (Icons.Default.Build to R.string.folder_icon_tools),
    "computer" to (Icons.Default.Computer to R.string.folder_icon_computer),
    "phone" to (Icons.Default.Smartphone to R.string.folder_icon_phone),
    "mail" to (Icons.Default.Mail to R.string.folder_icon_mail),
    "cloud" to (Icons.Default.Cloud to R.string.folder_icon_cloud),
    "security" to (Icons.Default.Shield to R.string.folder_icon_security),
    "science" to (Icons.Default.Science to R.string.folder_icon_science),
    "language" to (Icons.Default.Language to R.string.folder_icon_language),
    "flag" to (Icons.Default.Flag to R.string.folder_icon_flag)
)

internal fun folderIcon(key: String?) = folderIcons[key]?.first ?: Icons.Default.Folder
