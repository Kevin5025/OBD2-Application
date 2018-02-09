package com.example.obd2application;

import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.widget.TextView;

/**
 * Created by linkk on 2/6/2018.
 */

public class Obd2Activity extends AppCompatActivity {

    public void updateProgress(Pair<Integer, String>... values) {
        for (int v=0; v<values.length; v++) {
            if (values[v].first == null || values[v].second == null) {
                continue;
            }

            TextView textView = findViewById(values[v].first);
            if (textView != null) {
                textView.setText(values[v].second);
            }
        }
        TextView currentTimeMillisValueTextView = findViewById(R.id.currentTimeMillisValueTextView);
        if (currentTimeMillisValueTextView != null) {
            currentTimeMillisValueTextView.setText(String.valueOf(System.currentTimeMillis()));
        }
    }

}
