package com.kbds.daitsoandroidproject.extension

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context.DOWNLOAD_SERVICE
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbds.daitsoandroidproject.BaseActivity
import com.kbds.daitsoandroidproject.BuildConfig
import com.kbds.daitsoandroidproject.webview.HybridWebViewChromeClient
import com.kbds.daitsoandroidproject.webview.HybridWebViewClient
import java.net.URLDecoder


@SuppressLint("SetJavaScriptEnabled")
fun WebView.loadWithHybridInitialize(
    url: String
) {
    val thisWebview = this
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
    isScrollbarFadingEnabled = true

    setInitialScale(1)//수정하는 방법 : error : Unable to create layer for WebView, size 1008x9024 exceeds max size 8192

    downloadFileListener()

    with(settings) {

        setEnableSmoothTransition(true)
        setRenderPriority(WebSettings.RenderPriority.HIGH)

        javaScriptEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        databaseEnabled = true
        domStorageEnabled = true
        setGeolocationEnabled(true)
        loadWithOverviewMode = true
        useWideViewPort = true

        allowFileAccess = true
        allowContentAccess = true

        displayZoomControls = false

        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        isLongClickable = false

        isHapticFeedbackEnabled = false
        isScrollbarFadingEnabled = true
        isVerticalScrollBarEnabled = true
        isHorizontalFadingEdgeEnabled = false
        isHorizontalScrollBarEnabled = false

        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)

        textZoom = 100

        cacheMode = WebSettings.LOAD_NO_CACHE

        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(thisWebview, true)

        layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN;

        userAgentString = "$userAgentString APP_KBLAND"

    }

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

    webViewClient = HybridWebViewClient()

    webChromeClient = HybridWebViewChromeClient(context)

    Log.d("hnp_toolbar", "tag : ${getTag(HybridWebViewClient.KEY_LOAD_COMPLETE)}")
    val webViewLoadComplete = getTag(HybridWebViewClient.KEY_LOAD_COMPLETE)?.let {
        it as Boolean
    }?: true

    if(url.contains("gotoMainPage")){
        Log.e("hnp_toolbar", "webViewLoadComplete : $webViewLoadComplete")
    }

    loadUrl(url)

    setBackgroundColor(Color.TRANSPARENT)
}

fun WebView.downloadFileListener(){
    this.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->

        Log.d("hnp_webview", "downloadFileListener url : $url")

        val activity = context as BaseActivity

        try {
            val request = DownloadManager.Request(Uri.parse(url))
            val dm = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

            val contentDisposition = URLDecoder.decode(contentDisposition,"UTF-8"); //디코딩
            val FileName = contentDisposition.replace("attachment; filename=", "") //attachment; filename*=UTF-8''뒤에 파일명이있는데 파일명만 추출하기위해 앞에 attachment; filename*=UTF-8''제거

            val fileName = FileName //위에서 디코딩하고 앞에 내용을 자른 최종 파일명
            request.setMimeType(mimetype)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading File")
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setTitle(fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.setRequiresCharging(false)
            }

            request.allowScanningByMediaScanner()
            request.setAllowedOverMetered(true)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            dm.enqueue(request)

            Toast.makeText(context, "파일이 다운로드됩니다.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    Toast.makeText(activity, "다운로드를 위해\n권한이 필요합니다.", Toast.LENGTH_LONG)
                        .show()
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1004
                    )
                } else {
                    Toast.makeText(activity, "다운로드를 위해\n권한이 필요합니다.", Toast.LENGTH_LONG)
                        .show()
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1004
                    )
                }
            }
        }
    }
}

fun WebView.clear(){
    clearHistory()
    clearCache(true)
    loadUrl("about:blank")
}
