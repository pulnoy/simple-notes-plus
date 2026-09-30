package dev.dettmer.simplenotes.models

/** 🆕 v2.7.0 (Folders): UI-Repräsentation eines Ordners (Name + optionale Farbe). */
data class Folder(
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val parentName: String? = null,
    val order: Int? = null
)

/** Legacy folders remain at the root; missing parents never hide a folder. */
fun List<Folder>.childrenOf(parent: String?): List<Folder> {
    val names = map { it.name }.toSet()
    return filter { (it.parentName?.takeIf { name -> name in names }) == parent }
        .sortedWith(compareBy<Folder> { it.order ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() })
}

/** Includes the selected folders themselves; the visited set also handles corrupt cycles. */
fun List<Folder>.subtreeNames(selected: Set<String>): Set<String> {
    val result = selected.toMutableSet()
    do {
        val added = filter { it.parentName in result }.map { it.name }.filterNot { it in result }
        result.addAll(added)
    } while (added.isNotEmpty())
    return result
}

fun List<Folder>.folderTree(): List<Pair<Folder, Int>> {
    val result = mutableListOf<Pair<Folder, Int>>()
    val visited = mutableSetOf<String>()
    fun visit(folder: Folder, depth: Int) {
        if (!visited.add(folder.name)) return
        result.add(folder to depth)
        childrenOf(folder.name).forEach { visit(it, depth + 1) }
    }
    childrenOf(null).forEach { visit(it, 0) }
    // Defensive fallback for imported cycles or incomplete remote snapshots.
    forEach { if (it.name !in visited) visit(it, 0) }
    return result
}
