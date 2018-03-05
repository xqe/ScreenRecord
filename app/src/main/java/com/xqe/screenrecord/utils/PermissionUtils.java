package com.xqe.screenrecord.utils;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by xieqe on 2018/2/27.
 */

public class PermissionUtils {
    public static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    /**
     * 检测运行时权限是否授予，若未授予则申请权限
     */
    public static void checkRuntimePermission(Activity activity) {
        for (String permission : PERMISSIONS) {
            int result = activity.checkSelfPermission(permission);
            if (result == PackageManager.PERMISSION_DENIED) {
                activity.requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * 根据Activity返回结果判断是否所有权限已被批准
     */
    public static boolean isAllPermissionAllowed(int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 跳转到当前应用详情页
     */
    public static void jumpToDetailSettings(Activity activity) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }
}
