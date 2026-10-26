package com.rickdnamps.glp1tracker.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Daily JSON backup into a folder chosen by the user (Storage Access Framework), keeping the
 * newest [KEEP] files. Pointing the folder at Google Drive / a synced folder gives an off-device copy.
 */
object AutoBackup {
    const val KEEP = 7
    private const val PREFIX = "glp1-tracker-backup-"
    private const val MIN_INTERVAL_MS = 20 * 3_600_000L

    /** Returns true when a backup was written. */
    suspend fun runIfDue(context: Context, repository: TrackerRepository, force: Boolean = false): Boolean {
        val settings = repository.settingsRepository.current()
        if (settings.autoBackupUri.isBlank()) return false
        val now = System.currentTimeMillis()
        if (!force && now - settings.lastAutoBackupAt < MIN_INTERVAL_MS) return false
        val json = Backup.toJson(repository.snapshot())
        withContext(Dispatchers.IO) { write(context, Uri.parse(settings.autoBackupUri), json) }
        repository.settingsRepository.update { it.copy(lastAutoBackupAt = now) }
        return true
    }

    private data class Child(val id: String, val name: String)

    private fun write(context: Context, tree: Uri, json: String) {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(tree)
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
        val name = "$PREFIX${LocalDate.now()}.json"

        val children = mutableListOf<Child>()
        resolver.query(
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId),
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) children += Child(c.getString(0), c.getString(1) ?: "")
        }
        fun delete(child: Child) {
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, child.id)) }
        }

        children.filter { it.name == name }.forEach(::delete)
        val doc = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("Cannot create backup file")
        resolver.openOutputStream(doc, "wt")!!.use { it.write(json.toByteArray()) }

        children.filter { it.name.startsWith(PREFIX) && it.name != name }
            .sortedByDescending { it.name }
            .drop(KEEP - 1)
            .forEach(::delete)
    }
}
