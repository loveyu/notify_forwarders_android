package com.hestudio.notifyforwarders

import android.app.Application
import android.content.Context
import com.hestudio.notifyforwarders.util.AppStateManager
import com.hestudio.notifyforwarders.util.LocaleHelper
import com.hestudio.notifyforwarders.util.ServerPreferences

class NotifyForwardersApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()

        // 初始化应用状态管理器
        AppStateManager.initialize()

        // 应用保存的语言设置
        applyLanguageSettings()
    }
    
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { context ->
            // 获取保存的语言设置
            val languageCode = ServerPreferences.getSelectedLanguage(context)
            LocaleHelper.setLocale(context, languageCode)
        })
    }
    
    private fun applyLanguageSettings() {
        val languageCode = ServerPreferences.getSelectedLanguage(this)
        LocaleHelper.setLocale(this, languageCode)
    }
}
