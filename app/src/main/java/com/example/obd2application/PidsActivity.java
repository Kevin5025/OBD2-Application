package com.example.obd2application;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

public class PidsActivity extends Obd2Activity {

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

    Integer[] commandCheckBoxIds = new Integer[] {
            R.id.calculatedEngineLoadCheckBox,
            R.id.engineCoolantTemperatureCheckBox,
            R.id.intakeManifoldAbsolutePressureCheckBox,
            R.id.engineRpmCheckBox,
            R.id.vehicleSpeedCheckBox,
            R.id.intakeAirTemperatureCheckBox,
            R.id.fuelTankInputLevelCheckBox,
            R.id.ambientAirTemperatureCheckBox
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pids);

        Intent intent = getIntent();
//        String allAvailablePidsBinary = intent.getStringExtra(MainActivity.ALL_AVAILABLE_PIDS_BINARY_KEY);
//        String[] allAvailablePidsHexResult = intent.getStringArrayExtra(MainActivity.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY);

        if (!MainActivity.isKnownAllAvailablePids) {
            Obd2AvailablePidsTask obd2AvailablePidsTask = new Obd2AvailablePidsTask(this);
            obd2AvailablePidsTask.execute();
            //also see OnPostExecute function overrided in Obd2AvailablePidsTask class
            MainActivity.isKnownAllAvailablePids = true;
            Intent resultIntent = new Intent();
            //resultIntent.putExtra("WantedAvailablePidsBytes", getWantedAvailablePidsHex());
            //MainActivity.wantedAvailablePidsHex = getWantedAvailablePidsHex();
            setResult(Activity.RESULT_OK, resultIntent);
            //finish();
        } else {
            for (int b=0; b<MainActivity.allAvailablePidsHexResult.length; b++) {
                updateProgress(new Pair<Integer, String>(pidsSupportedValueTextViewIds[b], MainActivity.allAvailablePidsHexResult[b]));
            }
            disableUnavailablePidCommandCheckBoxes();
            checkWantedCommandCheckBoxes();
        }
    }

    public void onCommandCheckBoxClick(View view) {
        MainActivity.wantedAvailablePidsHex = getWantedAvailablePidsHex();
    }

    private void pruneWantedAvailablePidsHex() {
        StringBuilder wantedAvailablePidsHexStringBuilder = new StringBuilder();
        String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailablePidsHex.split(" ");
        for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
            int wantedAvailablePidDec = Integer.parseInt(wantedAvailablePidsHexArray[wp], 16);
            if (wantedAvailablePidDec < MainActivity.allAvailablePidsBinary.length() && MainActivity.allAvailablePidsBinary.charAt(wantedAvailablePidDec) == '1') {
                wantedAvailablePidsHexStringBuilder.append(wantedAvailablePidsHexArray[wp]);
                wantedAvailablePidsHexStringBuilder.append(" ");
            }
        }
        MainActivity.wantedAvailablePidsHex = wantedAvailablePidsHexStringBuilder.toString();
    }

    private void disableUnavailablePidCommandCheckBoxes() {
        for (int c=0; c<commandCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(commandCheckBoxIds[c]);
            String checkBoxPidHex = checkBox.getText().toString().substring(0, 2);
            int checkBoxPidDec = Integer.parseInt(checkBoxPidHex, 16);
            if (checkBoxPidDec < MainActivity.allAvailablePidsBinary.length() && MainActivity.allAvailablePidsBinary.charAt(checkBoxPidDec) == '1') {
                checkBox.setEnabled(true);
            } else {
                checkBox.setEnabled(false);
            }
        }
    }

    private void checkWantedCommandCheckBoxes() {
        for (int c=0; c<commandCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(commandCheckBoxIds[c]);
            String checkBoxPidHex = checkBox.getText().toString().substring(0, 2);
            int checkBoxPidDec = Integer.parseInt(checkBoxPidHex, 16);
            if (checkBoxPidDec < MainActivity.allAvailablePidsBinary.length() && MainActivity.allAvailablePidsBinary.charAt(checkBoxPidDec) == '1') {
                String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailablePidsHex.split(" ");
                //using a linear search for easy implementation, but a log search would work too:
                for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
                    if (wantedAvailablePidsHexArray[wp].equalsIgnoreCase(checkBoxPidHex)) {
                        checkBox.setChecked(true);
                        break;
                    }
                }
            }
        }
    }

    private String getWantedAvailablePidsHex(){
        StringBuilder wantedAvailablePidsBytesStringBuilder = new StringBuilder();
        for (int c=0; c<commandCheckBoxIds.length; c++) {
            CheckBox checkBox = findViewById(commandCheckBoxIds[c]);
            if (checkBox.isChecked()) {
                String checkBoxPidHex = checkBox.getText().toString().substring(0, 2);
                wantedAvailablePidsBytesStringBuilder.append(checkBoxPidHex);
                wantedAvailablePidsBytesStringBuilder.append(" ");
            }
        }
        String wantedAvailablePidsBytes = wantedAvailablePidsBytesStringBuilder.toString();
        return wantedAvailablePidsBytes;
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
                initializeObd2();//TODO REMOCK
                runAvailablePidsCommands();
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

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
                //String obdCommandResults = "0000AAAAAAAA";//TODO REMOCK
                BigInteger availablePids = getAvailablePids(obdCommandResults);

                String availablePidsBinary = availablePids.toString(2);
                String availablePidsBinaryLeadingZeros = ("00000000000000000000000000000000" + availablePidsBinary).substring(availablePidsBinary.length());
                MainActivity.allAvailablePidsBinary += availablePidsBinaryLeadingZeros;
                String availablePidsHexResult = availablePids.toString(16);
                String availablePidsHexResultLeadingZeros = ("00000000" + availablePidsHexResult).substring(availablePidsHexResult.length()).toUpperCase();
                MainActivity.allAvailablePidsHexResult[b] = availablePidsHexResultLeadingZeros;
                publishProgress(new Pair<Integer, String>(pidsSupportedValueTextViewIds[b], availablePidsHexResultLeadingZeros));

                if (availablePids.mod(new BigInteger("2")).equals(BigInteger.ZERO)) {//if even, then no ECU's support any more
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
            pruneWantedAvailablePidsHex();
            disableUnavailablePidCommandCheckBoxes();
            checkWantedCommandCheckBoxes();
        }
    }
}
