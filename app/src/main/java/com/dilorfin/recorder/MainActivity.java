package com.dilorfin.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dilorfin.recorder.recorders.FrontCameraRecorder;
import com.dilorfin.recorder.recorders.Recorder;
import com.dilorfin.recorder.recorders.ScreenRecorder;
import com.dilorfin.recorder.utils.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener
{
    private Recorder[] recorders;

    private boolean isRecording = false;

    private ToggleButton toggleButton;
    public FrameLayout mTextureContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.outputPath = "/storage/emulated/0/Android/data/com.dilorfin.recorder/logs/";

        this.grantPermissions();

        mTextureContainer = findViewById(R.id.textureContainer);
        toggleButton = findViewById(R.id.toggleButton);
        createFolder();
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
            prepareRecording();
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

        this.startRecording();
    }

    private void prepareRecording()
    {
        Logger.debug("Preparing recorders");
        for (Recorder recorder : recorders) {
            recorder.prepare();
        }
    }

    private void startRecording()
    {
        Logger.debug("Start recording");
        for (Recorder recorder : recorders) {
            recorder.start();
        }
        isRecording = true;
        toggleButton.setChecked(true);
    }

    public void stopRecording()
    {
        for (Recorder recorder : recorders) {
            recorder.stop();
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

    public void onError(String message)
    {
        Logger.error(message);
        Toast.makeText(getApplicationContext(),
                "An error occurred. Please, try again",
                Toast.LENGTH_LONG).show();

        stopRecording();
        for (Recorder recorder : recorders) {
            recorder.onError(message);
        }
    }
}