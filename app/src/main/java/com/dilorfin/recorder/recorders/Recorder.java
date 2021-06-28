package com.dilorfin.recorder.recorders;

import android.content.Context;
import android.content.Intent;

public abstract class Recorder {
    protected final Context context;

    public Recorder(Context context) { this.context = context; }

    public abstract void prepare();
    public abstract void start();
    public abstract void stop();
    public abstract boolean isRecording();

    public void HBRecorderOnError(int errorCode, String reason)
    { }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    { }
}
