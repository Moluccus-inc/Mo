package mo.browser.app.module

import android.content.Context
import android.content.SharedPreferences
import java.util.prefs.Preferences

class PreferenceDatabase(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MoBrowser", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    // Define your preference keys and default values here
    companion object {
        private val KEY_DEFAULT_SEARCH_ENGINE = "DEFAULT_SEARCH_ENGINE"
        private val KEY_SELECTED_SEARCH_ENGINE = "SELECTED_SEARCH_ENGINE"
        private val KEY_BROWSER_MO_CACHE_SIZE = "BROWSER_MO_CACHE_SIZE"
        private val KEY_BROWSER_DATA_USAGE = "BROWSER_DATA_USAGE"
    }

    fun setChacheSize(size: String) {
        editor.putString(KEY_BROWSER_MO_CACHE_SIZE, size)
        editor.apply()
    }

    fun getChacheSize(): String {
        return sharedPreferences.getString(KEY_BROWSER_MO_CACHE_SIZE, "") ?: ""
    }

    fun setDefaultSearchEngine(searchEngine: String) {
        if (searchEngine.isNullOrBlank()) {
            editor.putString(KEY_DEFAULT_SEARCH_ENGINE, "https://search.brave.com/search?q=")
            editor.apply()
        } else {
            editor.putString(KEY_DEFAULT_SEARCH_ENGINE, searchEngine)
            editor.apply()
        }
    }

    fun getDefaultSearchEngine(): String {
        return sharedPreferences.getString(KEY_DEFAULT_SEARCH_ENGINE, "") ?: ""
    }

    fun setSelectedSearchEnginePosition(position: Int) {
        editor.putInt(KEY_SELECTED_SEARCH_ENGINE, position)
        editor.apply()
    }

    fun getSelectedSearchEnginePosition(): Int {
        return sharedPreferences.getInt(KEY_SELECTED_SEARCH_ENGINE, 0)
    }

    fun setCurrenDataUsage(size: Long) {
        editor.putLong(KEY_BROWSER_DATA_USAGE, size)
        editor.apply()
    }

    fun getCurrenDataUsage(): Long {
        return sharedPreferences.getLong(KEY_BROWSER_DATA_USAGE, 0)
    }
}
