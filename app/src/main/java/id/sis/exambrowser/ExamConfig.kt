package id.sis.exambrowser

import android.content.Context
import android.net.Uri

object ExamConfig {

    private const val PREF_NAME = "exam_config"
    private const val KEY_BASE_URL = "base_exam_url"
    private const val DEFAULT_BASE_URL = ""

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getBaseUrl(context: Context): String {
        return prefs(context).getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_BASE_URL, url.trim()).apply()
    }

    fun buildExamUrl(context: Context, sessionToken: String): String {
        val base = getBaseUrl(context).trimEnd('/')
        if (sessionToken.isEmpty()) {
            return base
        }
        return "$base?sessionToken=$sessionToken"
    }

    fun allowedHost(context: Context): String {
        val base = getBaseUrl(context)
        return Uri.parse(base).host.orEmpty()
    }
}
