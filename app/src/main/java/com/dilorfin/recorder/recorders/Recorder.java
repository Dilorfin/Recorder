package com.dilorfin.recorder.recorders;

import android.content.Context;
import android.content.Intent;

public abstract class Recorder {
    protected final Context context;

    public Recorder(Context context)
    {
        this.context = context;
    }

    public abstract void startRecording();
    public abstract void stopRecording();
    public abstract boolean isRecording();

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    { }
}
