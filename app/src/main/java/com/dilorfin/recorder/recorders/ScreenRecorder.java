package com.dilorfin.recorder.recorders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import com.dilorfin.recorder.SharedValues;
import com.dilorfin.recorder.utils.Logger;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import static android.app.Activity.RESULT_OK;

public class ScreenRecorder extends Recorder
{
    private final HBRecorder hbRecorder;

    public ScreenRecorder(Context context)
    {
        super(context);

        hbRecorder = new HBRecorder(context, (HBRecorderListener) context);
        hbRecorder.isAudioEnabled(false);
        hbRecorder.setFileName("screen");
    }

    @Override
    public void startRecording()
    {
        if (!this.isRecording())
        {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
            ((Activity) context).startActivityForResult(permissionIntent, SharedValues.SCREEN_RECORD_REQUEST_CODE);
            Logger.writeLine("Request MediaProjection");
        }
    }

    @Override
    public void stopRecording()
    {
        if (this.isRecording())
        {
            hbRecorder.stopScreenRecording();
            Logger.writeLine("Stop recording");
        }
    }

    @Override
    public boolean isRecording() { return hbRecorder.isBusyRecording(); }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SharedValues.SCREEN_RECORD_REQUEST_CODE && resultCode == RESULT_OK)
        {
            hbRecorder.setOutputPath(SharedValues.outputPath);
            hbRecorder.startScreenRecording(data, resultCode, (Activity)context);
            Logger.writeLine("Start recording");
        }
    }
}
