package com.example.obd2application;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;

import java.math.BigInteger;

public class PidsActivity extends Obd2Activity {

    /**
     * these are the 8 "00000000" textview lines in the PID screen
     */
    Integer[] pidsSupportedValueTextViewIds = new Integer[] {
            R.id.pidsSupported_01_20_ValueTextView,
            R.id.pidsSupported_21_40_ValueTextView,
            R.id.pidsSupported_41_60_ValueTextView,
            R.id.pidsSupported_61_80_ValueTextView,
            R.id.pidsSupported_81_A0_ValueTextView,
            R.id.pidsSupported_A1_C0_ValueTextView,
            R.id.pidsSupported_C1_E0_ValueTextView,
            R.id.pidsSupported_E1_00_ValueTextView
    };

    Integer[] parameterCheckBoxIds = new Integer[] {//EVERY NEW PARAMETER TODO
            R.id.calculatedEngineLoadCheckBox,
            R.id.engineCoolantTemperatureCheckBox,
            R.id.shortTermFuelTrimBank1CheckBox,
            R.id.fuelPressureCheckBox,
            R.id.intakeManifoldAbsolutePressureCheckBox,
            R.id.engineRpmCheckBox,
            R.id.vehicleSpeedCheckBox,
            R.id.timingAdvanceCheckBox,
            R.id.intakeAirTemperatureCheckBox,
            R.id.airFlowRateCheckBox,
            R.id.throttlePositionCheckBox,
            R.id.runTimeSinceEngineStartCheckBox,
            R.id.fuelRailGaugePressureCheckBox,
            R.id.fuelTankInputLevelCheckBox,
            R.id.distanceTraveledSinceCodesClearedCheckBox,
            R.id.absoluteBarometricPressureCheckBox,
            R.id.controlModuleVoltageCheckBox,
            R.id.absoluteLoadValueCheckBox,
            R.id.fuelAirCommandedEquivalenceRatioCheckBox,
            R.id.ambientAirTemperatureCheckBox,
            R.id.engineFuelRateCheckBox,

            R.id.estimatedEngineFuelRateCheckBox,
            R.id.estimatedFuelEfficiencyCheckBox
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pids);

        Intent intent = getIntent();
//        String allAvailablePidsBinary = intent.getStringExtra(MainActivity.ALL_AVAILABLE_PIDS_BINARY_KEY);
//        String[] allAvailablePidsHexResult = intent.getStringArrayExtra(MainActivity.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY);

