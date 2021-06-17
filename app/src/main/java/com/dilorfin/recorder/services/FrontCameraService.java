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

            Logger.debug("Recording Started");
        } catch (Exception exc) {
            Logger.error(exc);
        }
    }

    public void stopRecording()
    {
        try {
            mServiceCamera.reconnect();
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            mMediaRecorder.release();

        mServiceCamera.release();
            mServiceCamera = null;

            Logger.debug("Recording Stopped");
        } catch (Exception exc) {
            Logger.error(exc);
        }
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
                } catch (RuntimeException exc) {
                    Logger.error("Camera failed to open: " + exc);
                    return null;
                }
            }
        }

        Logger.error("No front camera was found");
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
