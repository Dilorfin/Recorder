package com.dilorfin.recorder.recorders;

import android.content.Context;
import android.content.Intent;

import com.dilorfin.recorder.services.FrontCameraService;
import com.dilorfin.recorder.utils.Logger;

public class FrontCameraRecorder extends Recorder
{
    private boolean _isRecording = false;

    public FrontCameraRecorder(Context context) { super(context); }

    public void startRecording()
    { }

    public void stopRecording()
    {
        if(!isRecording()) return;

        Logger.writeLine("Stopping service");

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

        Logger.writeLine("Starting service");

        Intent intent = new Intent(context, FrontCameraService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent);
        _isRecording = true;
    }
}