        /**
         * only runs the obd2AvailablePidsTask if we don't already know the available pids
         */
        if (!MainActivity.isKnownAllAvailablePids) {
            Obd2AvailablePidsTask obd2AvailablePidsTask = new Obd2AvailablePidsTask(this);
            obd2AvailablePidsTask.execute();
            //also see OnPostExecute function overrided in Obd2AvailablePidsTask class
            MainActivity.isKnownAllAvailablePids = true;
            Intent resultIntent = new Intent();
            //resultIntent.putExtra("WantedAvailablePidsBytes", getWantedAvailableParametersHex());
            //MainActivity.wantedAvailableParametersHex = getWantedAvailableParametersHex();
            setResult(Activity.RESULT_OK, resultIntent);
            //finish();
        } else {
            for (int b=0; b<MainActivity.allAvailablePidsHexResult.length; b++) {
                updateProgress(new Pair<Integer, String>(pidsSupportedValueTextViewIds[b], MainActivity.allAvailablePidsHexResult[b]));
            }
            initializeCheckBoxes();
        }
    }

    /**
     * Checks the boxes that were checked last time or the default if this is the first time.
     */
    private void initializeCheckBoxes() {
        StringBuilder wantedAvailablePidsHexStringBuilder = new StringBuilder();
        String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailableParametersHex.split(" ");
        for (int c = 0; c< parameterCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(parameterCheckBoxIds[c]);
            String checkBoxText = checkBox.getText().toString();
            boolean isCustomParameterCheckBox = checkBoxText.charAt(0) == 'Z';

            String checkBoxPidHex;
            if (isCustomParameterCheckBox) {
                int colonIndex = checkBoxText.indexOf(':');
                checkBoxPidHex = checkBoxText.substring(0, colonIndex);
            } else {
                checkBoxPidHex = checkBoxText.substring(0, 2);
            }

            boolean isAvailablePid = false;
            if (isCustomParameterCheckBox) {//EVERY NEW CUSTOM PARAMETER TODO
                if (checkBoxPidHex.equals("Z0")) {
                    isAvailablePid = MainActivity.isAvailablePidHex("10");
                } else if (checkBoxPidHex.equals("Z1")) {
                    isAvailablePid = MainActivity.isAvailablePidHex("10") && MainActivity.isAvailablePidHex("0D");
                }
            } else {
                isAvailablePid = MainActivity.isAvailablePidHex(checkBoxPidHex);
            }

            if (isAvailablePid) {
                checkBox.setEnabled(true);
                for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
                    if (wantedAvailablePidsHexArray[wp].equalsIgnoreCase(checkBoxPidHex)) {
                        checkBox.setChecked(true);
                        wantedAvailablePidsHexStringBuilder.append(checkBoxPidHex);
                        wantedAvailablePidsHexStringBuilder.append(" ");
                        break;
                    }
                }
            } else {
                checkBox.setEnabled(false);
            }
        }
        MainActivity.wantedAvailableParametersHex = wantedAvailablePidsHexStringBuilder.toString();
    }

    public void onParameterCheckBoxClick(View view) {
        if (getNumCheckBoxesChecked() > 8) {
            CheckBox checkBox = (CheckBox) view;
            checkBox.setChecked(false);
            Toast.makeText(this, "Can't have more than 8 monitors selected. ", Toast.LENGTH_LONG).show();
        } else {
            MainActivity.wantedAvailableParametersHex = getWantedAvailableParametersHex();
        }
    }

    private int getNumCheckBoxesChecked() {
        int numCheckBoxesChecked = 0;
        for (int c = 0; c< parameterCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(parameterCheckBoxIds[c]);
            if (checkBox.isChecked()) {
                numCheckBoxesChecked++;
            }
        }
        return numCheckBoxesChecked;
    }

    private String getWantedAvailableParametersHex(){
        StringBuilder wantedAvailableParametersHexStringBuilder = new StringBuilder();
        for (int c = 0; c< parameterCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(parameterCheckBoxIds[c]);
            if (checkBox.isChecked()) {
                String checkBoxPidHex = checkBox.getText().toString().substring(0, 2);
                wantedAvailableParametersHexStringBuilder.append(checkBoxPidHex);
                wantedAvailableParametersHexStringBuilder.append(" ");
            }
        }
        String wantedAvailableParametersHex = wantedAvailableParametersHexStringBuilder.toString();
        return wantedAvailableParametersHex;
    }

    private class Obd2AvailablePidsTask extends Obd2AsyncTask {

        PidsActivity pidsActivity;

        public Obd2AvailablePidsTask(PidsActivity pidsActivity) {
            super(pidsActivity);
            this.pidsActivity = pidsActivity;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                initializeObd2();//REMOCK BY COMMENTING
                runAvailablePidsCommands();
                deinitializeObd2();//REMOCK BY COMMENTING
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

        /**
         * runs the up to 8 commands to check which pids are available in this vehicle
         * stores the pid availability into MainActivity.allAvailablePidsBinary and MainActivity.allAvailablePidsHexResult
         * @throws InterruptedException
         */
        private void runAvailablePidsCommands() throws InterruptedException {
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

            MainActivity.allAvailablePidsBinary = "1";
            MainActivity.allAvailablePidsHexResult = new String[8];
            for (int b=0; b<availablePidsCommands.length; b++) {
                String obdCommandResults = runCommand(availablePidsCommands[b], pidsSupportedValueTextViewIds[b]);
                //String obdCommandResults = "0000AAAAAAAA";//REMOCK BY UNCOMMENTING AND COMMENTING ABOVE LINE
                BigInteger availablePids = getAvailablePids(obdCommandResults);

                String availablePidsBinary = availablePids.toString(2);
                String availablePidsBinaryLeadingZeros = ("00000000000000000000000000000000" + availablePidsBinary).substring(availablePidsBinary.length());
                MainActivity.allAvailablePidsBinary += availablePidsBinaryLeadingZeros;
                String availablePidsHexResult = availablePids.toString(16);
                String availablePidsHexResultLeadingZeros = ("00000000" + availablePidsHexResult).substring(availablePidsHexResult.length()).toUpperCase();
                MainActivity.allAvailablePidsHexResult[b] = availablePidsHexResultLeadingZeros;
                publishProgress(new Pair<Integer, String>(pidsSupportedValueTextViewIds[b], availablePidsHexResultLeadingZeros));

                if (availablePids.mod(new BigInteger("2")).equals(BigInteger.ZERO)) {//if even, then last bit is 0, then no ECU's support any more
                    break;
                }
            }

            publishProgress(new Pair<Integer, String>(R.id.statusTextView, "DONE"));
        }

        /**
         * availablePidsCommandResults may contain results from multiple ECU's.
         * This function combines all of such results by doing bit-wise-or across all results
         * @param availablePidsCommandResults
         * @return
         */
        private BigInteger getAvailablePids(String availablePidsCommandResults) {
            String[] obdCommandResultArray = availablePidsCommandResults.replaceAll("[\\s.]","").split("(?<=\\G.{12})");//because possible for multiple ECU's to respond to the command
            BigInteger availablePids = new BigInteger("0");//bit-wise or of all ECUs' results
            for (int i=0; i<obdCommandResultArray.length; i++) {
                String obdCommandResultWithoutCommand = obdCommandResultArray[i].substring(4);
                BigInteger obdCommandResultBigInteger = new BigInteger(obdCommandResultWithoutCommand, 16);
                availablePids = availablePids.or(obdCommandResultBigInteger);
            }
            return availablePids;
        }

        @Override
        protected void onPostExecute(String result) {
            initializeCheckBoxes();
        }
    }
}
