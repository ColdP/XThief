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

    /**
     * Folders whose clips are hidden from the 全部 tab. Kept here rather than in the settings store
     * because it only ever refers to folders this store owns.
     */
    private val _hiddenFromAll = MutableStateFlow(loadHiddenFromAll())
    val hiddenFromAll: StateFlow<Set<String>> = _hiddenFromAll.asStateFlow()

    val currentFolders: List<String> get() = _folders.value
    val currentMembership: Map<String, Set<String>> get() = _membership.value
    val currentHiddenFromAll: Set<String> get() = _hiddenFromAll.value

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

    /** Folders [fileName] currently belongs to. */
    fun foldersOf(fileName: String): Set<String> =
        _membership.value.filterValues { fileName in it }.keys.toSet()

    /** Replaces the whole set of folders [fileName] belongs to. */
    fun setFolders(fileName: String, folders: Set<String>) {
        val next = _membership.value.mapValues { it.value - fileName }.toMutableMap()
        folders.forEach { folder -> next[folder] = next[folder].orEmpty() + fileName }
        persistMembership(next.filterValues { it.isNotEmpty() })
    }

    /** Moves [fileName] into [folder], or out of every folder when it is null. */
    fun setFolder(fileName: String, folder: String?) {
        setFolders(fileName, if (folder == null) emptySet() else setOf(folder))
    }

    /** Drops memberships whose files are gone from disk. */
    fun prune(aliveFileNames: Set<String>) {
        val next = _membership.value
            .mapValues { (_, files) -> files intersect aliveFileNames }
            .filterValues { it.isNotEmpty() }
        if (next != _membership.value) persistMembership(next)
    }

    /** Replaces the set of folders hidden from 全部; names that no longer exist are dropped. */
    fun setHiddenFromAll(folders: Set<String>) {
        val valid = folders intersect _folders.value.toSet()
        _hiddenFromAll.value = valid
        prefs.edit().putString(KEY_HIDDEN_FROM_ALL, JSONArray(valid.toList()).toString()).apply()
    }

    private fun persistFolders(list: List<String>) {
        _folders.value = list
        prefs.edit().putString(KEY_FOLDERS, JSONArray(list).toString()).apply()
        // A deleted folder cannot stay hidden.
        if (_hiddenFromAll.value.any { it !in list }) {
            setHiddenFromAll(_hiddenFromAll.value)
        }
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

    private fun loadHiddenFromAll(): Set<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY_HIDDEN_FROM_ALL, "[]"))
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.toSet()
    }.getOrDefault(emptySet())
}

private const val PREF_GROUP = "xthief_folders"
private const val KEY_FOLDERS = "folders"
private const val KEY_MEMBERSHIP = "membership"
private const val KEY_HIDDEN_FROM_ALL = "hidden_from_all"
