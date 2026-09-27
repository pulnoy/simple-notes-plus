package dev.dettmer.simplenotes.sync.drive

import dev.dettmer.simplenotes.models.Note
import dev.dettmer.simplenotes.models.SyncStatus
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/** One immutable version of a note. A tombstone has no note and remains in every snapshot. */
data class DriveNoteVersion(
    val id: String,
    val revision: Long,
    val deviceId: String,
    val note: Note? = null
)

data class DriveSnapshot(
    val formatVersion: Int = 1,
    val deviceId: String,
    val versions: List<DriveNoteVersion>,
    val folders: List<dev.dettmer.simplenotes.storage.FolderMeta> = emptyList(),
    val assets: List<dev.dettmer.simplenotes.backup.BackupAsset> = emptyList()
)

data class DriveLocalState(
    val hashes: Map<String, String> = emptyMap(),
    val revisions: Map<String, Long> = emptyMap(),
    val tombstones: Map<String, Long> = emptyMap()
)

data class DriveMergePlan(
    val notesToSave: List<Note>,
    val idsToDelete: Set<String>,
    val versionsToPublish: List<DriveNoteVersion>,
    val nextState: DriveLocalState,
    val conflictCount: Int
)

/**
 * Merges per-device snapshots. Each device writes only its own Drive file, so concurrent
 * uploads cannot overwrite each other. Equal revisions with different contents produce
 * stable conflict-copy IDs; subsequent syncs on any device converge on the same copies.
 */
object DriveMergePlanner {
    fun noteHash(note: Note): String = sha256(note.copy(syncStatus = SyncStatus.SYNCED).toJson())

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun versionHash(version: DriveNoteVersion): String =
        version.note?.let(::noteHash) ?: "deleted"

    fun merge(
        deviceId: String,
        localNotes: List<Note>,
        remoteSnapshots: List<DriveSnapshot>,
        previous: DriveLocalState,
        excludedIds: Set<String> = emptySet()
    ): DriveMergePlan {
        val local = localNotes.associateBy { it.id }
        val candidates = mutableMapOf<String, MutableList<DriveNoteVersion>>()
        remoteSnapshots.forEach { snapshot ->
            require(snapshot.formatVersion == 1) { "Unsupported Drive snapshot format" }
            snapshot.versions.forEach { version ->
                if (version.id.isNotBlank() && version.revision > 0L && version.id !in excludedIds) {
                    candidates.getOrPut(version.id) { mutableListOf() }.add(version)
                }
            }
        }

        val knownIds = previous.hashes.keys + previous.tombstones.keys
        (local.keys + knownIds).filterNot { it in excludedIds }.forEach { id ->
            val note = local[id]
            val oldHash = previous.hashes[id]
            val newHash = note?.let(::noteHash)
            val changed = when {
                note != null -> newHash != oldHash
                oldHash != null -> true // A formerly synced note was permanently removed.
                else -> false
            }
            if (changed) {
                val remoteRevision = candidates[id]?.maxOfOrNull { it.revision } ?: 0L
                val nextRevision = maxOf((previous.revisions[id] ?: 0L) + 1L, remoteRevision)
                candidates.getOrPut(id) { mutableListOf() }.add(
                    DriveNoteVersion(id, nextRevision, deviceId, note?.copy(syncStatus = SyncStatus.SYNCED))
                )
            } else if (id !in candidates) {
                val revision = previous.revisions[id] ?: 1L
                candidates.getOrPut(id) { mutableListOf() }.add(
                    DriveNoteVersion(id, revision, deviceId, note?.copy(syncStatus = SyncStatus.SYNCED))
                )
            }
        }

        val toSave = mutableListOf<Note>()
        val toDelete = mutableSetOf<String>()
        val published = mutableListOf<DriveNoteVersion>()
        val hashes = mutableMapOf<String, String>()
        val revisions = mutableMapOf<String, Long>()
        val tombstones = mutableMapOf<String, Long>()
        excludedIds.forEach { id ->
            previous.hashes[id]?.let { hashes[id] = it }
            previous.revisions[id]?.let { revisions[id] = it }
            previous.tombstones[id]?.let { tombstones[id] = it }
        }
        var conflicts = 0

        candidates.forEach { (id, entries) ->
            val maxRevision = entries.maxOf { it.revision }
            val latest = entries.filter { it.revision == maxRevision }
                .distinctBy { it.deviceId to versionHash(it) }
            val winner = latest.maxWith(
                compareBy<DriveNoteVersion> { it.note == null }
                    .thenBy { it.deviceId }
                    .thenBy(::versionHash)
            )
            revisions[id] = maxRevision
            published += winner
            if (winner.note == null) {
                tombstones[id] = maxRevision
                if (id in local) toDelete += id
            } else {
                val normalized = winner.note.copy(syncStatus = SyncStatus.SYNCED)
                hashes[id] = noteHash(normalized)
                if (local[id]?.let(::noteHash) != hashes[id]) toSave += normalized
            }

            latest.filter { versionHash(it) != versionHash(winner) }.forEach { loser ->
                val note = loser.note ?: return@forEach
                val conflictId = UUID.nameUUIDFromBytes(
                    "drive-conflict:$id:${loser.deviceId}:${loser.revision}:${versionHash(loser)}"
                        .toByteArray(StandardCharsets.UTF_8)
                ).toString()
                val copy = note.copy(
                    id = conflictId,
                    title = "${note.title} (conflit)",
                    syncStatus = SyncStatus.SYNCED
                )
                if (conflictId !in candidates && conflictId !in local) {
                    toSave += copy
                    hashes[conflictId] = noteHash(copy)
                    revisions[conflictId] = 1L
                    published += DriveNoteVersion(conflictId, 1L, deviceId, copy)
                    conflicts++
                }
            }
        }

        return DriveMergePlan(
            notesToSave = toSave,
            idsToDelete = toDelete,
            versionsToPublish = published,
            nextState = DriveLocalState(hashes, revisions, tombstones),
            conflictCount = conflicts
        )
    }
}
