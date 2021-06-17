package com.dilorfin.recorder.utils;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger
{
    private enum Style { Debug, Error }

    private static final String TAG = "Logger";
    private static final String filename;
    public static String outputPath;

    static
    {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
        filename = simpleDateFormat.format(new Date()) + ".txt";
    }

    public static void debug(String message)
    {
        writeLine(message, Style.Debug);
    }

    public static void error(String message)
    {
        writeLine(message, Style.Error);
    }

    public static void error(Exception exception)
    {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        writeLine(sw.toString(), Style.Error);
    }

    private static void writeLine(String message, Style style)
    {
        File dir = new File(outputPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }

        File logFile = new File(outputPath, filename);
        if (!logFile.exists())
        {
            try  {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        String composedMessage = style.name() + ": ";
        if (message == null)
        {
            composedMessage += "Message was null";
        }
        else
        {
            composedMessage += message;
        }

        if (style == Style.Error)
        {
            Log.e(TAG, composedMessage);
        }
        else
        {
            Log.d(TAG, composedMessage);
        }

        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));

            buf.write(composedMessage);

            buf.newLine();
            buf.flush();
            buf.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, e.getMessage());
        }
    }
}