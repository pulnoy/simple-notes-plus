package dev.dettmer.simplenotes.ui.main.components

data class HomeFolderActions(
    val select: (String?) -> Unit,
    val add: () -> Unit,
    val rename: (String) -> Unit,
    val delete: (String) -> Unit,
    val customize: (String) -> Unit = {},
    val move: (String, Int) -> Unit = { _, _ -> },
    val addChild: (String) -> Unit = {}
)
