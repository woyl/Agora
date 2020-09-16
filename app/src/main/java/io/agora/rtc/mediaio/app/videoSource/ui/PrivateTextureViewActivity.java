package io.agora.rtc.mediaio.app.videoSource.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.gl.EglBase;
import io.agora.rtc.mediaio.AgoraTextureCamera;
import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSink;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.mediaio.app.BaseActivity;
import io.agora.rtc.mediaio.app.R;
import io.agora.rtc.mediaio.app.videoSource.source.AgoraLocalVideoSource;
import io.agora.rtc.mediaio.app.videoSource.source.PrivateTextureHelper;
import io.agora.rtc.mediaio.app.rtcEngine.AGEventHandler;
import io.agora.rtc.mediaio.app.rtcEngine.ConstantApp;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static io.agora.rtc.mediaio.MediaIO.BufferType.BYTE_ARRAY;
import static io.agora.rtc.mediaio.MediaIO.BufferType.TEXTURE;
import static io.agora.rtc.mediaio.MediaIO.PixelFormat.I420;
import static io.agora.rtc.mediaio.MediaIO.PixelFormat.TEXTURE_OES;

public class PrivateTextureViewActivity extends BaseActivity implements AGEventHandler {
    public final static String TAG = "PrivateTextureView";
    private Map<Integer, Boolean> mUsers;
    private String mChannelName;
    private String videoPath;
    private int mClientRole;

    private TextureView mLocalTextureView;
    private IVideoSource mVideoSource;
    private IVideoSink mRender;

    private TextureView mRemoteTextureView;
    private IVideoSink mRemoteRender;

    private SeekBar mAlphaSeekBar;
    private SeekBar mRotationSeekBar;
    private SeekBar mZoomSeekBar;

    private boolean mLocalSourceFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_texture_view);
        mUsers = new HashMap<>();
        mRemoteTextureView = (TextureView) findViewById(R.id.textureView2);
        mRemoteRender = new PrivateTextureHelper(this, mRemoteTextureView);

        SeekBarCallBack seekBarCallBack = new SeekBarCallBack();
        mAlphaSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mAlphaSeekBar.setOnSeekBarChangeListener(seekBarCallBack);
        mRotationSeekBar = (SeekBar) findViewById(R.id.seekBar2);
        mRotationSeekBar.setOnSeekBarChangeListener(seekBarCallBack);
        mZoomSeekBar = (SeekBar) findViewById(R.id.seekBar3);
        mZoomSeekBar.setOnSeekBarChangeListener(seekBarCallBack);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        worker().leaveChannel(mChannelName);
        worker().preview(false, null, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected void initUIandEvent() {
        Log.i(TAG, "initUIandEvent");





        event().addEventHandler(this);
        Intent i = getIntent();
        mChannelName = i.getStringExtra(ConstantApp.ACTION_KEY_ROOM_NAME);
        videoPath = i.getStringExtra(ConstantApp.ACTION_KEY_VIDEO_PATH);
        mClientRole = i.getIntExtra(ConstantApp.ACTION_KEY_CROLE, Constants.CLIENT_ROLE_BROADCASTER);
        mVideoSource = new AgoraTextureCamera(this, 1280, 720);
        FrameLayout container = (FrameLayout) findViewById(R.id.texture_view_container);
        if (container.getChildCount() >= 1) {
            return;
        }
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        container.getLayoutParams().width = container.getLayoutParams().height * 16 / 9;

        mLocalTextureView = new TextureView(this);
        container.addView(mLocalTextureView);
        mRender = new PrivateTextureHelper(this, mLocalTextureView);
        ((PrivateTextureHelper) mRender).init(((AgoraTextureCamera) mVideoSource).getEglContext());
        ((PrivateTextureHelper) mRender).setBufferType(TEXTURE);
        ((PrivateTextureHelper) mRender).setPixelFormat(TEXTURE_OES);

        // 配置一个 VideoEncoderConfiguration 实例，参数可参考下文中的 API 参考链接
        VideoEncoderConfiguration config = new VideoEncoderConfiguration(
                // 视频分辨率。可以选择默认的几种分辨率选项，也可以自定义
                VideoEncoderConfiguration.VD_480x480,
                // 视频编码帧率。可自定义。通常建议是 15 帧，不超过 30 帧
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                // 标准码率。也可以配置其它的码率值，但一般情况下推荐使用标准码率
                VideoEncoderConfiguration.STANDARD_BITRATE,
                // 方向模式
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                // 带宽受限时，视频编码降级偏好；MAINTAIN_QUALITY(0)，表示带宽受限时，降低帧率以保证视频质量
        );

        worker().getRtcEngine().setVideoEncoderConfiguration(config);

        worker().setVideoSource(mVideoSource);


        worker().setLocalRender(mRender);
        worker().preview(true, null, 0);
        worker().configEngine(mClientRole);

        worker().joinChannel(mChannelName, 0);



    }

    private void sreen() {
        /**
         * 判断Android横竖屏
         **/
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int orientation = mConfiguration.orientation ; //获取屏幕方向
        Log.e("msg","...........orientation......."+orientation);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
        } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //横屏
        }


        /**
         * 求取屏幕切换角度值
         **/
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        Log.e("msg",".......rotation..........."+rotation);
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        //代码设置横竖屏(landscape:横屏---portrait:竖屏)
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


    }


    /**
     * 获取摄像头方向
     **/
    public void getCameraOrientation(SurfaceTexture surfaceTexture) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        int cameraOrientation = info.orientation;
        Log.e("msg",".......cameraOrientation..........."+cameraOrientation);


        int cameraId = -1;
        //摄像
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i <= numberOfCameras; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;

                Camera.open(cameraId).setDisplayOrientation(0);
                try {
                    Camera.open(cameraId).setPreviewTexture(surfaceTexture);
                    Camera.open(cameraId).startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }

