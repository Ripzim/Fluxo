package app.fluxo

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("fluxo", Context.MODE_PRIVATE)

    var blockShorts: Boolean
        get() = sp.getBoolean("block_shorts", true)
        set(value) = sp.edit().putBoolean("block_shorts", value).apply()

    fun loadTheme(): FxTheme =
        sp.getString("theme", null)
            ?.let { runCatching { FxTheme.fromJson(JSONObject(it)) }.getOrNull() }
            ?: FxTheme.presets.first()

    fun saveTheme(theme: FxTheme) {
        sp.edit().putString("theme", theme.toJson().toString()).apply()
    }

    fun loadSaved(): List<FxTheme> {
        val raw = sp.getString("saved", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { FxTheme.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun saveSaved(list: List<FxTheme>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("saved", arr.toString()).apply()
    }
}
