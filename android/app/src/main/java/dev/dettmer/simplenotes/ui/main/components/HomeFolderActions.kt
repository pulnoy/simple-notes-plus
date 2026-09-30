package dev.dettmer.simplenotes.ui.main.components

data class HomeFolderActions(
    val select: (String?) -> Unit,
    val add: () -> Unit,
    val rename: (String) -> Unit,
    val delete: (String) -> Unit
)
