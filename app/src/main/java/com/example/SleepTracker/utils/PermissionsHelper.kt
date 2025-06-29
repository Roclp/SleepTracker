// PermissionsHelper.kt
package com.example.SleepTracker.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class PermissionsHelper(
    private val activity: ComponentActivity,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    private val alarmManager by lazy { activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    companion object {
        @RequiresApi(Build.VERSION_CODES.Q)
        private val RUNTIME_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.FOREGROUND_SERVICE
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private val PERMISSION_EXPLANATIONS = mapOf(
            Manifest.permission.POST_NOTIFICATIONS to "闹钟设置，显示睡眠跟踪状态和闹钟通知",
            Manifest.permission.ACTIVITY_RECOGNITION to "体动设置，用于数据采集和睡眠质量分析",
            Manifest.permission.RECORD_AUDIO to "音频录制，用于检测打鼾和环境噪音水平",
            Manifest.permission.SCHEDULE_EXACT_ALARM to "确保闹钟在您设置的精确时间响起",
            Manifest.permission.FOREGROUND_SERVICE to "在后台跟踪您的睡眠数据"
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun hasRequiredPermissions(): Boolean {
        val standardPermissionsGranted = RUNTIME_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            standardPermissionsGranted && alarmManager.canScheduleExactAlarms()
        } else {
            standardPermissionsGranted
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getPermissionsNeedingExplanation(): List<String> {
        return RUNTIME_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED &&
                    activity.shouldShowRequestPermissionRationale(permission)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMissingRuntimePermissions(): List<String> {
        return RUNTIME_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun needsExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            !alarmManager.canScheduleExactAlarms()
        } else {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getMissingPermissions(): List<String> {
        val missingStandardPermissions = getMissingRuntimePermissions()
        return if (needsExactAlarmPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                missingStandardPermissions + Manifest.permission.SCHEDULE_EXACT_ALARM
            } else {
                TODO("VERSION.SDK_INT < S")
            }
        } else {
            missingStandardPermissions
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getPermissionExplanation(permission: String): String {
        return PERMISSION_EXPLANATIONS[permission] ?: "该权限是SleepTracker APP 必要的"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestRuntimePermissions(callback: (Boolean) -> Unit) {
        val missingPermissions = getMissingRuntimePermissions()
        if (missingPermissions.isEmpty()) {
            callback(true)
            return
        }
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            activity.startActivity(intent)
        }
    }

    fun isPermissionGranted(permission: String): Boolean {
        return if (permission == Manifest.permission.SCHEDULE_EXACT_ALARM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
