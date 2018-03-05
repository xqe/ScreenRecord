package com.xqe.screenrecord.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;

import com.xqe.screenrecord.R;
import com.xqe.screenrecord.model.ScreenRecordModel;
import com.xqe.screenrecord.utils.PermissionUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 100;
    MediaProjectionManager mediaProjectionManager;
    @BindView(R.id.start_record)
    Button startRecord;
    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        PermissionUtils.checkRuntimePermission(this);
        requestScreenPermission();
    }

    private void requestScreenPermission() {
        mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(screenCaptureIntent, REQUEST_CODE);
        }
    }

    @OnClick(R.id.start_record)
    public void onClick() {
        Log.i(TAG, "onClick: startRecord");
        if (isRecording) {
            ScreenRecordModel.getInstance().stopRecord();
            startRecord.setText(R.string.start);
            isRecording = false;
        } else {
            String savePath = Environment.getExternalStorageDirectory() + File.separator + "screenRecord.264";
            ScreenRecordModel.getInstance().startRecord(savePath);
            startRecord.setText(R.string.stop);
            isRecording = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + "," + resultCode);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            //权限请求成功，根据返回结果初始化ScreenRecordModel
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data);
            ScreenRecordModel.getInstance().init(mediaProjection, windowManager);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (String permission : permissions) {
            Log.i(TAG, "onRequestPermissionsResult: " + permission + "," + grantResults.length);
        }
        if (grantResults.length <= 0) {
            return;
        }
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE
                && PermissionUtils.isAllPermissionAllowed(grantResults)) {
            //所有运行时权限已被授予
            Log.i(TAG, "onRequestPermissionsResult: 所有运行时权限已被授予");
        } else {
            //仍有权限未被授予，弹窗引导用户跳转到设置界面，配置所需权限,这里暂时直接跳转到设置界面
            PermissionUtils.jumpToDetailSettings(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenRecordModel.getInstance().release();
    }
}