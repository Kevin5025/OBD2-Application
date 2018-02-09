package com.example.obd2application;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;

import java.util.ArrayList;

public class MainActivity extends Obd2Activity {

    TextView[] textViewArray;
    Obd2StreamTask obd2StreamTask;
    boolean isToggleButtonOn;

    public static String allAvailablePidsBinary;//includes PID 00
    public static String[] allAvailablePidsHexResult;//excludes PID 00
    public static boolean allAvailablePidsKnown;
//    public static final String ALL_AVAILABLE_PIDS_BINARY_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_BINARY_KEY";
//    public static final String ALL_AVAILABLE_PIDS_HEX_RESULT_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY";
    public static final int PIDS_ACTIVITY_REQUEST_CODE = 1;

    ArrayList<Integer> obdCommandTextViewIdArrayList = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewArray = new TextView[] {};
        obd2StreamTask = null;
        isToggleButtonOn = false;
        allAvailablePidsBinary = "1";//because PID 00 is always available
        allAvailablePidsHexResult = new String[8];
        allAvailablePidsKnown = false;

        findViewById(R.id.toggleButton).setEnabled(false);//until available PIDs are known
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (PIDS_ACTIVITY_REQUEST_CODE) : {
                if (resultCode == Activity.RESULT_OK && allAvailablePidsKnown) {
                    //e.g. String returnValue = data.getStringExtra("some_key");
                    findViewById(R.id.toggleButton).setEnabled(true);//now available PIDs are known
                }
                break;
            }
        }
    }

    public void onPidsButtonClick(View view) {
        Intent intent = new Intent(this, PidsActivity.class);
//        intent.putExtra(ALL_AVAILABLE_PIDS_BINARY_KEY, allAvailablePidsBinary);
//        intent.putExtra(ALL_AVAILABLE_PIDS_HEX_RESULT_KEY, allAvailablePidsHexResult);
        startActivityForResult(intent, PIDS_ACTIVITY_REQUEST_CODE);
    }

    public void onToggleButtonClick(View view) {
        isToggleButtonOn = !isToggleButtonOn;
        if (isToggleButtonOn) {
            Toast.makeText(this, "Toggle Is On", Toast.LENGTH_SHORT).show();
            obd2StreamTask = new Obd2StreamTask(this);
            obd2StreamTask.execute();
        } else {
            Toast.makeText(this, "Toggle Is Off", Toast.LENGTH_SHORT).show();
            obd2StreamTask.cancel(false);
            obd2StreamTask = null;
        }
    }

    public void onDebugButtonClick(View view) {
        updateProgress(new Pair<Integer, String>(R.id.debugTextViewB, allAvailablePidsBinary));
    }

    private class Obd2StreamTask extends Obd2AsyncTask {

        public Obd2StreamTask(Obd2Activity obd2Activity) {
            super(obd2Activity);
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                initializeObd2();

                ArrayList<ObdCommand> obdCommands = getAvailableCommands();
//                ConsumptionRateCommand engineFuelRateCommand = new ConsumptionRateCommand();
//                engineFuelRateCommand.setMaxNumberResponses(1);
                while (!isCancelled()) {
                    publishProgress(new Pair<Integer, String>(R.id.statusTextView, "SLEEPING"));
                    Thread.sleep(2000 - System.currentTimeMillis()%2000);
                    for (int oc=0; oc<obdCommands.size(); oc++) {
                        runCommand(obdCommands.get(oc), obdCommandTextViewIdArrayList.get(oc));
                    }
//                    String engineFuelRateCommandResult = runCommand(engineFuelRateCommand, R.id.engineFuelRateValueTextView);
                }
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

        protected ArrayList<ObdCommand> getAvailableCommands() {
            ArrayList<ObdCommand> obdCommands = new ArrayList<>();

            //Eventually COULDDO: dynamically generate the TextViews
            //Eventually COULDDO: map the PIDs to ObdCommands
            if (MainActivity.allAvailablePidsBinary.length() >= 5 && MainActivity.allAvailablePidsBinary.charAt(4) == '1') {
                LoadCommand calculatedEngineLoadCommand = new LoadCommand();
                calculatedEngineLoadCommand.setMaxNumberResponses(1);
                obdCommands.add(calculatedEngineLoadCommand);
                obdCommandTextViewIdArrayList.add(R.id.calculatedEngineLoadValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 6 && MainActivity.allAvailablePidsBinary.charAt(5) == '1') {
                EngineCoolantTemperatureCommand engineCoolantTemperatureCommand = new EngineCoolantTemperatureCommand();
                engineCoolantTemperatureCommand.setMaxNumberResponses(1);
                obdCommands.add(engineCoolantTemperatureCommand);
                obdCommandTextViewIdArrayList.add(R.id.engineCoolantTemperatureValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 12 && MainActivity.allAvailablePidsBinary.charAt(11) == '1') {
                IntakeManifoldPressureCommand intakeManifoldAbsolutePressureCommand = new IntakeManifoldPressureCommand();
                intakeManifoldAbsolutePressureCommand.setMaxNumberResponses(1);
                obdCommands.add(intakeManifoldAbsolutePressureCommand);
                obdCommandTextViewIdArrayList.add(R.id.intakeManifoldAbsolutePressureValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 13 && MainActivity.allAvailablePidsBinary.charAt(12) == '1') {
                RPMCommand engineRpmCommand = new RPMCommand();
                engineRpmCommand.setMaxNumberResponses(1);
                obdCommands.add(engineRpmCommand);
                obdCommandTextViewIdArrayList.add(R.id.engineRpmValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 14 && MainActivity.allAvailablePidsBinary.charAt(13) == '1') {
                SpeedCommand vehicleSpeedCommand = new SpeedCommand();
                vehicleSpeedCommand.setMaxNumberResponses(1);
                obdCommands.add(vehicleSpeedCommand);
                obdCommandTextViewIdArrayList.add(R.id.vehicleSpeedValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 16 && MainActivity.allAvailablePidsBinary.charAt(15) == '1') {
                AirIntakeTemperatureCommand intakeAirTemperatureCommand = new AirIntakeTemperatureCommand();
                intakeAirTemperatureCommand.setMaxNumberResponses(1);
                obdCommands.add(intakeAirTemperatureCommand);
                obdCommandTextViewIdArrayList.add(R.id.intakeAirTemperatureValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 48 && MainActivity.allAvailablePidsBinary.charAt(47) == '1') {
                FuelLevelCommand fuelTankInputLevelCommand = new FuelLevelCommand();
                fuelTankInputLevelCommand.setMaxNumberResponses(1);
                obdCommands.add(fuelTankInputLevelCommand);
                obdCommandTextViewIdArrayList.add(R.id.fuelTankLevelInputValueTextView);
            }
            if (MainActivity.allAvailablePidsBinary.length() >= 71 && MainActivity.allAvailablePidsBinary.charAt(70) == '1') {
                AmbientAirTemperatureCommand ambientAirTemperatureCommand = new AmbientAirTemperatureCommand();
                ambientAirTemperatureCommand.setMaxNumberResponses(1);
                obdCommands.add(ambientAirTemperatureCommand);
                obdCommandTextViewIdArrayList.add(R.id.ambientAirTemperatureValueTextView);
            }

            return obdCommands;
        }
    }
}
