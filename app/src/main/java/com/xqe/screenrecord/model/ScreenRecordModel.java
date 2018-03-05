package com.xqe.screenrecord.model;

import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Created by xieqe on 2018/3/2.
 */

public class ScreenRecordModel implements Runnable {
    private static final String TAG = "ScreenShotModel";
    private ImageReader imageReader;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private int displayWidth;
    private int displayHeight;
    private int screenDensity;
    private MediaCodec mediaCodec;
    private boolean isRunning = false;
    private RandomAccessFile h264File;

    private ScreenRecordModel() {
    }

    public static ScreenRecordModel getInstance() {
        return Holder.INSTANCE;
    }

    //选择编码器级别
    private static MediaCodecInfo selectCodec() {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (MediaFormat.MIMETYPE_VIDEO_AVC.equalsIgnoreCase(type)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public void init(MediaProjection mediaProjection, WindowManager windowManager) {
        isRunning = false;
        this.mediaProjection = mediaProjection;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
            screenDensity = displayMetrics.densityDpi;
            displayWidth = displayMetrics.widthPixels;
            displayHeight = displayMetrics.heightPixels;
        }
    }

    /**
     * 启动mediaCodec，编码录屏surface为h264
     */
    public void startRecord(String h264FilePath) {
        if (mediaProjection != null) {
            isRunning = true;
            try {
                File file = new File(h264FilePath);
                //todo 删除已存在文件
                h264File = new RandomAccessFile(file, "rw");
                initMediaCodec();
                initVirtualDisplay();
                new Thread(this).start();
            } catch (FileNotFoundException e) {
                Log.i(TAG, "initEncoderAndVirtualDisplay: " + e.toString());
            }
        } else {
            //可选择再次请求权限
            Log.e(TAG, "startRecord: permission denied");
        }
    }

    public void stopRecord() {
        isRunning = false;
    }

    /**
     * 初始化编码器
     */
    private void initMediaCodec() {
        //1.实例化MediaCodecInfo
        int maxCodecProfileLevel = MediaCodecInfo.CodecProfileLevel.AVCLevel1;
        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            return;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaCodecInfo.CodecProfileLevel[] profileLevels = capabilities.profileLevels;
        for (MediaCodecInfo.CodecProfileLevel codecProfileLevel : profileLevels) {
            if (codecProfileLevel.profile != MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline) {
                continue;
            }
            if (codecProfileLevel.level > maxCodecProfileLevel) {
                maxCodecProfileLevel = codecProfileLevel.level;
            }
        }

        //2.创建媒体格式
        MediaFormat format = new MediaFormat();
        //设置文件类型为H264的MIME类型
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC);
        //设置媒体内容宽高
        format.setInteger(MediaFormat.KEY_WIDTH, displayWidth);
        format.setInteger(MediaFormat.KEY_HEIGHT, displayHeight);
        //设置颜色格式
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        //设置比特率(码率) 每秒按每个像素点4字节传输
        format.setInteger(MediaFormat.KEY_BIT_RATE, displayWidth * displayHeight * 4);
        //设置帧率25fps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        //设置关键帧间隔时间，单位s
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        //设置编码器配置文件，后续与level对应
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
        //设置编码器配置文件，与profile对应，如果没有指定profile，level设置可忽略
        format.setInteger(MediaFormat.KEY_LEVEL, maxCodecProfileLevel);


        //3.实例化MediaCodec
        try {
            // 实例化一个支持给定MIME类型的数据输出的编码器
            mediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
            // 配置好格式参数
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException ex) {
            Log.e(TAG, "initEncoderAndVirtualDisplay failed:" + ex.toString());
        }
    }

    /**
     * 启动录屏
     */
    private void initVirtualDisplay() {
        // 请求一个surface用于编码器的输入
        Surface encoderSurface = mediaCodec.createInputSurface();
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecord",
                displayWidth,
                displayHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                encoderSurface,
                null,
                null);
        mediaCodec.start();
        Log.i(TAG, "initEncoderAndVirtualDisplay: success");

    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            while (isRunning) {
                //开始编码
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);//获取输出区的缓冲的索引
                Log.i(TAG, "run:编码 " + encoderStatus);
                ByteBuffer[] encoderOutputBuffers = mediaCodec.getOutputBuffers();
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Thread.sleep(100);
                    Log.i(TAG, "--==wait 100ms ");
                } else if (encoderStatus >= 0) {
                    if (bufferInfo.size != 0) {
                        byte[] sendData = new byte[bufferInfo.size];
                        ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                        encodedData.get(sendData, 0, bufferInfo.size);
                        Log.i(TAG, "run: " + sendData.length);
                        //写h264到本地
                        h264File.write(sendData, 0, sendData.length);
                    }
                    mediaCodec.releaseOutputBuffer(encoderStatus, false);//释放缓存
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "run error: " + e.toString());
        }
        release();
    }

    /**
     * 释放录屏相关资源
     */
    public void release() {
        Log.i(TAG, "release");
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        /*if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }*/
    }

    /**
     * ImageReader方式截屏
     */
    public void getShotByImageReader() {
        try {
            imageReader = ImageReader.newInstance(
                    displayWidth,
                    displayHeight,
                    PixelFormat.RGBA_8888,
                    2);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //获取截图
                    Image screenShot = reader.acquireLatestImage();
                    int imageDataSize = screenShot.getHeight() * screenShot.getWidth() * 4;
                    Log.i(TAG, "onImageAvailable: " + imageDataSize);
                }
            }, null);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenRecord",
                    displayWidth,
                    displayHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                    imageReader.getSurface(),
                    null,
                    null);
        } catch (Exception exception) {
            Log.i(TAG, "startRecord: " + exception.toString());
        }
    }

    private static class Holder {
        private static final ScreenRecordModel INSTANCE = new ScreenRecordModel();
    }
}
