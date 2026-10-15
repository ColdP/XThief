// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-created media library folders.
 *
 * A folder is just a name; membership is a set of MediaStore file names — the same key the library
 * already uses to attach downloaded-post metadata (sensitivity, favourites). Stored in its own
 * preferences file because this is user content rather than a setting.
 */
class FolderStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_GROUP, Context.MODE_PRIVATE)

    private val _folders = MutableStateFlow(loadFolders())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _membership = MutableStateFlow(loadMembership())
    val membership: StateFlow<Map<String, Set<String>>> = _membership.asStateFlow()

    val currentFolders: List<String> get() = _folders.value
    val currentMembership: Map<String, Set<String>> get() = _membership.value

    /** Appends a folder; blank names and duplicates are ignored. */
    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed in _folders.value) return
        persistFolders(_folders.value + trimmed)
    }

    fun rename(old: String, new: String) {
        val trimmed = new.trim()
        if (trimmed.isEmpty() || trimmed == old) return
        persistFolders(_folders.value.map { if (it == old) trimmed else it })
        // Carry the membership over to the new name.
        val next = _membership.value.toMutableMap()
        next.remove(old)?.let { next[trimmed] = it }
        persistMembership(next)
    }

    fun remove(name: String) {
        persistFolders(_folders.value - name)
        persistMembership(_membership.value - name)
    }

    /** Files assigned to [folder]. */
    fun filesIn(folder: String): Set<String> = _membership.value[folder].orEmpty()

    /** Moves [fileName] into [folder], or out of every folder when it is null. */
    fun setFolder(fileName: String, folder: String?) {
        val next = _membership.value.mapValues { it.value - fileName }.toMutableMap()
        if (folder != null) {
            next[folder] = next[folder].orEmpty() + fileName
        }
        persistMembership(next.filterValues { it.isNotEmpty() })
    }

    /** Drops memberships whose files are gone from disk. */
    fun prune(aliveFileNames: Set<String>) {
        val next = _membership.value
            .mapValues { (_, files) -> files intersect aliveFileNames }
            .filterValues { it.isNotEmpty() }
        if (next != _membership.value) persistMembership(next)
    }

    private fun persistFolders(list: List<String>) {
        _folders.value = list
        prefs.edit().putString(KEY_FOLDERS, JSONArray(list).toString()).apply()
    }

    private fun persistMembership(map: Map<String, Set<String>>) {
        _membership.value = map
        val root = JSONObject()
        map.forEach { (folder, files) -> root.put(folder, JSONArray(files.toList())) }
        prefs.edit().putString(KEY_MEMBERSHIP, root.toString()).apply()
    }

    private fun loadFolders(): List<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY_FOLDERS, "[]"))
        (0 until array.length())
            .map { array.optString(it) }
            .filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun loadMembership(): Map<String, Set<String>> = runCatching {
        val root = JSONObject(prefs.getString(KEY_MEMBERSHIP, "{}"))
        val result = mutableMapOf<String, Set<String>>()
        root.keys().forEach { folder ->
            val array = root.optJSONArray(folder) ?: return@forEach
            result[folder] = (0 until array.length())
                .map { array.optString(it) }
                .filter { it.isNotBlank() }
                .toSet()
        }
        result
    }.getOrDefault(emptyMap())
}

private const val PREF_GROUP = "xthief_folders"
private const val KEY_FOLDERS = "folders"
private const val KEY_MEMBERSHIP = "membership"
