package com.kumhomarine.chartplotter.utils

import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * SystemSetting 앱 ContentProvider에서 읽은 언어 설정값을
 * Android Locale로 변환하여 적용하는 헬퍼.
 *
 * 저장 구조: SystemSetting 앱 SharedPreferences → ContentProvider → 다른 앱에서 query
 * 적용: AppCompatDelegate.setApplicationLocales() (현업 표준)
 */
object LocaleHelper {

    /** SystemSetting에서 저장하는 언어 표시명 → Locale 매핑 */
    fun languageToLocale(language: String): Locale = when (language) {
        "한국어" -> Locale.KOREAN
        "영어" -> Locale.ENGLISH
        "일본어" -> Locale.JAPANESE
        "중국어" -> Locale.SIMPLIFIED_CHINESE
        else -> Locale.KOREAN
    }

    /** Locale 적용 (앱 시작 시 호출) */
    fun applyLocale(language: String) {
        val locale = languageToLocale(language)
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(locale)
        )
    }
}
