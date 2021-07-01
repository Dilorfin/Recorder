package com.dilorfin.recorder.recorders;

import android.content.Context;
import android.content.Intent;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.dilorfin.recorder.MainActivity;
import com.dilorfin.recorder.SharedValues;
import com.dilorfin.recorder.utils.Logger;
import com.lmy.codec.encoder.Encoder;
import com.lmy.codec.presenter.VideoRecorder;
import com.lmy.codec.presenter.impl.VideoRecorderImpl;
import com.lmy.codec.wrapper.CameraWrapper;

import java.io.File;

public class FrontCameraRecorder extends Recorder
{
    private final VideoRecorderImpl mRecorder;

    public FrontCameraRecorder(Context context) {
        super(context);
        TextureView mTextureView = new TextureView(context);
        mTextureView.setFitsSystemWindows(true);
        mTextureView.setKeepScreenOn(true);

        ((MainActivity)context).mTextureContainer.addView(mTextureView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT)
        );

        mRecorder = new VideoRecorderImpl(context);
        mRecorder.reset();
        mRecorder.setOutputSize(720, 1280);
        mRecorder.setFps(30);
        mRecorder.enableHardware(true);
        mRecorder.setCameraIndex(CameraWrapper.CameraIndex.FRONT);
        mRecorder.setPreviewDisplay(mTextureView);
        VideoRecorder.OnStateListener onStateListener = new VideoRecorder.OnStateListener() {
            @Override
            public void onRecord(Encoder encoder, long timeUs) {
            }

            @Override
            public void onPrepared(Encoder encoder) {
            }

            @Override
            public void onError(int error, String msg) {
                ((MainActivity)context).onError("Front Camera Error: " + msg);
            }

            @Override
            public void onStop() {
            }
        };
        mRecorder.setOnStateListener(onStateListener);

        File file = new File(SharedValues.outputPath, "front-camera.mp4");
        mRecorder.setOutputUri(file.getAbsolutePath());
        mRecorder.prepare();
    }

    @Override
    public void prepare() {
        if (this.isRecording()){
            mRecorder.stop();
        }
        //mRecorder.reset();
        //File file = new File(SharedValues.outputPath, "front-camera.mp4");
        //mRecorder.setOutputUri(file.getAbsolutePath());
        //mRecorder.prepare();
    }

    @Override
    public void start() { }

    @Override
    public void stop()
    {
        if(!isRecording()) return;

        mRecorder.stop();
        mRecorder.reset();

        Logger.debug("Stopping front camera");
    }

    @Override
    public boolean isRecording() {
        return mRecorder.started();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(isRecording()) return;

        if (mRecorder.prepared())
        {
            mRecorder.start();
            Logger.debug("Starting front camera");
        }
        else
        {
            Logger.error("Front camera isn't prepared to start");
        }
    }
}
