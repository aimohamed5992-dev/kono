package sa.kono.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/** The last few successful searches, persisted across launches. */
object RecentSearches {
    private lateinit var prefs: android.content.SharedPreferences
    var items by mutableStateOf<List<String>>(emptyList()); private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("kono.app", Context.MODE_PRIVATE)
        items = runCatching {
            val a = JSONArray(prefs.getString("recentSearches", "[]"))
            List(a.length()) { a.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun record(raw: String) {
        val q = raw.trim()
        if (q.length < 2) return
        items = (listOf(q) + items.filterNot { it.equals(q, ignoreCase = true) }).take(8)
        save()
    }

    fun clear() { items = emptyList(); save() }

    private fun save() = prefs.edit().putString("recentSearches", JSONArray(items).toString()).apply()
}
