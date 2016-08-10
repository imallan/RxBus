package com.imallan.rxbusexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.imallan.rxbus.Bus;
import com.imallan.rxbus.annotation.Subscribe;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView mTextView;
    @SuppressWarnings("FieldCanBeLocal")
    private View mButtonBind;
    @SuppressWarnings("FieldCanBeLocal")
    private View mButtonUnbind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text_counter);
        mButtonBind = findViewById(R.id.button_bind);
        mButtonUnbind = findViewById(R.id.button_unbind);
        mButtonBind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity$BindUtils.bind(MainActivity.this);
            }
        });
        mButtonUnbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bus.unbind(MainActivity.this);
            }
        });

    }

    @Subscribe(scheduler = Subscribe.ANDROID_MAIN)
    public void setText(MyEvent ev) {
        mTextView.setText(ev.getTag() + "|" + ev.getObj());
    }

    @Subscribe(scheduler = Subscribe.IMMEDIATE)
    public void logEvent(MyEvent ev) {
        Log.d(TAG, "logEvent: " + ev.getTag() + "|" + ev.getObj());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.unbind(this);
    }

    static class MyEvent extends Bus.BusEvent {

        MyEvent(Object obj) {
            super(obj);
        }
    }
}

