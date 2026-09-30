package dev.dettmer.simplenotes.sync.drive

import dev.dettmer.simplenotes.storage.FolderMeta

/** The winning entry is kept whole, including hierarchy, appearance and sibling order. */
internal fun mergeDriveFolderMetadata(
    snapshots: List<DriveSnapshot>, currentFolders: List<FolderMeta>, localOnlyFolders: Set<String>
): List<FolderMeta> = (snapshots.flatMap { it.folders } + currentFolders)
    .filter { !it.name.isNullOrBlank() }
    .groupBy { it.name.lowercase() }
    .map { (_, versions) ->
        versions.firstOrNull { it.name in localOnlyFolders && it in currentFolders }
            ?: versions.maxWith(compareBy<FolderMeta> { it.updatedAt }.thenBy { it.deleted })
    }
