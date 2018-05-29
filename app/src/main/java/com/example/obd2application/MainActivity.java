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
import com.github.pires.obd.commands.control.DistanceSinceCCCommand;
import com.github.pires.obd.commands.control.EquivalentRatioCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.control.TimingAdvanceCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.AirFuelRatioCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.fuel.FuelTrimCommand;
import com.github.pires.obd.commands.pressure.BarometricPressureCommand;
import com.github.pires.obd.commands.pressure.FuelPressureCommand;
import com.github.pires.obd.commands.pressure.FuelRailPressureCommand;
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

    public static boolean isKnownAllAvailablePids;//once we launch the PIDs activity, we will ask the vehicle which PIDs are available
    public static String wantedAvailableParametersHex;//each unit (of this string) (e.g. 04 or Z1) is a byte (two hexadecimals 16^2=256) indicating which PID (from 0 to 255) is desired. Thus, this string indicates all of the PIDs that the user checkmarked on the PIDs page.
    public static String allAvailablePidsBinary;//includes PID 00, which is always available in every obd2 vehicle. Thus, the 0th character of this string will be a "1", meaning that PID00 is available. If the 3rd character is a "0", then PID03 is unavailable. If the string is length of 30, then PID31 (also any PID >31) is unavailable.
    public static String[] allAvailablePidsHexResult;//excludes PID 00; Array of 8 strings, each of length 8; Each character is a hexadecimal, which expresses 4 bits. Thus, 8*8*4 bits are expressed. This allAvailablePidsHexResult expresses the same thing as allAvailablePidsBinary, but in a different form.
