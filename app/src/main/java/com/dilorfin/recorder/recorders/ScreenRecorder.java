package com.dilorfin.recorder.recorders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.dilorfin.recorder.SharedValues;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import static android.app.Activity.RESULT_OK;
import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR;

public class ScreenRecorder extends Recorder
        implements HBRecorderListener
{
    private final String TAG = "ScreenRecorder";

    private final HBRecorder hbRecorder;

    public ScreenRecorder(Context context)
    {
        super(context);

        hbRecorder = new HBRecorder(context, this);
        hbRecorder.enableCustomSettings();
        hbRecorder.isAudioEnabled(true);
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.setAudioSource("MIC");
        hbRecorder.setVideoEncoder("H264");
        hbRecorder.setScreenDimensions(1920, 1080);
        hbRecorder.setVideoFrameRate(30);
        hbRecorder.setVideoBitrate(12000000);
        hbRecorder.setOutputFormat("THREE_GPP");
        hbRecorder.setFileName("screen");

        hbRecorder.not
    }

    @Override
    public void startRecording()
    {
        if (!this.isRecording())
        {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            ((Activity) context).startActivityForResult(permissionIntent, SharedValues.SCREEN_RECORD_REQUEST_CODE);
            Log.d(TAG, "Request MediaProjection");
        }
    }

    @Override
    public void stopRecording()
    {
        if (this.isRecording())
        {
            hbRecorder.stopScreenRecording();
            Log.d(TAG, "Stop recording");
        }
    }

    @Override
    public boolean isRecording() {
        return hbRecorder.isBusyRecording();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SharedValues.SCREEN_RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            hbRecorder.setOutputPath(SharedValues.outputPath);
            hbRecorder.startScreenRecording(data, resultCode, (Activity)context);
            Log.d(TAG, "Start recording");
        }
    }

    @Override
    public void HBRecorderOnStart() {

    }

    @Override
    public void HBRecorderOnComplete() {

    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        if (errorCode == MAX_FILE_SIZE_REACHED_ERROR) {
            Log.e(TAG, "Max Size reached (" + reason + ")");
        } else if (errorCode == SETTINGS_ERROR) {
            // Error 38 happens when
            // - the selected video encoder is not supported
            // - the output format is not supported
            // - if another app is using the microphone

            Log.e(TAG, "Settings Error (" + reason + ")");
        } else {
            Log.e(TAG, errorCode + " (" + reason + ")");
        }
    }
}
