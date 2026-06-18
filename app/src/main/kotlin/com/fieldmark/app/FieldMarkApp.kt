package com.fieldmark.app

import android.app.Application
import com.fieldmark.app.i18n.LocaleManager

class FieldMarkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocaleManager.applyStored(this)
    }
}
