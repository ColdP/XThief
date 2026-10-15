// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 btm_m
package btm.m.xthief.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Download history backed by a JSON document in SharedPreferences. */
class HistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_GROUP, Context.MODE_PRIVATE)
    private val _records = MutableStateFlow(load())
    val records: StateFlow<List<DownloadRecord>> = _records.asStateFlow()

    val current: List<DownloadRecord> get() = _records.value

    fun add(record: DownloadRecord) {
        val next = (listOf(record) + _records.value.filterNot { it.id == record.id })
            .sortedByDescending { it.createdAt }
        persist(next)
    }

    fun remove(id: String) {
        persist(_records.value.filterNot { it.id == id })
    }

    fun clear() {
        persist(emptyList())
    }

    fun prune(keepDays: Int) {
        if (keepDays <= 0) return
        val cutoff = System.currentTimeMillis() - keepDays * 24L * 60L * 60L * 1000L
        val next = _records.value.filter { it.createdAt >= cutoff }
        if (next.size != _records.value.size) persist(next)
    }

    private fun persist(records: List<DownloadRecord>) {
        _records.value = records
        prefs.edit().putString(KEY_RECORDS, encode(records)).apply()
    }

    private fun load(): List<DownloadRecord> = runCatching {
        val array = JSONArray(prefs.getString(KEY_RECORDS, "[]"))
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            DownloadRecord(
                id = obj.optString("id"),
                tweetId = obj.optString("tweetId"),
                author = obj.optString("author"),
                authorHandle = obj.optString("authorHandle"),
                text = obj.optString("text"),
                variantLabel = obj.optString("variantLabel"),
                bitrate = obj.optInt("bitrate"),
                fileName = obj.optString("fileName"),
                uri = obj.optString("uri"),
                byteCount = obj.optLong("byteCount"),
                createdAt = obj.optLong("createdAt"),
                completed = obj.optBoolean("completed", true),
                sensitive = obj.optBoolean("sensitive", false),
            )
        }.sortedByDescending { it.createdAt }
    }.getOrDefault(emptyList())

    private fun encode(records: List<DownloadRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("id", record.id)
                    .put("tweetId", record.tweetId)
                    .put("author", record.author)
                    .put("authorHandle", record.authorHandle)
                    .put("text", record.text)
                    .put("variantLabel", record.variantLabel)
                    .put("bitrate", record.bitrate)
                    .put("fileName", record.fileName)
                    .put("uri", record.uri)
                    .put("byteCount", record.byteCount)
                    .put("createdAt", record.createdAt)
                    .put("completed", record.completed)
                    .put("sensitive", record.sensitive),
            )
        }
        return array.toString()
    }
}

private const val PREF_GROUP = "xthief_history"
private const val KEY_RECORDS = "records"
