package sa.kono.app.ui.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.Locale

/**
 * Arabic ⇄ English. Screens are written with Arabic source strings (as on iOS);
 * when English is selected every [Text] looks its string up in `assets/en.json`
 * — the same dictionary the iOS app ships — and the layout flips to LTR.
 */
object AppLanguage {
    var lang by mutableStateOf("ar"); private set
    val isArabic get() = lang == "ar"
    val locale: Locale get() = if (isArabic) Locale("ar") else Locale.ENGLISH

    private lateinit var prefs: android.content.SharedPreferences
    private val exact = HashMap<String, String>()
    private val patterns = ArrayList<Pair<Regex, String>>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("kono.app", Context.MODE_PRIVATE)
        lang = prefs.getString("lang", "ar") ?: "ar"
        runCatching {
            val json = JSONObject(context.assets.open("en.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            json.keys().forEach { k ->
                val v = json.getString(k)
                if ('%' in k) patterns.add(keyToRegex(k) to v) else exact[k] = v
            }
        }
    }

    fun set(l: String) { lang = l; prefs.edit().putString("lang", l).apply() }

    /** `"%lld صنف"` → regex `^(-?\d+) صنف$`. */
    private fun keyToRegex(key: String): Regex {
        val sb = StringBuilder("^")
        var i = 0
        while (i < key.length) {
            when {
                key.startsWith("%@", i) -> { sb.append("(.+?)"); i += 2 }
                key.startsWith("%lld", i) -> { sb.append("(-?\\d+)"); i += 4 }
                key.startsWith("%%", i) -> { sb.append("%"); i += 2 }
                else -> { sb.append(Regex.escape(key[i].toString())); i++ }
            }
        }
        return Regex(sb.append("$").toString())
    }

    /** Translates an Arabic UI string; returns it unchanged in Arabic or when there is no entry. */
    fun tr(s: String): String {
        if (isArabic || s.isEmpty()) return s
        exact[s]?.let { return it }
        for ((re, tpl) in patterns) {
            val m = re.matchEntire(s) ?: continue
            val args = m.groupValues.drop(1).map { tr(it) }
            var n = 0
            // %1$@ / %2$lld (numbered) or plain %@ / %lld (sequential)
            return Regex("%(\\d+)\\$(?:@|lld)|%(?:@|lld)|%%").replace(tpl) { r ->
                when {
                    r.value == "%%" -> "%"
                    r.groupValues[1].isNotEmpty() -> args.getOrElse(r.groupValues[1].toInt() - 1) { "" }
                    else -> args.getOrElse(n++) { "" }
                }
            }
        }
        return s
    }
}