//        setCameraDisplayOrientation(this,cameraId,Camera.open(cameraId));
    }


    /**
     * 设置相机显示方向的详细解读
     **/
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        // 1.获取屏幕切换角度值。
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        // 2.获取摄像头方向。
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        // 3.设置相机显示方向。
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }






    protected void deInitUIandEvent() {
        Log.i(TAG, "deInitUIandEvent()");
        worker().leaveChannel(mChannelName);
        if (mClientRole == Constants.CLIENT_ROLE_BROADCASTER) {
            worker().preview(false, null, 0);
        }
        event().removeEventHandler(this);
        mUsers.clear();
    }


    public void onFirstRemoteVideoDecoded(final int uid, final int width, final int height, final int elapsed) {
        Log.d(TAG, "onFirstRemoteVideoDecoded");
        if (uid != config().mUid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addNewUser(uid, width, height);
                }
            });
        }
    }

    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        Log.d(TAG, "onJoinChannelSuccess");
    }

    public void onUserOffline(int uid, int reason) {
        Log.d(TAG, "onUserOffline");
    }
    //from AGEventHandler end

    private void addNewUser(int uid, int width, int height) {
        Log.d(TAG, "addNewUser");
        if (mUsers.containsKey(uid)) return;

        mUsers.put(uid, true);

        ((PrivateTextureHelper) mRemoteRender).init(null);
        ((PrivateTextureHelper) mRemoteRender).setBufferType(BYTE_ARRAY);
        ((PrivateTextureHelper) mRemoteRender).setPixelFormat(I420);
        worker().addRemoteRender(uid, mRemoteRender);
    }


    public class SeekBarCallBack implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == mAlphaSeekBar) {
                float alpha = progress / 100.0f;
                mLocalTextureView.setAlpha(1 - alpha);
            }

            if (seekBar == mRotationSeekBar) {
                float rotation = progress * 1.0f;
                mRemoteTextureView.setRotation(0f);
                mLocalTextureView.setRotation(0f);
            }

            if (seekBar == mZoomSeekBar) {
                float zoom = 1.0f + progress / 100.0f;
                Matrix matrix = new Matrix();
                matrix.postScale(zoom, zoom);
                mRemoteTextureView.setTransform(matrix);
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }


    public void switchSource(View view) {
        worker().preview(false, null, 0);

        FrameLayout container = (FrameLayout) findViewById(R.id.texture_view_container);
        container.removeAllViews();
        if (container.getChildCount() >= 1) {
            return;
        }
        mLocalTextureView = new TextureView(this);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        EglBase.Context sharedContext;
        if (mLocalSourceFlag) {
            Log.i(TAG, "switch to camera source");
            mVideoSource = new AgoraTextureCamera(this, 1280, 720);
            sharedContext = ((AgoraTextureCamera) mVideoSource).getEglContext();
            container.getLayoutParams().width = container.getLayoutParams().height * 16 / 9;
        } else {
            Log.i(TAG, "switch to local video source");
            mVideoSource = new AgoraLocalVideoSource(this, 640, 480, videoPath);
            sharedContext = ((AgoraLocalVideoSource) mVideoSource).getEglContext();
            container.getLayoutParams().width = wm.getDefaultDisplay().getWidth();
        }
        mRender = new PrivateTextureHelper(this, mLocalTextureView);
        ((PrivateTextureHelper) mRender).init(sharedContext);
        ((PrivateTextureHelper) mRender).setBufferType(TEXTURE);
        ((PrivateTextureHelper) mRender).setPixelFormat(TEXTURE_OES);
        worker().setVideoSource(mVideoSource);
        worker().setLocalRender(mRender);
        worker().preview(true, null, 0);
        mLocalSourceFlag = !mLocalSourceFlag;
        container.addView(mLocalTextureView);
    }
}
