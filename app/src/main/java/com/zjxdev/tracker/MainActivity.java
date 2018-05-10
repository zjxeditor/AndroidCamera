package com.zjxdev.tracker;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testfunc();

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("hello world");
    }

    private void testfunc() {
        CSRT tracker = new CSRT(100, 100, new CSRT.TrackerParams(), "");
        tracker.dispose();
    }

}
