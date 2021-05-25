package com.kbds.daitsoandroidproject.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kbds.daitsoandroidproject.BaseActivity
import java.io.File

fun Context.getColorCompat(@ColorRes colorId: Int): Int = ContextCompat.getColor(this, colorId)

fun Context.getDrawableCompat(@DrawableRes drawableId: Int): Drawable =
    ContextCompat.getDrawable(this, drawableId) ?: throw RuntimeException("Drawable not available")

/**
 * 어플리케이션에 캐시 파일 디렉토리를 전부 삭제 합니다.
 */
fun Context.clearApplicationCache(file : File?) {

    Log.d("Context_clear", "clearApplicationCache file : $file")

    var dir = when(file){
        null -> cacheDir
        else -> file
    }

    dir?.let {
        it.listFiles().forEach { childDir ->
            when{
                childDir.isDirectory -> clearApplicationCache(childDir)
                else -> childDir.delete()
            }
        }
    }
}

fun Context.call(phoneNumber: String){
    checkCallPermission({
        //권한 획득
        var callPhoneNumber =  when(phoneNumber.startsWith("tel:")){
            true -> Uri.parse(phoneNumber)
            else -> Uri.parse("tel:$phoneNumber")
        }
        startActivity(
            Intent("android.intent.action.CALL", callPhoneNumber)
        )
    },{
        //권한 없음
        Toast.makeText(this, "전화 요청 권한이 필요합니다.", Toast.LENGTH_LONG).show()
    })
}
fun Context.checkCallPermission(successAction : ()->Unit, failAction : ()->Unit){
    try{
        val REQUEST_CODE_PHONE_CALL = 5710

        val activity = this as BaseActivity
        val permissionList: Array<String> = arrayOf(android.Manifest.permission.CALL_PHONE)

        when (hasPermissions(activity, permissionList)) {
            true -> {
                successAction.invoke()
            }
            else -> {
                activity.requestPermissions(permissionList, REQUEST_CODE_PHONE_CALL)
                activity.addPermissionCallback(REQUEST_CODE_PHONE_CALL, object :
                    BaseActivity.OnPermissionCallback {
                    override fun requestResult(
                        requestCode: Int,
                        permissions: Array<out String>,
                        grantResults: IntArray
                    ) {
                        when (requestCode) {
                            REQUEST_CODE_PHONE_CALL -> {
                                if ((grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                                    successAction.invoke()
                                    return
                                }
                                failAction.invoke()
                            }
                        }
                    }
                })
            }
        }
    }catch(e: ArrayIndexOutOfBoundsException){
        e.printStackTrace()
    }
}

fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    return true
}

/**
 * 시스템 다크모드 체크
 */
fun Context.isDarkThemeOn(): Boolean {
    return resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}
