package com.dilorfin.recorder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dilorfin.recorder.recorders.FrontCameraRecorder;
import com.dilorfin.recorder.recorders.Recorder;
import com.dilorfin.recorder.recorders.ScreenRecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private final String TAG = "Recorder-MainActivity";
    private Recorder[] recorders;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.grantPermissions();

        SharedValues.mSurfaceHolder = ((SurfaceView)findViewById(R.id.surfaceView)).getHolder();

        recorders = new Recorder[]{
                new FrontCameraRecorder(this),
                new ScreenRecorder(this)
        };
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

        ((ToggleButton)view).setChecked(isRecording);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        for (Recorder recorder : recorders) {
            recorder.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startRecording()
    {
        isRecording = true;
        for (Recorder recorder : recorders) {
            recorder.startRecording();
        }
    }

    private void stopRecording()
    {
        isRecording = false;
        for (Recorder recorder : recorders) {
            recorder.stopRecording();
        }
    }

    private void createFolder()
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");

        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM)+"/Recorder", simpleDateFormat.format(new Date()));
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.i(TAG, "Folder created");
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
            Log.d(TAG, "Requesting permissions");
            String[] temp = new String[permissions.size()];
            temp = permissions.toArray(temp);

            ActivityCompat.requestPermissions(this, temp, 44);
        }
    }
}