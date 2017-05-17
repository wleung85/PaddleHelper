package com.example.paddlehelper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.System;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private static final int PERMS_REQUEST_CODE = 111;

    Button btnReset, btnHit;
    TextView txtSpeed, txtSpm, txtLastSpm, txtAvgSpm;
    int strokeNum;
    boolean started;
    long lastTime, currentTime;
    Toolbar mToolbar;
    double sum, hitNum, spmActual;
    LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        btnReset = (Button) findViewById(R.id.buttonReset);
        btnHit = (Button) findViewById(R.id.buttonHit);
        txtSpm = (TextView) findViewById(R.id.textViewSpmCount);
        txtLastSpm = (TextView) findViewById(R.id.textViewLastSpm);
        txtAvgSpm = (TextView) findViewById(R.id.textViewAvgSpmCount);

        if (hasPermissions()) {
            lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
        else {
            requestPerms();
        }

        started = false;
        strokeNum = getStrokeNum();
        sum = 0.0;
        hitNum = 0.0;

        btnHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);

                if (btnReset.getText().equals("Reset")) {
                    btnReset.setText("Stop");
                }
                if (!started) {
                    lastTime = System.nanoTime();
                    started = true;
                }
                else {
                    //Updating actual stroke per min
                    if(!txtSpm.getText().equals("0")) {
                        txtLastSpm.setText(txtSpm.getText());
                    }

                    currentTime = System.nanoTime();
                    spmActual = (double)strokeNum*60000.00/TimeUnit.NANOSECONDS.toMillis(currentTime - lastTime);
                    txtSpm.setText(String.format("%.1f", spmActual));
                    lastTime = currentTime;

                    //Updating average stroke per min
                    hitNum++;
                    sum += spmActual;
                    txtAvgSpm.setText(String.format("%.1f", sum/hitNum));

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
                    btnReset.setText("Reset");
                }

                else {
                    txtLastSpm.setText("");
                    txtAvgSpm.setText("");
                    sum = 0.0;
                    hitNum = 0.0;
                    btnReset.setText("Stop");
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
        return Integer.parseInt(pref.getString("strokePref", "3"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        strokeNum = getStrokeNum();
    }

    @Override
    public void onLocationChanged(Location location) {

        txtSpeed = (TextView) findViewById(R.id.textViewSpeedCount);
        if(location==null) {
            txtSpeed.setText("Null");
        }
        else {
            float nCurrentSpeed = location.getSpeed();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (pref.getString("speedUnitPref", "2").equals("1")) {
                nCurrentSpeed = nCurrentSpeed * (float)3.6;
            }
            txtSpeed.setText(String.valueOf(nCurrentSpeed));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private boolean hasPermissions() {
        int res;
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

        for (String perms : permissions) {
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)) {
                return false;
            }
        }

        return true;
    }

    private void requestPerms() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(permissions,PERMS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;

        switch (requestCode) {
            case PERMS_REQUEST_CODE:

                for (int res : grantResults) {
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }

                break;

            default:
                allowed = false;
                break;
        }

        if (!allowed) {
            Toast.makeText(this,"Location Permissions denied.", Toast.LENGTH_SHORT).show();
        }
    }

}
