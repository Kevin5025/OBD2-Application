package com.example.obd2application;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends Obd2Activity {

    Obd2StreamTask obd2StreamTask;
    boolean isToggleButtonOn;

    public static boolean isKnownAllAvailablePids;
    public static String wantedAvailableParametersHex;//each character is a byte indicating which PID from 0 to 255 is desired
    public static String allAvailablePidsBinary;//includes PID 00
    public static String[] allAvailablePidsHexResult;//excludes PID 00
//    public static final String ALL_AVAILABLE_PIDS_BINARY_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_BINARY_KEY";
//    public static final String ALL_AVAILABLE_PIDS_HEX_RESULT_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY";
    public static final int PIDS_ACTIVITY_REQUEST_CODE = 1;

    public static TextView[] parameterTextViewArray;//8 views
    int[] parameterValueTextViewIdArray;
    public static GraphView[] parameterGraphViewArray;//8 views
    int[] parameterGraphViewIdArray;
    LineGraphSeries<DataPoint>[] parameterLineGraphSeriesArray;

    String[] customParameterNames;
    String[] pidNames;
    ObdCommand[] pidCommands;
    String[] memoizedPidCommandResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        obd2StreamTask = null;
        isToggleButtonOn = false;
        findViewById(R.id.toggleButton).setEnabled(false);//disabled until available PIDs are known

        isKnownAllAvailablePids = false;
        wantedAvailableParametersHex = "04 05 0C 0D 2F Z0 Z1";
        allAvailablePidsBinary = "1";//because PID 00 is always available
        allAvailablePidsHexResult = new String[8];

        initializeViewArrays();
//        updateProgress(new Pair<Integer, String>(R.id.engineRpmValueGraphView, "801"));
        initializePidMaps();

        usingBluetoothConnection = false;
        connectedBlueToothDevice = null;
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    private void initializeViewArrays() {
        parameterTextViewArray = new TextView[] {
                findViewById(R.id.parameterTextView0),
                findViewById(R.id.parameterTextView1),
                findViewById(R.id.parameterTextView2),
                findViewById(R.id.parameterTextView3),
                findViewById(R.id.parameterTextView4),
                findViewById(R.id.parameterTextView5),
                findViewById(R.id.parameterTextView6),
                findViewById(R.id.parameterTextView7)
        };

        parameterValueTextViewIdArray = new int[] {
                R.id.parameterValueTextView0,
                R.id.parameterValueTextView1,
                R.id.parameterValueTextView2,
                R.id.parameterValueTextView3,
                R.id.parameterValueTextView4,
                R.id.parameterValueTextView5,
                R.id.parameterValueTextView6,
                R.id.parameterValueTextView7
        };

        parameterGraphViewArray = new GraphView[] {
                findViewById(R.id.parameterValueGraphView0),
                findViewById(R.id.parameterValueGraphView1),
                findViewById(R.id.parameterValueGraphView2),
                findViewById(R.id.parameterValueGraphView3),
                findViewById(R.id.parameterValueGraphView4),
                findViewById(R.id.parameterValueGraphView5),
                findViewById(R.id.parameterValueGraphView6),
                findViewById(R.id.parameterValueGraphView7)
        };

        parameterGraphViewIdArray = new int[] {
                R.id.parameterValueGraphView0,
                R.id.parameterValueGraphView1,
                R.id.parameterValueGraphView2,
                R.id.parameterValueGraphView3,
                R.id.parameterValueGraphView4,
                R.id.parameterValueGraphView5,
                R.id.parameterValueGraphView6,
                R.id.parameterValueGraphView7
        };

        parameterLineGraphSeriesArray = new LineGraphSeries[parameterGraphViewArray.length];
        for (int i = 0; i< parameterGraphViewArray.length; i++) {
            GridLabelRenderer gridLabelRenderer = parameterGraphViewArray[i].getGridLabelRenderer();
            gridLabelRenderer.setPadding(80);
            parameterGraphViewArray[i].getViewport().setScrollable(true);
            parameterGraphViewArray[i].getViewport().setXAxisBoundsManual(true);

            parameterLineGraphSeriesArray[i] = new LineGraphSeries<>();
            parameterGraphViewArray[i].addSeries(parameterLineGraphSeriesArray[i]);
        }
    }

    private void initializePidMaps() {//EVERY NEW PARAMETER TODO
        customParameterNames = new String[2];
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
        pidNames[16] = "10: Air Flow Rate";
        pidCommands[16] = new MassAirFlowCommand();
        pidNames[47] = "2F: Fuel Tank Input Level";
        pidCommands[47] = new FuelLevelCommand();
        pidNames[70] = "46: Ambient Air Temperature";
        pidCommands[70] = new AmbientAirTemperatureCommand();
        pidNames[94] = "5E: Engine Fuel Rate";
        pidCommands[94] = new ConsumptionRateCommand();
        customParameterNames[0] = "Z0: (Estimated) Engine Fuel Rate";
        customParameterNames[1] = "Z1: (Estimated) Fuel Efficiency";

        for (int p=0; p<pidCommands.length; p++) {
            if (pidCommands[p] != null) {
                pidCommands[p].setMaxNumberResponses(1);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.toggleConnectionTypeMenuItem:
                usingBluetoothConnection = !usingBluetoothConnection;
                if (usingBluetoothConnection) {
                    chooseBondedBluetoothDevice();
                    Toast.makeText(this, "Using BlueTooth Connection", Toast.LENGTH_SHORT).show();
                } else {
                    Obd2Activity.connectedBlueToothDevice = null;
                    Toast.makeText(this, "Using WiFi Connection", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void chooseBondedBluetoothDevice() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedBluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] bondedBluetoothDeviceArray = bondedBluetoothDeviceSet.toArray(new BluetoothDevice[bondedBluetoothDeviceSet.size()]);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, bondedBluetoothDeviceArray);
        alertDialogBuilder.setSingleChoiceItems(arrayAdapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                //int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                Obd2Activity.connectedBlueToothDevice = bondedBluetoothDeviceArray[which];
            }
        });
        alertDialogBuilder.setTitle("Choose Bluetooth device");
        alertDialogBuilder.show();
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
                    //this.wantedAvailableParametersHex = data.getStringExtra("WantedAvailablePidsBytes");
                    findViewById(R.id.toggleButton).setEnabled(true);//now available PIDs are known
                    setTextViewToWanted();
                }
                break;
            }
        }
    }

    private void setTextViewToWanted() {
        for (int tv = 0; tv< parameterTextViewArray.length; tv++) {
            parameterTextViewArray[tv].setText("NONE");
        }
        String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailableParametersHex.length() > 0 ? MainActivity.wantedAvailableParametersHex.split(" ") : new String[0];
        for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
            if (wantedAvailablePidsHexArray[wp].charAt(0) == 'Z') {
                int wantedAvailableCustomParameterDec = Integer.parseInt(wantedAvailablePidsHexArray[wp].substring(1), 16);
                parameterTextViewArray[wp].setText(customParameterNames[wantedAvailableCustomParameterDec]);
            } else {
                int wantedAvailablePidDec = Integer.parseInt(wantedAvailablePidsHexArray[wp], 16);
                parameterTextViewArray[wp].setText(pidNames[wantedAvailablePidDec]);
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
        updateProgress(new Pair<Integer, String>(R.id.debugTextView, this.wantedAvailableParametersHex));
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
                initializeObd2();//REMOCK BY COMMENTING
                while (!isCancelled()) {
                    publishProgress(new Pair<Integer, String>(R.id.statusTextView, "SLEEPING"));
                    Long scanPeriodMillis = 2000L;
                    Thread.sleep( scanPeriodMillis - System.currentTimeMillis()%scanPeriodMillis);
                    memoizedPidCommandResults = new String[256];

                    String[] wantedAvailablePidsHexArray = MainActivity.wantedAvailableParametersHex.split(" ");
                    for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {//TODO extract method from for loop
                        String parameterUnitlessResult = getParameterHexUnitlessResult(wantedAvailablePidsHexArray[wp], parameterValueTextViewIdArray[wp]);//REMOCK BY COMMENTING
                        publishProgress(new Pair<Integer, String>(parameterGraphViewIdArray[wp], parameterUnitlessResult));
                    }
                }
                deinitializeObd2();//REMOCK BY COMMENTING
            } catch (Exception e) {
                publishProgress(new Pair<Integer, String>(R.id.statusTextView, e.getMessage()));
                Log.e("example.app", e.getMessage());
            }
            return null;
        }

        private String getParameterHexUnitlessResult(String parameterHex, int parameterValueTextViewId) {
            String parameterResult = "123M";//MOCK
            String parameterUnitlessResult = "123";//MOCK
            if (parameterHex.charAt(0) == 'Z') {//EVERY NEW CUSTOM PARAMETER TODO
                if (parameterHex.equals("Z0")) {
                    int airFlowRatePidDec = 16;
                    Double airFlowRateValue = getPidDecCommandValue(airFlowRatePidDec, null);//in g per s
                    Double estimatedEngineFuelRateValue = airFlowRateValue / 14.7 / 719.7 * 3600;//1 gram of gasoline per 14.7 grams of air, one liter of gasoline per 719.7g of gasoline, 3600 seconds per hour

                    parameterUnitlessResult = String.format("%.2f", estimatedEngineFuelRateValue);
                    publishProgress(new Pair(parameterValueTextViewId, parameterUnitlessResult + "L/hr"));//in L per hr
                } else if (parameterHex.equals("Z1")) {
                    int airFlowRatePidDec = 16;
                    Double airFlowRateValue = getPidDecCommandValue(airFlowRatePidDec, null);//in g per s
                    Double estimatedEngineFuelRateValue = airFlowRateValue / 14.7 / 719.7 * 3600;//1g of gasoline per 14.7g of air, 1L of gasoline per 719.7g of gasoline, 3600s per hr

                    int vehicleSpeedPidDec = 13;
                    Double vehicleSpeedValue = getPidDecCommandValue(vehicleSpeedPidDec, null);//in km/hr
                    Double estimatedFuelEfficiencyValue = vehicleSpeedValue / estimatedEngineFuelRateValue;

                    parameterUnitlessResult = String.format("%.2f", estimatedFuelEfficiencyValue);
                    publishProgress(new Pair(parameterValueTextViewId, parameterUnitlessResult + "km/L"));//in km per L
                }
            } else {
                int wantedAvailablePidDec = Integer.parseInt(parameterHex, 16);
                parameterUnitlessResult = getPidDecCommandUnitlessResult(wantedAvailablePidDec, parameterValueTextViewId);
            }
            return parameterUnitlessResult;
        }

        private Double getPidDecCommandValue(int pidDec, Integer commandValueTextViewId) {
            String commandUnitlessResult = getPidDecCommandUnitlessResult(pidDec, commandValueTextViewId);
            Double commandValue = Double.valueOf(commandUnitlessResult);
            return commandValue;
        }

        private String getPidDecCommandUnitlessResult(int pidDec, Integer commandValueTextViewId) {
            String commandResult = getMemoizedPidDecCommandResult(pidDec, commandValueTextViewId);
            String commandUnitlessResult = commandResult.replaceAll("[^\\d.]", "");//TODO memoize write
            return commandUnitlessResult;
        }

        private String getMemoizedPidDecCommandResult(int pidDec, Integer commandValueTextViewId) {
            if (memoizedPidCommandResults[pidDec] == null) {
                memoizedPidCommandResults[pidDec] = runCommand(pidCommands[pidDec], commandValueTextViewId);
            }
            return memoizedPidCommandResults[pidDec];
        }
    }

    public static boolean isAvailablePidHex(String pidHex) {
        int pidDec = Integer.parseInt(pidHex, 16);
        boolean isAvailablePidHex = pidDec < MainActivity.allAvailablePidsBinary.length() && MainActivity.allAvailablePidsBinary.charAt(pidDec) == '1';
        return isAvailablePidHex;
    }
}
