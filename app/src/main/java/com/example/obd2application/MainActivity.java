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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends Obd2Activity {

    Obd2StreamTask obd2StreamTask;
    boolean isToggleButtonOn;

    public static boolean isKnownAllAvailablePids;
    public static String wantedAvailablePidsHex;//each character is a byte indicating which PID from 0 to 255 is desired
    public static String allAvailablePidsBinary;//includes PID 00
    public static String[] allAvailablePidsHexResult;//excludes PID 00
//    public static final String ALL_AVAILABLE_PIDS_BINARY_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_BINARY_KEY";
//    public static final String ALL_AVAILABLE_PIDS_HEX_RESULT_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY";
    public static final int PIDS_ACTIVITY_REQUEST_CODE = 1;

    public static TextView[] commandTextViewArray;//8 views
    int[] commandValueTextViewIdArray;
    public static GraphView[] commandGraphViewArray;//8 views
    int[] commandGraphViewIdArray;
    LineGraphSeries<DataPoint>[] commandLineGraphSeriesArray;

    String[] pidNames;
    ObdCommand[] pidCommands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        obd2StreamTask = null;
        isToggleButtonOn = false;
        findViewById(R.id.toggleButton).setEnabled(false);//disabled until available PIDs are known

        isKnownAllAvailablePids = false;
        wantedAvailablePidsHex = "04 05 0B 0C 0D 0F 2F 46 ";
        allAvailablePidsBinary = "1";//because PID 00 is always available
        allAvailablePidsHexResult = new String[8];

        initializeViewArrays();
//        updateProgress(new Pair<Integer, String>(R.id.engineRpmValueGraphView, "801"));
        initializePidMaps();
    }

    private void initializeViewArrays() {
        commandTextViewArray = new TextView[] {
                findViewById(R.id.commandTextView0),
                findViewById(R.id.commandTextView1),
                findViewById(R.id.commandTextView2),
                findViewById(R.id.commandTextView3),
                findViewById(R.id.commandTextView4),
                findViewById(R.id.commandTextView5),
                findViewById(R.id.commandTextView6),
                findViewById(R.id.commandTextView7)
        };

        commandValueTextViewIdArray = new int[] {
                R.id.commandValueTextView0,
                R.id.commandValueTextView1,
                R.id.commandValueTextView2,
                R.id.commandValueTextView3,
                R.id.commandValueTextView4,
                R.id.commandValueTextView5,
                R.id.commandValueTextView6,
                R.id.commandValueTextView7
        };

        commandGraphViewArray = new GraphView[] {
                findViewById(R.id.commandValueGraphView0),
                findViewById(R.id.commandValueGraphView1),
                findViewById(R.id.commandValueGraphView2),
                findViewById(R.id.commandValueGraphView3),
                findViewById(R.id.commandValueGraphView4),
                findViewById(R.id.commandValueGraphView5),
                findViewById(R.id.commandValueGraphView6),
                findViewById(R.id.commandValueGraphView7)
        };

        commandGraphViewIdArray = new int[] {
                R.id.commandValueGraphView0,
                R.id.commandValueGraphView1,
                R.id.commandValueGraphView2,
                R.id.commandValueGraphView3,
                R.id.commandValueGraphView4,
                R.id.commandValueGraphView5,
                R.id.commandValueGraphView6,
                R.id.commandValueGraphView7
        };

        commandLineGraphSeriesArray = new LineGraphSeries[commandGraphViewArray.length];
        for (int i = 0; i< commandGraphViewArray.length; i++) {
            GridLabelRenderer gridLabelRenderer = commandGraphViewArray[i].getGridLabelRenderer();
            gridLabelRenderer.setPadding(80);
            commandGraphViewArray[i].getViewport().setScrollable(true);
            commandGraphViewArray[i].getViewport().setXAxisBoundsManual(true);

            commandLineGraphSeriesArray[i] = new LineGraphSeries<>();
            commandGraphViewArray[i].addSeries(commandLineGraphSeriesArray[i]);
        }
    }

    private void initializePidMaps() {
        pidNames = new String[256];
        pidCommands = new ObdCommand[256];

        pidNames[4] = "04: Calculated Engine Load";
        pidCommands[4] = new LoadCommand();
        pidNames[5] = "05: Engine Coolant Temperature";
        pidCommands[5] = new EngineCoolantTemperatureCommand();
        pidNames[11] = "0B: Intake Manifold Absolute Pressure";
        pidCommands[11] = new IntakeManifoldPressureCommand();
        pidNames[12] = "0C: Engine Rpm";
        pidCommands[12] = new RPMCommand();
        pidNames[13] = "0D: Vehicle Speed";
        pidCommands[13] = new SpeedCommand();
        pidNames[15] = "0F: Intake Air Temperature";
        pidCommands[15] = new AirIntakeTemperatureCommand();
        pidNames[47] = "2F: Fuel Tank Input Level";
        pidCommands[47] = new FuelLevelCommand();
        pidNames[70] = "46: Ambient Air Temperature";
        pidCommands[70] = new AmbientAirTemperatureCommand();

        for (int p=0; p<pidCommands.length; p++) {
            if (pidCommands[p] != null) {
                pidCommands[p].setMaxNumberResponses(1);
            }
        }
    }

