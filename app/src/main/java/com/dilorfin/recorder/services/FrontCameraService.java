package com.dilorfin.recorder.services;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.view.SurfaceHolder;

import com.dilorfin.recorder.SharedValues;
import com.dilorfin.recorder.utils.Logger;

import java.io.File;
import java.io.IOException;

public class FrontCameraService extends Service
{
    private SurfaceHolder mSurfaceHolder;
    private static Camera mServiceCamera;
    private MediaRecorder mMediaRecorder;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mServiceCamera = chooseFrontCamera();
        if (mServiceCamera == null)
        {
            Logger.writeLine("No camera has been opened");
            return;
        }
        mSurfaceHolder = SharedValues.mSurfaceHolder;

        startRecording();
    }

    @Override
    public void onDestroy()
    {
        stopRecording();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public void startRecording()
    {
        try {
            Camera.Parameters p = mServiceCamera.getParameters();
            Camera.Size size = getMaxCameraSize(p);

            p.setPictureSize(size.width, size.height);

            mServiceCamera.setParameters(p);

            mServiceCamera.unlock();

            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setCamera(mServiceCamera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

            mMediaRecorder.setVideoEncodingBitRate(40000000);

            File file = new File(SharedValues.outputPath, "front-camera.mp4");

            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(size.width, size.height);
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.setOrientationHint(270);

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            Logger.writeLine("Recording Started");
        } catch (Exception e) {
            Logger.writeLine(e.getMessage());
        }
    }

    public void stopRecording()
    {
        try {
            mServiceCamera.reconnect();
        } catch (IOException e) {
            Logger.writeLine(e.getMessage());
        }
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        mMediaRecorder.release();

        mServiceCamera.release();
        mServiceCamera = null;

        Logger.writeLine("Recording Stopped");
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
                    Logger.writeLine("Camera failed to open: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private Camera.Size getMaxCameraSize(Camera.Parameters parameters) throws Exception
    {
        Camera.Size size = null;
        for(Camera.Size s : parameters.getSupportedPictureSizes())
        {
            if (size == null)
            {
                size = s;
            }
            else {
                if (size.width < s.width)
                {
                    size = s;
                }
            }
        }
        if (size == null)
        {
            throw new Exception("No camera size selected");
        }
        return size;
    }
}
