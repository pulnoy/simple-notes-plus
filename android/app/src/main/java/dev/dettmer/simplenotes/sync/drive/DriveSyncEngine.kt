package dev.dettmer.simplenotes.sync.drive

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import dev.dettmer.simplenotes.backup.BackupAsset
import dev.dettmer.simplenotes.models.SyncStatus
import dev.dettmer.simplenotes.storage.AssetStore
import dev.dettmer.simplenotes.storage.FolderStore
import dev.dettmer.simplenotes.storage.NotesStorage
import dev.dettmer.simplenotes.utils.AssetReferences
import dev.dettmer.simplenotes.utils.Constants
import dev.dettmer.simplenotes.utils.DeviceIdGenerator
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DriveSyncEngine(private val context: Context) {
    data class Outcome(val downloaded: Int, val uploaded: Int, val conflicts: Int)

    private val gson = Gson()
    private val storage = NotesStorage(context)
    private val folders = FolderStore(context)
    private val assets = AssetStore(context)

    suspend fun sync(): Outcome = mutex.withLock {
        withContext(Dispatchers.IO) {
            val token = DriveAuthorization.token(context)
                ?: throw IOException("Reconnect Google Drive in synchronization settings")
            val api = DriveApi(token)
            val deviceId = DeviceIdGenerator.getDeviceId(context)
            val ownName = DriveApi.SNAPSHOT_PREFIX + sha256(deviceId).take(24) + ".json"
            val account = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(Constants.KEY_DRIVE_ACCOUNT_EMAIL, null)
                ?: throw IOException("Google Drive account is missing")
            val stateFile = File(context.filesDir, "drive_sync_state_${sha256(account).take(24)}.json")
            val previous = readState(stateFile)

            val remoteFiles = api.listSnapshots()
            val snapshots = readSnapshots(api, remoteFiles)

            val localOnlyFolders = folders.getLocalOnlyFolderNames()
            val allLocalNotes = storage.loadAllNotes(forceReload = true)
            val excludedIds = allLocalNotes.filter { it.folderName in localOnlyFolders }.map { it.id }.toSet() +
                snapshots.flatMap { it.versions }.filter { it.note?.folderName in localOnlyFolders }.map { it.id }
            val localNotes = allLocalNotes
                .filterNot { it.folderName in localOnlyFolders }
            val originalHashes = allLocalNotes.associate { it.id to DriveMergePlanner.noteHash(it) }
            val plan = DriveMergePlanner.merge(deviceId, localNotes, snapshots, previous, excludedIds)

            // Fetch binary assets before writing any note that references them.
            restoreAssets(snapshots, plan)

            val currentFolders = folders.loadMeta()
            val mergedFolders = mergeDriveFolderMetadata(snapshots, currentFolders, localOnlyFolders)
            val publishedNotes = plan.versionsToPublish.mapNotNull { it.note }
            val snapshotAssets = collectAssets(publishedNotes)
            val outgoing = DriveSnapshot(
                deviceId = deviceId,
                versions = plan.versionsToPublish,
                folders = mergedFolders.filterNot { it.name in localOnlyFolders },
                assets = snapshotAssets
            )
            val ownFile = remoteFiles.firstOrNull { it.name == ownName }
            api.upload(ownName, ownFile?.id, gson.toJson(outgoing))

            // Local changes are applied only after the remote snapshot is safely uploaded.
            applyLocalChanges(plan, originalHashes)
            if (folders.loadMeta() == currentFolders) folders.replaceMeta(mergedFolders)
            writeState(stateFile, plan.nextState)

            // The upload has succeeded. Clear stale local sync badges without changing note content.
            clearSyncedBadges(plan.nextState)
            Outcome(plan.notesToSave.size + plan.idsToDelete.size, publishedNotes.size, plan.conflictCount)
        }
    }

    private fun readSnapshots(api: DriveApi, files: List<DriveApi.FileInfo>): List<DriveSnapshot> = files.map { file ->
        val snapshot = gson.fromJson(api.download(file.id), DriveSnapshot::class.java)
            ?: throw IOException("Invalid Google Drive sync data")
        if (snapshot.formatVersion != 1 || snapshot.deviceId.isBlank()) {
            throw IOException("Unsupported Google Drive sync data")
        }
        snapshot
    }

    private suspend fun restoreAssets(snapshots: List<DriveSnapshot>, plan: DriveMergePlan) {
        val remoteAssets = snapshots.asSequence().flatMap { it.assets.asSequence() }.distinctBy { it.name }.toList()
        val neededAssets = AssetReferences.extractAllReferenced(plan.versionsToPublish.mapNotNull { it.note })
        remoteAssets.filter { it.name in neededAssets }.forEach { asset ->
            if (!assets.getAssetFile(asset.name).exists()) {
                assets.saveAssetAs(Base64.decode(asset.dataBase64, Base64.NO_WRAP), asset.name)
            }
        }
    }

    private fun collectAssets(notes: List<dev.dettmer.simplenotes.models.Note>): List<BackupAsset> =
        AssetReferences.extractAllReferenced(notes).map { name ->
            val file = assets.getAssetFile(name)
            if (!file.exists()) throw IOException("Missing note attachment: $name")
            BackupAsset(name, Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
        }

    private suspend fun applyLocalChanges(plan: DriveMergePlan, originalHashes: Map<String, String>) {
        plan.idsToDelete.forEach { id ->
            val current = storage.loadNote(id)
            if (current != null && DriveMergePlanner.noteHash(current) == originalHashes[id]) storage.deleteNote(id)
        }
        plan.notesToSave.forEach { note ->
            val current = storage.loadNote(note.id)
            if (current?.let(DriveMergePlanner::noteHash) == originalHashes[note.id]) storage.saveNote(note)
        }
    }

    private suspend fun clearSyncedBadges(state: DriveLocalState) {
        storage.loadAllNotes(forceReload = true).forEach { note ->
            val syncedHash = state.hashes[note.id]
            if (syncedHash != null && syncedHash == DriveMergePlanner.noteHash(note) &&
                note.syncStatus != SyncStatus.SYNCED
            ) storage.saveNote(note.copy(syncStatus = SyncStatus.SYNCED))
        }
    }

    private fun readState(file: File): DriveLocalState = try {
        if (!file.exists()) DriveLocalState()
        else gson.fromJson(file.readText(), DriveLocalState::class.java) ?: DriveLocalState()
    } catch (_: Exception) {
        DriveLocalState()
    }

    private fun writeState(file: File, state: DriveLocalState) {
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(gson.toJson(state))
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private val mutex = Mutex()
    }
}
