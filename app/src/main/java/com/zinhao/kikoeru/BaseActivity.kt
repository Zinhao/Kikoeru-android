package com.zinhao.kikoeru

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*
import java.util.function.Consumer

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    ContextCompat.getColor(this,R.color.main_color),
                    ContextCompat.getColor(this,R.color.white)
                    ),
            )
        }
    }

    interface InsetReady{
        fun onInsetReady(insets: Insets)
    }

    fun setSafeArea(topView: View, insetReady: InsetReady? = null){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ViewCompat.setOnApplyWindowInsetsListener(topView) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
                // 旋转后，cutout.left 或 cutout.right 会自动有值
                // 结合系统栏和刘海，计算出最终需要的 padding
                val leftPadding = maxOf(bars.left, cutout.left)
                val rightPadding = maxOf(bars.right, cutout.right)
                val topPadding = bars.top // 横屏时状态栏高度通常变为 0 或很小
                val bottomPadding = bars.bottom

                v.setPadding(leftPadding, topPadding, rightPadding, 0)
                insetReady?.onInsetReady(Insets.of(leftPadding, topPadding, rightPadding, bottomPadding))
                insets
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_READ_CODE) {
            if (activityResultCallBack != null) {
                activityResultCallBack!!.run()
                activityResultCallBack = null
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_READ_CODE) {
            if (activityResultCallBack != null) {
                activityResultCallBack!!.run()
                activityResultCallBack = null
            }
        }
    }

    private var activityResultCallBack: Runnable? = null

    /**
     * 请求读写权限
     * 
     * @param callback 对话框被关闭时回调 或者 获取权限成功回调
     * @return
     */
    protected fun requestReadWriteExternalPermission(callback: Runnable?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val builder = AlertDialog.Builder(this,R.style.RoundedAlertDialog)
                builder.setTitle(R.string.tip)
                builder.setMessage(R.string.external_storage_tip)
                builder.setNegativeButton("去授予", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, REQUEST_WRITE_READ_CODE)
                        dialog.dismiss()
                        activityResultCallBack = callback
                    }
                })
                builder.setPositiveButton("取消", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.dismiss()
                    }
                })
                builder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                    override fun onDismiss(dialog: DialogInterface?) {
                        if (callback != null) callback.run()
                    }
                })
                builder.create().show()
                return false
            } else {
                return true
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_READ_CODE
                )
                activityResultCallBack = callback
                return false
            } else {
                return true
            }
        }
    }

    fun alertException(e: Exception) {
        if (!App.getInstance().isAppDebug() || isDestroyed()) {
            return
        }
        runOnUiThread(object : Runnable {
            override fun run() {
                val builder = AlertDialog.Builder(this@BaseActivity,R.style.RoundedAlertDialog)
                builder.setTitle(e.javaClass.getSimpleName())
                val stringBuilder = StringBuilder()
                stringBuilder.append(String.format("%s: %s", e.javaClass.getSimpleName(), e.message)).append('\n')
                Arrays.stream<StackTraceElement?>(e.getStackTrace()).forEach(object : Consumer<StackTraceElement?> {
                    override fun accept(stackTraceElement: StackTraceElement?) {
                        stringBuilder.append(stackTraceElement?.getClassName()).append('.')
                            .append(stackTraceElement?.getMethodName()).append(':')
                            .append(stackTraceElement?.getLineNumber()).append('\n')
                    }
                })
                builder.setMessage(stringBuilder.toString())
                builder.create().show()
            }
        })
    }

    protected fun alertMessage(e: AppMessage) {
        if (!App.getInstance().isAppDebug() || isDestroyed()) {
            return
        }
        runOnUiThread(object : Runnable {
            override fun run() {
                val builder = AlertDialog.Builder(this@BaseActivity,R.style.RoundedAlertDialog)
                builder.setTitle(e.getTitle())
                builder.setMessage(String.format("%s: %s", e.javaClass.getSimpleName(), e.message) + '\n')
                builder.setPositiveButton(e.getActionName(), object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        e.getAction().run()
                        dialog.dismiss()
                    }
                })
                builder.create().show()
            }
        })
    }

    override fun finish() {
        super.finish()
    }

    companion object {
        private const val REQUEST_WRITE_READ_CODE = 23
    }
}
