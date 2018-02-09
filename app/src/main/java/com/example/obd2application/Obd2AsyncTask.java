package com.example.obd2application;

import android.os.AsyncTask;
import android.util.Pair;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by linkk on 2/6/2018.
 */

public abstract class Obd2AsyncTask extends AsyncTask<String, Pair<Integer, String>, String> {

    protected Obd2Activity obd2Activity;
    protected OutputStream outputStream;
    protected InputStream inputStream;

    public Obd2AsyncTask(Obd2Activity obd2Activity) {
        this.obd2Activity = obd2Activity;
//            try {
//                Socket socket = new Socket("192.168.0.10", 35000);
//                outputStream = socket.getOutputStream();
//                inputStream = socket.getInputStream();
//            } catch (Exception e) {
//                Log.e("example.app", e.getMessage());
//            }
    }

    @Override
    protected void onProgressUpdate(Pair<Integer, String>... values) {
        obd2Activity.updateProgress(values);
    }

    protected void initializeObd2() throws IOException {
        publishProgress(new Pair<Integer, String>(R.id.statusTextView, "Connecting to WiFi socket"));
        Socket socket = new Socket("192.168.0.10", 35000);
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        ObdResetCommand obdResetCommand = new ObdResetCommand();//ObdWarmstartCommand obdWarmstartCommand = new ObdWarmstartCommand();
        runCommand(obdResetCommand, R.id.statusTextView);
        SelectProtocolCommand selectProtocolCommand = new SelectProtocolCommand(ObdProtocols.AUTO);
        runCommand(selectProtocolCommand, R.id.statusTextView);
        EchoOffCommand echoOffCommand = new EchoOffCommand();
        runCommand(echoOffCommand, R.id.statusTextView);
    }

    protected String runCommand(ObdCommand obdCommand, int valueTextViewId) {
        String obdCommandFormattedResult;
        try {
            publishProgress(new Pair<Integer, String>(R.id.statusTextView, "Running " + obdCommand.getName()));
            publishProgress(new Pair<Integer, String>(R.id.commandTextView, obdCommand.getCommandPID()));
            obdCommand.run(inputStream, outputStream);
            //obdCommandResult = obdCommand.getResult();
            obdCommandFormattedResult = obdCommand.getFormattedResult();//requires turning echo off
        } catch (Exception e) {
            obdCommandFormattedResult = e.getMessage();
        }
        publishProgress(new Pair<Integer, String>(valueTextViewId, obdCommandFormattedResult));
        return obdCommandFormattedResult;
    }
}