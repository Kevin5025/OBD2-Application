package com.example.obd2application;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.ObdWarmstartCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    TextView[] textViewArray;
    Obd2Task obd2Task;
    boolean isToggleButtonOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewArray = new TextView[] {};
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

    public void updateProgress(Pair<Integer, String>... values) {
        for (int v=0; v<values.length; v++) {
            TextView textView = findViewById(values[v].first);
            textView.setText(values[v].second);
        }

        TextView currentTimeMillisValueTextView = findViewById(R.id.currentTimeMillisValueTextView);
        currentTimeMillisValueTextView.setText(String.valueOf(System.currentTimeMillis()));
    }

    private class Obd2Task extends AsyncTask<String, Pair<Integer, String>, String> {

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
            publishProgress(new Pair<Integer, String>(R.id.statusTextView, "WAIT0"));
            try {
                Socket socket = new Socket("192.168.0.10", 35000);
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                ObdResetCommand obdResetCommand = new ObdResetCommand();//ObdWarmstartCommand obdWarmstartCommand = new ObdWarmstartCommand();
                String obdResetCommandResult = runCommand(obdResetCommand, R.id.statusTextView);

                SelectProtocolCommand selectProtocolCommand = new SelectProtocolCommand(ObdProtocols.AUTO);
                String selectProtocolCommandResult = runCommand(selectProtocolCommand, R.id.statusTextView);

                EchoOffCommand echoOffCommand = new EchoOffCommand();
                String echoOffCommandResult = runCommand(echoOffCommand, R.id.statusTextView);

                runAvailablePidsCommands();

                while (!isCancelled()) {
                    //Thread.sleep(1000);

                    SpeedCommand vehicleSpeedCommand = new SpeedCommand();
                    String vehicleSpeedCommandResult = runCommand(vehicleSpeedCommand, R.id.vehicleSpeedValueTextView);

                    RPMCommand engineRpmCommand = new RPMCommand();
                    String engineRPMCommandResult = runCommand(engineRpmCommand, R.id.engineRpmValueTextView);

                    EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = new EngineCoolantTemperatureCommand();
                    String engineCoolantTemperatureCommandResult = runCommand(engineCoolantTemperatureCommand, R.id.engineCoolantTemperatureValueTextView);

                    FuelLevelCommand fuelTankInputLevelCommand = new FuelLevelCommand();
                    String fuelTankInputLevelCommandResult = runCommand(fuelTankInputLevelCommand, R.id.fuelTankLevelInputValueTextView);

                    ConsumptionRateCommand engineFuelRateCommand = new ConsumptionRateCommand();
                    String engineFuelRateCommandResult = runCommand(engineFuelRateCommand, R.id.engineFuelRateValueTextView);
                }
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

        private void runAvailablePidsCommands() {
            ObdCommand[] availablePidsCommands = new ObdCommand[] {
                    //new AvailablePidsCommand_01_20(),
                    //new AvailablePidsCommand_21_40(),
                    //new AvailablePidsCommand_41_60(),
                    new ObdRawCommand("01 00"),
                    new ObdRawCommand("01 20"),
                    new ObdRawCommand("01 40"),
                    new ObdRawCommand("01 60"),
                    new ObdRawCommand("01 80"),
                    new ObdRawCommand("01 A0"),
                    new ObdRawCommand("01 C0"),
                    new ObdRawCommand("01 E0"),
            };
            Integer[] valueTextViewIds = new Integer[] {
                    R.id.pidsSupported_01_20_ValuesTextView,
                    R.id.pidsSupported_21_40_ValuesTextView,
                    R.id.pidsSupported_41_60_ValuesTextView,
                    R.id.pidsSupported_61_80_ValuesTextView,
                    R.id.pidsSupported_81_A0_ValuesTextView,
                    R.id.pidsSupported_A1_C0_ValuesTextView,
                    R.id.pidsSupported_C1_E0_ValuesTextView,
                    R.id.pidsSupported_E1_00_ValuesTextView
            };

            for (int b=0; b<availablePidsCommands.length; b++) {
                String obdCommandResult = runCommand(availablePidsCommands[b], valueTextViewIds[b]);
                String obdCommandResultBinary = new BigInteger(obdCommandResult.substring(obdCommandResult.length() - 8), 16).toString(2);
                String obdCommandResultBinaryLeadingZeros = ("00000000000000000000000000000000" + obdCommandResultBinary).substring(obdCommandResultBinary.length());
                String obdCommandResult2Binary = new BigInteger(obdCommandResult.substring(4, 12), 16).toString(2);//sometimes the obd2 scanner returns two sets of results (concatenated) for some reason
                String obdCommandResult2BinaryLeadingZeros = ("00000000000000000000000000000000" + obdCommandResult2Binary).substring(obdCommandResult2Binary.length());
                if (obdCommandResultBinaryLeadingZeros.charAt(31) == '0' && obdCommandResult2BinaryLeadingZeros.charAt(31) == '0') {
                    break;
                }
            }
        }

        private String runCommand(ObdCommand obdCommand, int valueTextViewId) {
            String obdCommandResult;
            try {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, "Running " + obdCommand.getName()));
                obdCommand.run(inputStream, outputStream);
                //obdCommandResult = obdCommand.getResult();
                obdCommandResult = obdCommand.getFormattedResult();//requires turning echo off
            } catch (Exception e) {
                obdCommandResult = e.getMessage();
            }
            publishProgress(new Pair<Integer, String>(valueTextViewId, obdCommandResult));
            return obdCommandResult;
        }

        @Override
        protected void onProgressUpdate(Pair<Integer, String>... values) {
            mainActivity.updateProgress(values);
        }
    }
}
