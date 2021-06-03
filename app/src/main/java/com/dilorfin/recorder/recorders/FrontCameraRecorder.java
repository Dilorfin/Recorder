package com.dilorfin.recorder.recorders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dilorfin.recorder.SharedValues;
import com.dilorfin.recorder.services.FrontCameraService;

import static android.app.Activity.RESULT_OK;

public class FrontCameraRecorder extends Recorder {
    private final String TAG = "FrontCameraRecorder";

    private boolean _isRecording = false;
    public FrontCameraRecorder(Context context)
    {
        super(context);
    }

    public void startRecording()
    {

    }

    public void stopRecording()
    {
        if(!isRecording()) return;

        Log.d(TAG, "Stopping service");

        context.stopService(new Intent(context, FrontCameraService.class));
        _isRecording = false;
    }

    @Override
    public boolean isRecording() {
        return _isRecording;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(isRecording()) return;

        Log.d(TAG, "Starting service");

        Intent intent = new Intent(context, FrontCameraService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent);
        _isRecording = true;
    }
}
