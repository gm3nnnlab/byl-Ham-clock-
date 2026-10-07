package com.hamdash.chornobyl.settings

import java.util.Locale

/** Languages the app is translated into. Names are shown in their own language on purpose. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    UKRAINIAN("uk", "Українська");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        /** First launch: follow the device language when it is Ukrainian, otherwise English. */
        fun fromDevice(): AppLanguage =
            if (Locale.getDefault().language == "uk") UKRAINIAN else ENGLISH

        fun fromName(name: String?): AppLanguage? = entries.firstOrNull { it.name == name }
    }
}
