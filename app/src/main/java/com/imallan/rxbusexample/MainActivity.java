package com.imallan.rxbusexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.imallan.rxbus.annotation.RxBusSchedulers;
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
                MainActivity$BindUtils.bind(MyApplication.getBus(), MainActivity.this);
            }
        });
        mButtonUnbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyApplication.getBus().unbind(MainActivity.this);
            }
        });

    }

    @Subscribe(scheduler = RxBusSchedulers.ANDROID_MAIN)
    public void setText(MyEvent ev) {
        mTextView.setText(ev.tag);
    }

    @Subscribe(scheduler = RxBusSchedulers.IMMEDIATE)
    public void logEvent(MyEvent ev) {
        Log.d(TAG, "logEvent: " + ev.tag);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity$BindUtils.unbind(MyApplication.getBus(), MainActivity.this);
    }

    static class MyEvent {

        public final String tag;

        MyEvent(String tag) {
            this.tag = tag;
        }
    }
}

