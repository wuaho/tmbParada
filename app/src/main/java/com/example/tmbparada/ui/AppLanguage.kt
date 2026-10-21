package com.example.tmbparada.ui

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Language of the app and its widgets. Per-app languages need Android 13; on older versions
 * the app follows the system language and the option is hidden.
 */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    SPANISH("es", "Español"),
    CATALAN("ca", "Català"),
    ENGLISH("en", "English"),
    ;

    companion object {
        @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        fun current(context: Context): AppLanguage {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return SYSTEM
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) return SYSTEM
            return entries.firstOrNull { it.tag == locales[0].language } ?: SYSTEM
        }

        /** Android recreates the activities; widgets are re-rendered by TmbApplication. */
        fun set(context: Context, language: AppLanguage) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        }
    }
}
