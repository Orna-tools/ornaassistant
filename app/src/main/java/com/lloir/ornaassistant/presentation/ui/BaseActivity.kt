package com.lloir.ornaassistant.presentation.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lloir.ornaassistant.domain.language.LanguageManager
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var languageManager: LanguageManager
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(languageManager.applyAppLanguage(newBase))
    }
}