//    @Override
//    public void onResume(){
//        super.onResume();
//        setTextViewToWanted();
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (PIDS_ACTIVITY_REQUEST_CODE) : {
                if (resultCode == Activity.RESULT_OK || isKnownAllAvailablePids) {
                    //this.wantedAvailablePidsHex = data.getStringExtra("WantedAvailablePidsBytes");
                    findViewById(R.id.toggleButton).setEnabled(true);//now available PIDs are known
                    setTextViewToWanted();
                }
                break;
            }
        }
    }

    private void setTextViewToWanted() {
        for (int tv = 0; tv< commandTextViewArray.length; tv++) {
            commandTextViewArray[tv].setText("NONE");
        }
        String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailablePidsHex.length() > 0 ? MainActivity.wantedAvailablePidsHex.split(" ") : new String[0];
        for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
            int wantedAvailablePidDec = Integer.parseInt(wantedAvailablePidsHexArray[wp], 16);
            commandTextViewArray[wp].setText(pidNames[wantedAvailablePidDec]);
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
        updateProgress(new Pair<Integer, String>(R.id.debugTextView, this.wantedAvailablePidsHex));
        //updateProgress(new Pair<Integer, String>(R.id.debugTextView, allAvailablePidsBinary));
        //updateProgress(new Pair<Integer, String>(R.id.commandValueGraphView0, "803"));
    }

    private class Obd2StreamTask extends Obd2AsyncTask {

        MainActivity mainActivity;

        public Obd2StreamTask(MainActivity mainActivity) {
            super(mainActivity);
            this.mainActivity = mainActivity;

        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                initializeObd2();//TODO REMOCK

//                ConsumptionRateCommand engineFuelRateCommand = new ConsumptionRateCommand();
//                engineFuelRateCommand.setMaxNumberResponses(1);
//                String engineFuelRateCommandResult = runCommand(engineFuelRateCommand, R.id.engineFuelRateValueTextView);
                while (!isCancelled()) {
                    publishProgress(new Pair<Integer, String>(R.id.statusTextView, "SLEEPING"));
                    Thread.sleep(2000 - System.currentTimeMillis()%2000);

                    String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailablePidsHex.split(" ");
                    for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
                        int wantedAvailablePidDec = Integer.parseInt(wantedAvailablePidsHexArray[wp], 16);
                        String commandResult = runCommand(pidCommands[wantedAvailablePidDec], commandValueTextViewIdArray[wp]);
                        //String commandResult = "123M";//TODO REMOCK
                        String unitlessResult = commandResult.replaceAll("[^\\d.]", "");
                        publishProgress(new Pair<Integer, String>(commandGraphViewIdArray[wp], unitlessResult));
                    }
                }
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }
    }
}