//    public static final String ALL_AVAILABLE_PIDS_BINARY_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_BINARY_KEY";//I used these to communicate across activities, but I found it easier to just use public static variables, so I don't use these anymore.
//    public static final String ALL_AVAILABLE_PIDS_HEX_RESULT_KEY = "com.example.obd2application.ALL_AVAILABLE_PIDS_HEX_RESULT_KEY";
    public static final int PIDS_ACTIVITY_REQUEST_CODE = 1;

    public static TextView[] parameterTextViewArray;//8 views; these are the 8 graph labels to the upperleft of each graph
    int[] parameterValueTextViewIdArray;//these are the ids of the 8 labels to the upperright of each graph; these labels tell you the exact value most recently added to the graph
    public static GraphView[] parameterGraphViewArray;//8 views; these are the 8 graphs
    int[] parameterGraphViewIdArray;//these are the ids of the 8 graph views
    LineGraphSeries<DataPoint>[] parameterLineGraphSeriesArray;//each graph view needs a line graph series; see 'com.jjoe64:graphview:4.2.1'

    String[] customParameterNames;//parameters that we calculate ourselves using the standard parameters of the obd2 protocol
    String[] pidNames;//mapping the PID ID (index in decimal) to the name of that (mode 01) parameter expressed here: https://en.wikipedia.org/wiki/OBD-II_PIDs
    ObdCommand[] pidCommands;//mapping the PID ID (index in decimal) to the pires.obd API copied in from here: https://github.com/Kevin5025/obd-java-api
    String[] memoizedPidCommandResults;//multiple custom parameters may rely on the same standard PIDs

    /**
     * This runs whenever the Main Activity starts, such as when the app starts. I think you can set which Activity that the app starts with in the AncdroidManifest.xml file?
     * This just initializes things such as defaults and such as buttons. The app does not do anything else until you click one of the buttons or options in the options menu.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);//hiddenly defined in AppCompatActivity I think
        setContentView(R.layout.activity_main);
        obd2StreamTask = null;
        isToggleButtonOn = false;
        findViewById(R.id.toggleButton).setEnabled(false);//disabled until available PIDs are known

        isKnownAllAvailablePids = false;
        wantedAvailableParametersHex = "04 05 0C 0D 2F Z0 Z1";//just a default
        allAvailablePidsBinary = "1";//because PID 00 is always available
        allAvailablePidsHexResult = new String[8];

        initializeViewArrays();
//        updateProgress(new Pair<Integer, String>(R.id.engineRpmValueGraphView, "801"));
        initializePidMaps();

        usingBluetoothConnection = false;
        connectedBlueToothDevice = null;
        Toast.makeText(this, "Using WiFi Connection", Toast.LENGTH_SHORT).show();
        uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//used for Bluetooth; you should lookup this string "00001101-0000-1000-8000-00805F9B34FB"
    }

    /**
     * Gets me convenient access to the 8 graphs/labels
     */
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

    /**
     * Gives me convenient access to each PID's name and API command
     */
    private void initializePidMaps() {//EVERY NEW PARAMETER TODO
        customParameterNames = new String[2];
        pidNames = new String[256];
        pidCommands = new ObdCommand[256];

        pidNames[4] = "04: Calculated Engine Load";
        pidCommands[4] = new LoadCommand();
        pidNames[5] = "05: Engine Coolant Temperature";
        pidCommands[5] = new EngineCoolantTemperatureCommand();
        pidNames[6] = "06: Short Term Fuel Trim - Bank 1";
        pidCommands[6] = new FuelTrimCommand();
        pidNames[10] = "0A: Fuel Pressure";
        pidCommands[10] = new FuelPressureCommand();
        pidNames[11] = "0B: Intake Manifold Absolute Pressure";
        pidCommands[11] = new IntakeManifoldPressureCommand();
        pidNames[12] = "0C: Engine Rpm";
        pidCommands[12] = new RPMCommand();
        pidNames[13] = "0D: Vehicle Speed";
        pidCommands[13] = new SpeedCommand();
        pidNames[14] = "0E: Timing Advance";
        pidCommands[14] = new TimingAdvanceCommand();
        pidNames[15] = "0F: Intake Air Temperature";
        pidCommands[15] = new AirIntakeTemperatureCommand();
        pidNames[16] = "10: Air Flow Rate";
        pidCommands[16] = new MassAirFlowCommand();
        pidNames[17] = "11: Throttle Position";
        pidCommands[17] = new ThrottlePositionCommand();
        pidNames[31] = "1F: Run Time Since Engine Start";
        pidCommands[31] = new RuntimeCommand();;
        pidNames[35] = "23: Fuel Rail Gauge Pressure";
        pidCommands[35] = new FuelRailPressureCommand();
        pidNames[47] = "2F: Fuel Tank Input Level";
        pidCommands[47] = new FuelLevelCommand();
        pidNames[49] = "31: Distance Traveled Since Codes Cleared";
        pidCommands[49] = new DistanceSinceCCCommand();
        pidNames[51] = "33: Absolute Barometric Pressure";
        pidCommands[51] = new BarometricPressureCommand();
        pidNames[66] = "42: Control Module Voltage";
        pidCommands[66] = new ModuleVoltageCommand();
        pidNames[67] = "43: Absolute Load Value";
        pidCommands[67] = new AbsoluteLoadCommand();
        pidNames[68] = "44: Fuel-Air Commanded Equivalence Ratio";
        pidCommands[68] = new AirFuelRatioCommand();//pidCommands[68] = new EquivalentRatioCommand();
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

    /**
     * Right now, the only menu option is to toggle between attempting a WiFi or BlueTooth connection with an obd2 device.
     * The phone should be connected to the OBD2 device's WiFi network or BlueTooth pairing.
     * @param menuItem
     * @return
     */
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

    /**
     * You should pair your phone to the BlueTooth device (the BlueTooth OBD2 adaptor) before hand.
     * This function will let the user choose a BlueTooth device out of all the BlueTooth devices that your phone has paired with before in its history.
     * The user should choose the appropriate option for the OBD2 adaptor
     * The user chooses the device from a dialog that pops up
     */
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

    /**
     * onPidsButtonClick() calls startActivityForResult(intent, PIDS_ACTIVITY_REQUEST_CODE)\
     * once the user exits the PIDs screen and returns to the MainActivity, this function (onActivityResult) is called
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (PIDS_ACTIVITY_REQUEST_CODE) : {
                if (resultCode == Activity.RESULT_OK || isKnownAllAvailablePids) {
                    //this.wantedAvailableParametersHex = data.getStringExtra("WantedAvailablePidsBytes");//I switched to just using public static variables
                    findViewById(R.id.toggleButton).setEnabled(true);//now available PIDs are known, so we can toggle the data streaming on
                    setTextViewToWanted();//switches the labels to the parameters that the user wants, as indicated by which checkboxes the user marked in the PIDs screen
                }
                break;
            }
        }
    }

    /**
     * switches the labels to the parameters that the user wants, as indicated by which checkboxes the user marked in the PIDs screen
     */
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

    /**
     * goes to the PIDs screen
     * @param view
     */
    public void onPidsButtonClick(View view) {
        Intent intent = new Intent(this, PidsActivity.class);
//        intent.putExtra(ALL_AVAILABLE_PIDS_BINARY_KEY, allAvailablePidsBinary);
//        intent.putExtra(ALL_AVAILABLE_PIDS_HEX_RESULT_KEY, allAvailablePidsHexResult);
        startActivityForResult(intent, PIDS_ACTIVITY_REQUEST_CODE);
    }

    /**
     * toggles whether the app will continously ask the OBD2 device for data about the vehicle
     * @param view
     */
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

    /**
     * displays some text on the screen to help me know what's going on when programming
     * @param view
     */
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

        /**
         * continuously asks the obd2 adaptor for data every two seconds (or every whatever scanPeriodMillis is set to)
         * @param strings
         * @return
         */
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
                    for (int wp=0; wp<wantedAvailablePidsHexArray.length; wp++) {
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

        /**
         *
         * @param parameterHex PID or custom parameter ID desired
         * @param parameterValueTextViewId id of textview to output the parameter result
         * @return the parameter result
         */
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
            String commandUnitlessResult = commandResult.replaceAll("[^\\d.]", "");
            return commandUnitlessResult;
        }

        //if memoized, use memoized value, otherwise run the command to get the value from the obd2 adapter
        private String getMemoizedPidDecCommandResult(int pidDec, Integer commandValueTextViewId) {
            if (memoizedPidCommandResults[pidDec] == null) {
                memoizedPidCommandResults[pidDec] = runCommand(pidCommands[pidDec], commandValueTextViewId);
            }
            return memoizedPidCommandResults[pidDec];
        }
    }

    /**
     * @param pidHex
     * @return whether the vehicle supports this PID, assuming we have launched the PIDs activity to check already
     */
    public static boolean isAvailablePidHex(String pidHex) {
        int pidDec = Integer.parseInt(pidHex, 16);
        boolean isAvailablePidHex = pidDec < MainActivity.allAvailablePidsBinary.length() && MainActivity.allAvailablePidsBinary.charAt(pidDec) == '1';
        return isAvailablePidHex;
    }
}
