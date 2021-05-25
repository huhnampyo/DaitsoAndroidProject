package com.kbds.daitsoandroidproject.webview;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.kbds.daitsoandroidproject.BaseActivity;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class HybridWebViewChromeClient extends WebChromeClient {

    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private int mOriginalOrientation;
    private FrameLayout mFullscreenContainer;
    private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private Context mContext;

    public HybridWebViewChromeClient(){}

    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;

    private Uri mCameraImageUri = null;

    public HybridWebViewChromeClient(Context context){
        mContext = context;

        if(mContext instanceof BaseActivity){
            BaseActivity activity = ((BaseActivity)mContext);

            activity.addOnActivityResultListener((requestCode, resultCode, data) -> {
                switch (requestCode){

                    case FILECHOOSER_LOLLIPOP_REQ_CODE:

                        if (resultCode == Activity.RESULT_OK) {
                            if (filePathCallbackLollipop == null) return;
                            if (data == null)
                                data = new Intent();
                            if (data.getData() == null)
                                data.setData(mCameraImageUri);

                            filePathCallbackLollipop.onReceiveValue(FileChooserParams.parseResult(resultCode, data));
                            filePathCallbackLollipop = null;
                        }else{
                            if (filePathCallbackLollipop != null){   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                                filePathCallbackLollipop.onReceiveValue(null);
                                filePathCallbackLollipop = null;
                            }
                        }
                        break;
                    default:

                        break;
                }
            });
        }
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
        WebView newWebView = new WebView(view.getContext());
        WebSettings webSettings = newWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setDomStorageEnabled(true);

        final Dialog dialog = new Dialog(view.getContext());
        dialog.setContentView(newWebView);
        dialog.show();
        newWebView.setWebChromeClient(new WebChromeClient() {
            @Override public void onCloseWindow(WebView window) {
                dialog.dismiss();
            }
        });
        ((WebView.WebViewTransport)resultMsg.obj).setWebView(newWebView);
        resultMsg.sendToTarget();
        return true;

    }

    // For Android 5.0+ 카메라 - input type="file" 태그를 선택했을 때 반응
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean onShowFileChooser(
            WebView webView, ValueCallback<Uri[]> filePathCallback,
            FileChooserParams fileChooserParams) {

        // Callback 초기화 (중요!)
        if (filePathCallbackLollipop != null) {
            filePathCallbackLollipop.onReceiveValue(null);
            filePathCallbackLollipop = null;
        }
        filePathCallbackLollipop = filePathCallback;
        boolean isCapture = fileChooserParams.isCaptureEnabled();

        if(mContext instanceof BaseActivity) {
            BaseActivity activity = ((BaseActivity) mContext);
            mCameraImageUri = activity.runCamera(isCapture, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
        return true;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }

            mOriginalOrientation = ((Activity)mContext).getRequestedOrientation();
            FrameLayout decor = (FrameLayout) ((Activity)mContext).getWindow().getDecorView();
            mFullscreenContainer = new FullscreenHolder(mContext);
            mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
            decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
            mCustomView = view;
            setFullscreen(true);
            mCustomViewCallback = callback;
//          mActivity.setRequestedOrientation(requestedOrientation);

        }

        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null) {
            return;
        }

        setFullscreen(false);
        FrameLayout decor = (FrameLayout) ((Activity)mContext).getWindow().getDecorView();
        decor.removeView(mFullscreenContainer);
        mFullscreenContainer = null;
        mCustomView = null;
        mCustomViewCallback.onCustomViewHidden();
        ((Activity)mContext).setRequestedOrientation(mOriginalOrientation);

    }

    private void setFullscreen(boolean enabled) {

        Window win = ((Activity)mContext).getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (enabled) {
            winParams.flags |= bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE|
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                        View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        } else {
            winParams.flags &= ~bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        win.setAttributes(winParams);
    }

    private static class FullscreenHolder extends FrameLayout {
        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.black));
        }

        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }
    }

    public interface OnProgressChanged{
        void onProgressChanged(WebView view, int newProgress);
    }

}
