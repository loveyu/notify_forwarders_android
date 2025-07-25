package com.hestudio.notifyforwarders.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    
    // 支持的语言列表
    enum class SupportedLanguage(val code: String, val displayName: String, val locale: Locale) {
        SYSTEM_DEFAULT("system", "跟随系统", Locale.getDefault()),
        ENGLISH("en", "English", Locale.ENGLISH),
        CHINESE_SIMPLIFIED("zh", "简体中文", Locale.SIMPLIFIED_CHINESE),
        CHINESE_TRADITIONAL("zh-TW", "繁體中文", Locale.TRADITIONAL_CHINESE),
        JAPANESE("ja", "日本語", Locale.JAPANESE),
        RUSSIAN("ru", "Русский", Locale("ru")),
        FRENCH("fr", "Français", Locale.FRENCH),
        GERMAN("de", "Deutsch", Locale.GERMAN);
        
        companion object {
            fun fromCode(code: String): SupportedLanguage {
                return values().find { it.code == code } ?: SYSTEM_DEFAULT
            }
            
            fun getAllLanguages(): List<SupportedLanguage> {
                return values().toList()
            }
        }
    }
    
    private const val PREF_LANGUAGE = "selected_language"
    
    /**
     * 设置应用语言
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val language = SupportedLanguage.fromCode(languageCode)
        val locale = if (language == SupportedLanguage.SYSTEM_DEFAULT) {
            getSystemLocale()
        } else {
            language.locale
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * 获取当前设置的语言
     */
    fun getCurrentLanguage(context: Context): SupportedLanguage {
        val languageCode = ServerPreferences.getSelectedLanguage(context)
        return SupportedLanguage.fromCode(languageCode)
    }
    
    /**
     * 保存语言设置
     */
    fun saveLanguage(context: Context, language: SupportedLanguage) {
        ServerPreferences.saveSelectedLanguage(context, language.code)
    }
    
    /**
     * 获取系统默认语言
     */
    private fun getSystemLocale(): Locale {
        return Locale.getDefault()
    }
    
    /**
     * 更新资源配置
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLocales(android.os.LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 根据系统语言自动选择最佳匹配的语言
     */
    fun getAutoSelectedLanguage(): SupportedLanguage {
        val systemLocale = Locale.getDefault()
        val language = systemLocale.language
        val country = systemLocale.country
        
        return when {
            // 中文处理
            language == "zh" -> {
                when (country) {
                    "TW", "HK", "MO" -> SupportedLanguage.CHINESE_TRADITIONAL
                    else -> SupportedLanguage.CHINESE_SIMPLIFIED
                }
            }
            // 其他语言直接匹配
            language == "en" -> SupportedLanguage.ENGLISH
            language == "ja" -> SupportedLanguage.JAPANESE
            language == "ru" -> SupportedLanguage.RUSSIAN
            language == "fr" -> SupportedLanguage.FRENCH
            language == "de" -> SupportedLanguage.GERMAN
            // 默认使用简体中文
            else -> SupportedLanguage.CHINESE_SIMPLIFIED
        }
    }
    
    /**
     * 检查是否需要重启应用以应用语言更改
     */
    fun needsRestart(context: Context, newLanguage: SupportedLanguage): Boolean {
        val currentLanguage = getCurrentLanguage(context)
        return currentLanguage != newLanguage
    }
    
    /**
     * 获取语言显示名称（使用当前语言环境）
     */
    fun getLanguageDisplayName(context: Context, language: SupportedLanguage): String {
        return when (language) {
            SupportedLanguage.SYSTEM_DEFAULT -> context.getString(com.hestudio.notifyforwarders.R.string.language_system_default)
            SupportedLanguage.ENGLISH -> context.getString(com.hestudio.notifyforwarders.R.string.language_english)
            SupportedLanguage.CHINESE_SIMPLIFIED -> context.getString(com.hestudio.notifyforwarders.R.string.language_chinese_simplified)
            SupportedLanguage.CHINESE_TRADITIONAL -> context.getString(com.hestudio.notifyforwarders.R.string.language_chinese_traditional)
            SupportedLanguage.JAPANESE -> context.getString(com.hestudio.notifyforwarders.R.string.language_japanese)
            SupportedLanguage.RUSSIAN -> context.getString(com.hestudio.notifyforwarders.R.string.language_russian)
            SupportedLanguage.FRENCH -> context.getString(com.hestudio.notifyforwarders.R.string.language_french)
            SupportedLanguage.GERMAN -> context.getString(com.hestudio.notifyforwarders.R.string.language_german)
        }
    }
}
