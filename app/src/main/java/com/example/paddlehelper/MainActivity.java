package com.example.paddlehelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.System;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Button btnReset, btnHit;
    TextView txtSpeed, txtSpm, txtLastSpm;
    int strokeNum;
    boolean started;
    long lastTime, currentTime;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        btnReset = (Button) findViewById(R.id.buttonReset);
        btnHit = (Button) findViewById(R.id.buttonHit);
        txtSpeed = (TextView) findViewById(R.id.textViewSpeedCount);
        txtSpm = (TextView) findViewById(R.id.textViewSpmCount);
        txtLastSpm = (TextView)findViewById(R.id.textViewLastSpm);

        started = false;
        strokeNum = getStrokeNum();

        btnHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!started) {
                    lastTime = System.nanoTime();
                    started = true;
                }
                else {
                    if(!txtSpm.getText().equals("0")) {
                        txtLastSpm.setText(txtSpm.getText());
                    }

                    currentTime = System.nanoTime();
                    txtSpm.setText(String.format("%.1f",((double)strokeNum*60000.00
                            /TimeUnit.NANOSECONDS.toMillis(currentTime - lastTime))));
                    lastTime = currentTime;

                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (started) {
                    started = false;
                    txtLastSpm.setText(txtSpm.getText());
                    txtSpm.setText("0");
                }

                else {
                    txtLastSpm.setText("");
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mMenuInflater = getMenuInflater();
        mMenuInflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_setting) {
            Intent i = new Intent(MainActivity.this, Prefs.class);
            startActivity(i);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    private int getStrokeNum() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int extracted = Integer.parseInt(pref.getString("strokePref", "3"));
        return extracted;
    }

    @Override
    protected void onResume() {
        super.onResume();
        strokeNum = getStrokeNum();
    }
}
