package com.kbds.daitsoandroidproject

import android.os.Bundle
import android.view.Window
import android.webkit.WebView
import com.kbds.daitsoandroidproject.extension.loadWithHybridInitialize
import kotlinx.android.synthetic.main.activity_fullscreen.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : BaseActivity(){

//    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)

        webView.loadWithHybridInitialize(BuildConfig.GRADLE_WEB_BASE_URL)
    }
}
