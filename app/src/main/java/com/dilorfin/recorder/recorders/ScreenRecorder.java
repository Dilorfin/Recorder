package com.dilorfin.recorder.recorders;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;

import androidx.appcompat.app.AppCompatActivity;

import com.dilorfin.recorder.SharedValues;
import com.dilorfin.recorder.utils.Logger;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.util.Objects;

public class ScreenRecorder extends Recorder
{
    private final HBRecorder hbRecorder;

    private Intent data;
    private int resultCode;

    private static class Config
    {
        public Config(String encoder, String outputFormat){
            this.encoder = encoder;
            this.outputFormat = outputFormat;
        }
        public String encoder;
        public String outputFormat;
    }
    private final String ConfigTag = "HBConfig";
    private final Config[] configs = {
            new Config("VP8", "WEBM"),
            new Config("H263", "MPEG_4"),
            new Config("H264", "MPEG_4"),
            new Config("H264", "MPEG_2_TS"),
    };

    public ScreenRecorder(Context context)
    {
        super(context);

        hbRecorder = new HBRecorder(context, (HBRecorderListener) context);

        SharedPreferences preferences = ((AppCompatActivity)context).getPreferences(Context.MODE_PRIVATE);
        int configId = preferences.getInt(ConfigTag, 0);
        this.configureRecorder(configId);
    }

    private void configureRecorder(int configId)
    {
        if (configId > configs.length)
        {
            Logger.error("No available HBConfig could to start recording");
            return;
        }
        hbRecorder.enableCustomSettings();

        hbRecorder.isAudioEnabled(false);
        hbRecorder.setVideoEncoder(configs[configId].encoder);
        hbRecorder.setOutputFormat(configs[configId].outputFormat);
        hbRecorder.setScreenDimensions(1920, 1080);
        hbRecorder.setVideoFrameRate(30);
        hbRecorder.setVideoBitrate(12000000);
        hbRecorder.setFileName("screen");
    }

    @Override
    public void prepare()
    {
        if (this.isRecording()) { return; }

        hbRecorder.setOutputPath(SharedValues.outputPath);

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        ((Activity) context).startActivityForResult(permissionIntent, SharedValues.SCREEN_RECORD_REQUEST_CODE);
        Logger.debug("Request MediaProjection");
    }

    @Override
    public void stop()
    {
        if (!this.isRecording()) { return; }

        hbRecorder.stopScreenRecording();
        Logger.debug("Screen stop recording");
    }

    @Override
    public void start()
    {
        if (this.isRecording()) { return; }

        hbRecorder.startScreenRecording(data, resultCode, (Activity)context);
        Logger.debug("Screen start recording");
    }

    @Override
    public boolean isRecording() { return hbRecorder.isBusyRecording(); }

    @Override
    public void HBRecorderOnError(int errorCode, String reason)
    {
        if (!Objects.equals(reason, "268435556")) {
            return;
        }

        SharedPreferences preferences = ((AppCompatActivity)context).getPreferences(Context.MODE_PRIVATE);
        int configId = preferences.getInt(ConfigTag, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(ConfigTag, configId + 1);
        editor.apply();
        configId = preferences.getInt(ConfigTag, 0);

        Logger.debug("HBRecorderError: Changing HBConfig to " + configs[configId].encoder + " "+ configs[configId].outputFormat);
        this.configureRecorder(configId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SharedValues.SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK)
            {
                this.data = data;
                this.resultCode = resultCode;
                Logger.debug("MediaProjection granted");
            }
            else
            {
                Logger.debug("MediaProjection not granted");
            }
        }
    }
}
