package com.dilorfin.recorder.utils;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
    private static final String TAG = "Logger";
    private static final String filename;
    public static String outputPath;

    static {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
        filename = simpleDateFormat.format(new Date()) + ".txt";
    }

    public static void writeLine(String message) {

        File dir = new File(outputPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }

        File logFile = new File(outputPath, filename);
        if (!logFile.exists())  {
            try  {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));

            buf.write(message);
            buf.newLine();
            buf.flush();
            buf.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}