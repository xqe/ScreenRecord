package com.xqe.screenrecord.ui;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import com.xqe.screenrecord.R;
import com.xqe.screenrecord.model.ScreenShotModel;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepare();
        requestPermission();
    }

    private void prepare() {
        mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        ScreenShotModel.getInstance().init(mediaProjectionManager,windowManager);
    }

    private void requestPermission() {
        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(screenCaptureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            ScreenShotModel.getInstance().setRecordData(resultCode, data);
        }
    }
}
