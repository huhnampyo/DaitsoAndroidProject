package com.kbds.daitsoandroidproject

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    //activityResult 결과값을 Activity가 아닌 다른 객체에서 받기위해 해놓은 공통 작업 ** 시작 **
    private var resultListenerList : MutableList<OnActivityResultListener> = arrayListOf()

    open fun addOnActivityResultListener(listener: OnActivityResultListener){

        resultListenerList.forEach{
            if(it == listener){
                return@addOnActivityResultListener
            }
        }

        resultListenerList.add(listener)
    }

    interface OnActivityResultListener{
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultListenerList.forEach{
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    //activityResult 결과값을 Activity가 아닌 다른 객체에서 받기위해 해놓은 공통 작업 ** 끝 **

    //퍼미션 요청을 다른곳에서 사용할 수 있도록 공통을 빼놓은 작업 ** 시작 **
    interface OnPermissionCallback {
        fun requestResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    }
    private val permissionCallback = SparseArray<OnPermissionCallback>()

    fun addPermissionCallback(requestCode: Int, callback: OnPermissionCallback) {
        permissionCallback.put(requestCode, callback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionCallback?.get(requestCode)?.let {
            it.requestResult(requestCode, permissions, grantResults)
        } ?: super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //퍼미션 요청을 다른곳에서 사용할 수 있도록 공통을 빼놓은 작업 ** 끝  **

    // 카메라 기능 구현 ( 웹에서만 사용함 어디서는 접근이 용이하게 하기 위해 여기에 구현 )
    fun runCamera(_isCapture: Boolean, reqCode: Int) : Uri? {
        var cameraImageUri: Uri? = null

        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        val path: File = filesDir
        val file = File(path, "sample.png") // sample.png 는 카메라로 찍었을 때 저장될 파일명이므로 사용자 마음대로
        // File 객체의 URI 를 얻는다.
        cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val strpa = applicationContext.packageName
            FileProvider.getUriForFile(this, "$strpa.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        if (!_isCapture) { // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.type = MediaStore.Images.Media.CONTENT_TYPE
            pickIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val pickTitle = "사진 가져올 방법을 선택하세요."
            val chooserIntent = Intent.createChooser(pickIntent, pickTitle)

            // 카메라 intent 포함시키기..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(intentCamera))
            startActivityForResult(chooserIntent, reqCode)
        } else { // 바로 카메라 실행..
            startActivityForResult(intentCamera, reqCode)
        }

        return cameraImageUri
    }

    override fun onDestroy() {
        super.onDestroy()
        resultListenerList.clear()
    }
}

data class ShowWebViewDialogData(
    val url: String,
    val actionCommand: String? = null,
    val indicatorType: String? = null,
    val requestCode: Int = 9999
)
