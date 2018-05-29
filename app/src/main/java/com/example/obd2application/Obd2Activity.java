package com.example.obd2application;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.UUID;

/**
 * Created by linkk on 2/6/2018.
 */

public abstract class Obd2Activity extends AppCompatActivity {

    public static boolean usingBluetoothConnection;
    public static BluetoothDevice connectedBlueToothDevice;
    public static UUID uuid;

    /**
     * Handles the logic for displaying values through either textviews or graphviews
     * @param values pairs of view id and value to display through that view
     */
    public void updateProgress(Pair<Integer, String>... values) {
        for (int v=0; v<values.length; v++) {
            if (values[v].first == null || values[v].second == null) {
                continue;
            }

            View viewById = findViewById(values[v].first);
            if (viewById != null) {
                Class<?> viewClass = viewById.getClass();
                if (TextView.class.isAssignableFrom(viewClass)) {
                    TextView textView = (TextView) viewById;
                    textView.setText(values[v].second);
                } else if (GraphView.class.isAssignableFrom(viewClass)) {
                    GraphView graphView = (GraphView) viewById;
                    LineGraphSeries<DataPoint> lineGraphSeries = (LineGraphSeries<DataPoint>) graphView.getSeries().get(0);
                    double currentTimeSeconds = (double) System.currentTimeMillis()/1000%10000;
                    if (currentTimeSeconds < lineGraphSeries.getHighestValueX()) {
                        lineGraphSeries.resetData(new DataPoint[0]);//TODO consider adding the already existing data minus 10000 (for negative time values) instead of completely resetting
                    }
                    lineGraphSeries.appendData(new DataPoint(currentTimeSeconds, Double.valueOf(values[v].second)), true, 21);
                    graphView.getViewport().setMaxX(currentTimeSeconds);
                    graphView.getViewport().setMinX(currentTimeSeconds - 40);
//                    graphView.removeAllSeries();
//                    graphView.addSeries(lineGraphSeries);
                }
            }
        }
        TextView currentTimeMillisValueTextView = findViewById(R.id.currentTimeMillisValueTextView);
        if (currentTimeMillisValueTextView != null) {
            currentTimeMillisValueTextView.setText(String.valueOf(System.currentTimeMillis()));
        }
    }

}
