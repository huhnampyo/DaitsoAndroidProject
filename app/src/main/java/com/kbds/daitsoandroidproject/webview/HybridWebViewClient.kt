package com.kbds.daitsoandroidproject.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import com.kbds.daitsoandroidproject.extension.call

open class HybridWebViewClient : WebViewClient() {

    companion object {
        const val INTENT_PROTOCOL_START = "intent:"
        const val INTENT_PROTOCOL_INTENT = "#Intent;"
        const val INTENT_PROTOCOL_END = ";end;"
        const val GOOGLE_PLAY_STORE_PREFIX = "market://details?id="
        const val BDL_KEY_CLRHISTORY = "clrHistory"
        const val KEY_GLOBAL_WEB_VIEW_RELOAD = 1 + 9 shl 24
        const val KEY_GLOBAL_WEB_VIEW_RELOAD_URL = 1 + 11 shl 24
        const val KEY_LOAD_COMPLETE = 1 + 13 shl 24
        const val KEY_LOAD_ERROR = 1 + 15 shl 24
        const val WEB_VIEW_RELOAD_MAX_COUNT = 3
    }

    private var mOnPageStarted: OnPageStarted?= null

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

        Log.d("WebViewSpeed", "shouldOverrideUrlLoading url : $url")

        val smallAndroidScriptScheme = "androidland:"
        val bigAndroidScriptScheme = "ANDROIDLAND:"

        if (url.isNotEmpty() && url.startsWith("tel:")) {
            view.context.call(url)
            return true
        }

        if (url.isNotEmpty() && url.startsWith("sms:")) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
            view.context.startActivity(intent)
            return true
        }

        if (url.isNotEmpty() && url.startsWith("intent:")) {
            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val existPackage = intent.getPackage()?.let {
                    view.context.packageManager.getLaunchIntentForPackage(
                        it
                    )
                }
                if (existPackage != null) {
                    view.context.startActivity(intent)
                } else {
                    val marketIntent = Intent(Intent.ACTION_VIEW);
                    marketIntent.data = Uri.parse("market://details?id=" + intent.getPackage());
                    view.context.startActivity(marketIntent)
                }
                return true
            }catch (e : Exception){
            }
        }

        if (url.startsWith(INTENT_PROTOCOL_START)) {
            val customUrlStartIndex = INTENT_PROTOCOL_START.length
            val customUrlEndIndex = url.indexOf(INTENT_PROTOCOL_INTENT)
            if (customUrlEndIndex < 0) {
                return false
            } else {
                val customUrl = url.substring(customUrlStartIndex, customUrlEndIndex)
                try {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(customUrl)))
                } catch (e: ActivityNotFoundException) {
                    val packageStartIndex = customUrlEndIndex + INTENT_PROTOCOL_INTENT.length
                    val packageEndIndex = url.indexOf(INTENT_PROTOCOL_END)

                    val packageName =
                        url.substring(packageStartIndex, if (packageEndIndex < 0) url.length else packageEndIndex)
                    view.context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(GOOGLE_PLAY_STORE_PREFIX + packageName)
                        )
                    )
                }

                return true
            }
        } else {
            if(url.contains("FILE_NAME")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view.context.startActivity(intent)
                return true
            }
            return false
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mOnPageStarted?.onPageStarted(view, url)

        view?.setTag(KEY_LOAD_COMPLETE, false)
        view?.setTag(KEY_LOAD_ERROR, false)
        Log.d("WebViewSpeed", "onPageStarted url : $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("WebViewSpeed", "onPageFinished url : $url")
        view?.setBackgroundColor(Color.TRANSPARENT)
        view?.visibility = View.VISIBLE

        if(url?.contains("mini")!!){
            Log.d("hnp_toolbar", "onPageFinished")
        }

        view?.setTag(KEY_LOAD_COMPLETE, true)

        view?.tag?.let {
            val tag = if(it is Bundle) it as Bundle else null

            if (tag != null && tag.getBoolean(BDL_KEY_CLRHISTORY)) {
                view.clearHistory()
                view.tag = null
            }
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {

        Log.d("WebViewSpeed", "onReceivedError : ${view?.url}")
        Log.d("WebViewSpeed", "onReceivedError : $error")

        view?.setTag(KEY_LOAD_ERROR, true)
        error?.errorCode.let {

            Log.d("hnp", "onReceivedError code : $it")
            when (it) {
                ERROR_AUTHENTICATION,
                ERROR_BAD_URL,
                ERROR_FAILED_SSL_HANDSHAKE,
                ERROR_FILE,
                ERROR_FILE_NOT_FOUND,
                ERROR_IO,
                ERROR_PROXY_AUTHENTICATION,
                ERROR_REDIRECT_LOOP,
                ERROR_TIMEOUT,
                ERROR_TOO_MANY_REQUESTS -> {

                    view?.let {
                        val reloadCount = view.getTag(KEY_GLOBAL_WEB_VIEW_RELOAD)
                        reloadCount?.let {count ->
                            val countInt = (count as Int)
                            if(countInt < WEB_VIEW_RELOAD_MAX_COUNT ){
                                actionReloadWebView( countInt, it )
                            }else{

                                view.setOnTouchListener { v, event ->
                                    val reloadUrl = view.getTag(KEY_GLOBAL_WEB_VIEW_RELOAD_URL) as String
                                    view.loadUrl(reloadUrl)

                                    view.setOnTouchListener(null)
                                    true
                                }
                                Log.d("reload", "view.url : ${view.url}")
                                view.loadDataWithBaseURL(

                                    null,

                                    "<div style=\"width:100%;height:100%;display:flex;align-items:center;justify-content: center;\">\n" +
                                            "        <img src=\"file:///android_res/drawable/reload.png\">\n" +
                                            "      </div>",

                                    "text/html",

                                    "UTF-8",

                                    null)
                            }
                        }?: actionReloadWebView(0, view)
                    }
                }
                else -> {}
            }
        }

        super.onReceivedError(view, request, error)
    }

    private fun actionReloadWebView(count: Int, webView: WebView){
        val plusCount = count + 1

        webView.reload()
        webView.setTag(KEY_GLOBAL_WEB_VIEW_RELOAD, plusCount)

        val url = webView?.url
        if(!url.isNullOrEmpty() && url != "about:blank"){
            webView.setTag(KEY_GLOBAL_WEB_VIEW_RELOAD_URL, url)
        }
    }



    interface OnPageStarted {
        fun onPageStarted(view: WebView?, url: String?)
    }
}
