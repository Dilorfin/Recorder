package com.dilorfin.recorder.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;

import com.dilorfin.recorder.SharedValues;

import java.io.File;
import java.io.IOException;

public class FrontCameraService extends Service {

    private final String TAG = "ServiceRecorder";

    private SurfaceHolder mSurfaceHolder;
    private static Camera mServiceCamera;
    private MediaRecorder mMediaRecorder;

    @Override
    public void onCreate() {
        super.onCreate();

        mServiceCamera = chooseFrontCamera();
        if (mServiceCamera == null)
        {
            Log.e(TAG, "No camera has been opened");
            return;
        }
        mSurfaceHolder = SharedValues.mSurfaceHolder;

        startRecording();
    }

    @Override
    public void onDestroy() {
        stopRecording();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Camera chooseFrontCamera()
    {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    return Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public void startRecording(){
        try {
            Camera.Parameters p = mServiceCamera.getParameters();
            Camera.Size size = null;
            for(Camera.Size s : p.getSupportedPictureSizes()) {
                if (size == null) {
                    size = s;
                }
                else {
                    if (size.width < s.width)
                    {
                        size = s;
                    }
                }
            }

            p.setPictureSize(size.width, size.height);
            mServiceCamera.setParameters(p);

            mServiceCamera.unlock();

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mServiceCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

            File file = new File(SharedValues.outputPath, "front-camera.mp4");

            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(size.width, size.height);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.setOrientationHint(270);

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            Log.d(TAG, "Recording Started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        try {
            mServiceCamera.reconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        mMediaRecorder.release();

        mServiceCamera.release();
        mServiceCamera = null;

        Log.d(TAG, "Recording Stopped");
    }
}
