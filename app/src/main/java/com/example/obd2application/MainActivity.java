package com.example.obd2application;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.ObdWarmstartCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    Obd2Task obd2Task;
    boolean isToggleButtonOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obd2Task = null;
        isToggleButtonOn = false;
    }

    public void onToggleButtonClick(View view) {
        isToggleButtonOn = !isToggleButtonOn;
        if (isToggleButtonOn) {
            Toast.makeText(this, "Toggle Is On", Toast.LENGTH_SHORT).show();
            obd2Task = new Obd2Task(this);
            obd2Task.execute();
        } else {
            Toast.makeText(this, "Toggle Is Off", Toast.LENGTH_SHORT).show();
            obd2Task.cancel(false);
            obd2Task = null;
        }
    }

    public void updateProgress(String... values) {
        TextView statusTextView = findViewById(R.id.statusTextView);
        statusTextView.setText(values[0]);

        TextView vehicleSpeedValueTextView = findViewById(R.id.vehicleSpeedValueTextView);
        vehicleSpeedValueTextView.setText(values[1]);

        TextView engineRpmValueTextView = findViewById(R.id.engineRpmValueTextView);
        engineRpmValueTextView.setText(values[2]);

        TextView engineCoolantTemperatureValueTextView = findViewById(R.id.engineCoolantTemperatureValueTextView);
        engineCoolantTemperatureValueTextView.setText(values[3]);

        TextView currentTimeMillisValueTextView = findViewById(R.id.currentTimeMillisValueTextView);
        currentTimeMillisValueTextView.setText(values[4]);
    }

    private class Obd2Task extends AsyncTask<String, String, String> {

        MainActivity mainActivity;
        private OutputStream outputStream;
        private InputStream inputStream;

        public Obd2Task(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
//            try {
//                Socket socket = new Socket("192.168.0.10", 35000);
//                outputStream = socket.getOutputStream();
//                inputStream = socket.getInputStream();
//            } catch (Exception e) {
//                Log.e("example.app", e.getMessage());
//            }
        }

        @Override
        protected String doInBackground(String... strings) {
            publishProgress("STATUS", "wait", "wait", "wait", String.valueOf(System.currentTimeMillis()));
            try {
                Socket socket = new Socket("192.168.0.10", 35000);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                ObdResetCommand obdResetCommand = new ObdResetCommand();//ObdWarmstartCommand obdWarmstartCommand = new ObdWarmstartCommand();
                String obdResetCommandResult = runCommand(obdResetCommand);
                publishProgress(obdResetCommandResult, "wait1", "wait1", "wait1", String.valueOf(System.currentTimeMillis()));
                Thread.sleep(250);

                SelectProtocolCommand selectProtocolCommand = new SelectProtocolCommand(ObdProtocols.AUTO);
                String selectProtocolCommandResult = runCommand(selectProtocolCommand);
                publishProgress(selectProtocolCommandResult, "wait2", "wait2", "wait2", String.valueOf(System.currentTimeMillis()));
                Thread.sleep(250);

                EchoOffCommand echoOffCommand = new EchoOffCommand();
                String echoOffCommandResult = runCommand(echoOffCommand);
                publishProgress(selectProtocolCommandResult, "wait3", "wait3", "wait3", String.valueOf(System.currentTimeMillis()));

//                AvailablePidsCommand_01_20 availablePidsCommand_01_20 = new AvailablePidsCommand_01_20();
//                String availablePidsCommand_01_20Result = runCommand(availablePidsCommand_01_20);
//                publishProgress(availablePidsCommand_01_20Result, "wait3", "wait3", "wait3", String.valueOf(System.currentTimeMillis()));
//                Thread.sleep(250);

                while (!isCancelled()) {
                    Thread.sleep(1000);

                    SpeedCommand vehicleSpeedCommand = new SpeedCommand();
                    String vehicleSpeedCommandResult = runCommand(vehicleSpeedCommand);

                    RPMCommand engineRpmCommand = new RPMCommand();
                    String engineRPMCommandResult = runCommand(engineRpmCommand);

                    EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = new EngineCoolantTemperatureCommand();
                    String engineCoolantTemperatureCommandResult = runCommand(engineCoolantTemperatureCommand);

                    publishProgress("STATUS", vehicleSpeedCommandResult, engineRPMCommandResult, engineCoolantTemperatureCommandResult, String.valueOf(System.currentTimeMillis()));
                    //publishProgress("STATUS", "testing 0", "testing 1", "testing 2", String.valueOf(System.currentTimeMillis()));
                    //publishProgress("STATUS", "wait", "wait", engineCoolantTemperatureCommandResult, String.valueOf(System.currentTimeMillis()));
                }
            } catch (Exception e) {
                publishProgress("EXCEPTION", "EXCEPTION", "EXCEPTION", "EXCEPTION", String.valueOf(System.currentTimeMillis()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

        private String runCommand(ObdCommand obdCommand) {
            String obdCommandResult;
            try {
                obdCommand.run(inputStream, outputStream);
                //obdCommandResult = obdCommand.getResult();
                obdCommandResult = obdCommand.getFormattedResult();//requires turning echo off
            } catch (Exception e) {
                obdCommandResult = e.getMessage();
            }
            return obdCommandResult;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            mainActivity.updateProgress(values);
        }
    }
}
