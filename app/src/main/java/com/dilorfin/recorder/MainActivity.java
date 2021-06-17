package com.dilorfin.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dilorfin.recorder.recorders.FrontCameraRecorder;
import com.dilorfin.recorder.recorders.Recorder;
import com.dilorfin.recorder.recorders.ScreenRecorder;
import com.dilorfin.recorder.utils.Logger;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, HBRecorderListener
{
    private Recorder[] recorders;

    private boolean isRecording = false;

    ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.outputPath = "/storage/emulated/0/Android/data/com.dilorfin.recorder/files/";

        this.grantPermissions();

        SharedValues.mSurfaceHolder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();
        toggleButton = findViewById(R.id.toggleButton);

        recorders = new Recorder[]{
                new FrontCameraRecorder(this),
                new ScreenRecorder(this)
        };
    }

    @Override
    protected void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    @Override
    public void onClick(View view)
    {
        if (!isRecording)
        {
            createFolder();
            startRecording();
        }
        else
        {
            stopRecording();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        for (Recorder recorder : recorders)
        {
            recorder.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startRecording()
    {
        for (Recorder recorder : recorders) {
            recorder.startRecording();
        }
        isRecording = true;
        toggleButton.setChecked(true);
    }

    private void stopRecording()
    {
        for (Recorder recorder : recorders) {
            recorder.stopRecording();
        }
        isRecording = false;
        toggleButton.setChecked(false);
    }

    private void createFolder()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");

        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM)+"/Recorder", simpleDateFormat.format(new Date()));
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Logger.debug("Folder created");
            }
        }
        SharedValues.outputPath = folder.getAbsolutePath();
    }

    private void grantPermissions()
    {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        boolean hasAllPermissions = true;
        for(String permission : permissions)
        {
            if(ActivityCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_DENIED)
            {
                hasAllPermissions = false;
            }
        }

        if (!hasAllPermissions)
        {
            Logger.debug("Requesting permissions");
            String[] temp = new String[permissions.size()];
            temp = permissions.toArray(temp);

            ActivityCompat.requestPermissions(this, temp, 44);
        }
    }

    @Override
    public void HBRecorderOnStart() { }

    @Override
    public void HBRecorderOnComplete() { stopRecording(); }

    @Override
    public void HBRecorderOnError(int errorCode, String reason)
    {
        if (errorCode == MAX_FILE_SIZE_REACHED_ERROR)
        {
            Logger.error("HBRecorder max Size reached (" + reason + ")");
        }
        else if (errorCode == SETTINGS_ERROR)
        {
            // Error 38 happens when
            // - the selected video encoder is not supported
            // - the output format is not supported
            // - if another app is using the microphone

            Logger.error("HBRecorder settings Error (" + reason + ")");
        }
        else
        {
            Logger.error("HBRecorder " + errorCode + " (" + reason + ")");
        }
    }
}