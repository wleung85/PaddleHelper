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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.System;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private static final int PERMS_REQUEST_CODE = 111;
    private static final String SPM_TAG = "SPM Count";
    private static final String SPEED_TAG = "Speed Count";

    Button btnHit, btnSpeed, btnStopwatch;
    TextView txtSpeed, txtMaxSpeed, txtSpeedTitle, txtSpm, txtLastSpm, txtStopwatch;
    int strokeNum;
    boolean SPMstarted, speedStarted, stopwatchStarted;
    long lastTime, currentTime, secs, mins, hrs;
    Toolbar mToolbar;
    double spmActual;
    LocationManager lm;
    String seconds, minutes, milliseconds;

    private TextView tempTextView; //Temporary TextView
    private Button tempBtn; //Temporary Button
    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        txtSpm = (TextView) findViewById(R.id.textViewSpmCount);
        txtLastSpm = (TextView) findViewById(R.id.textViewLastSpmCount);
        btnHit = (Button) findViewById(R.id.SPMButton);

        txtSpeed = (TextView) findViewById(R.id.textViewSpeedCount);
        txtSpeedTitle = (TextView) findViewById(R.id.textViewSpeedTitle);
        txtMaxSpeed = (TextView) findViewById (R.id.textViewMaxSpeed);
        btnSpeed = (Button) findViewById(R.id.SpeedButton);
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        txtStopwatch = (TextView) findViewById(R.id.textViewStopwatchCount);
        btnStopwatch = (Button) findViewById(R.id.StopwatchButton);

        SPMstarted = false;
        strokeNum = getStrokeNum();
        stopwatchStarted = false;

        //Hitting SPM (i.e. Hit)
        btnHit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SPMstarted) {
                    lastTime = System.nanoTime();
                    SPMstarted = true;

                    //Setting Last SPM
                    if(!txtSpm.getText().equals("00.0")) {
                        txtLastSpm.setText(txtSpm.getText());
                    }

                    //Indicating waiting
                    txtSpm.setText("wait");
                }
                else {
                    //Updating actual stroke per min
                    currentTime = System.nanoTime();
                    spmActual = (double)strokeNum*60000.00/TimeUnit.NANOSECONDS.toMillis(currentTime - lastTime);
                    txtSpm.setText(String.format(Locale.CANADA,"%.1f", spmActual));

                    SPMstarted = false;
                }
            }
        });

        // Long press on SPM (to reset)
        btnHit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SPMstarted = false;
                txtSpm.setText("00.0");

                return true;
            }
        });

        // Measure speed
        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    // Fine Location permission has been granted
                    if (!speedStarted) {
                        // Start measuring speed
                        measureSpeed();
                        txtSpeedTitle.setText("Speed (M)");
                        speedStarted = true;
                    }
                    else {
                        // Stop measuring speed
                        lm.removeUpdates(MainActivity.this);
                        txtSpeedTitle.setText("Speed");
                        speedStarted = false;
                    }
                }

                else {
                    // Fine Location permission has not been granted
                    requestPerms();
                }
            }
        });

        btnSpeed.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Reset measuring speed
                lm.removeUpdates(MainActivity.this);
                txtSpeedTitle.setText("Speed");
                txtSpeed.setText("00.0");
                txtMaxSpeed.setText("00.0");
                speedStarted = false;
                return true;
            }
        });

        btnStopwatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!stopwatchStarted) {
                    if ((((TextView) findViewById(R.id.textViewStopwatchCount)).getText().equals("00:00.0"))) {
                        startTime = System.currentTimeMillis();
                    }
                    else {
                        startTime = System.currentTimeMillis() - elapsedTime;
                    }
                    stopwatchStarted = true;
                    mHandler.removeCallbacks(startTimer);
                    mHandler.postDelayed(startTimer, 0);
                }

                else {
                    mHandler.removeCallbacks(startTimer);
                    stopwatchStarted = false;
                }
            }
        });

        btnStopwatch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                stopwatchStarted = false;
                mHandler.removeCallbacks(startTimer);
                txtStopwatch.setText("00:00.0");

                return true;
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
    protected void onStop() {
        super.onStop();
        if (speedStarted) {
            lm.removeUpdates(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        strokeNum = getStrokeNum();
        if (speedStarted) {
            measureSpeed();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if(location==null) {
            txtSpeed.setText("Null");
        }
        else {
            float nCurrentSpeed = location.getSpeed();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            if (pref.getString("speedUnitPref", "2").equals("1")) {
                nCurrentSpeed = nCurrentSpeed * (float)3.6;
            }
            txtSpeed.setText(String.format(Locale.CANADA,"%.1f",nCurrentSpeed));

            // Setting max speed
            if (nCurrentSpeed > Float.parseFloat((String)txtMaxSpeed.getText())) {
                txtMaxSpeed.setText(String.format(Locale.CANADA,"%.1f",nCurrentSpeed));
            }
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
                    // If user granted all permissions
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }

                break;
            default:

                // If user didn't grant permissions
                allowed = false;
                break;
        }

        if (allowed) {
            // User granted permission, measure speed
            measureSpeed();
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "GPS Permissions denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void measureSpeed(){
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 250, 0, this);
        onLocationChanged(null);
    }

    private void updateTimer (float time) {
        secs = (long)(time/1000);
        mins = (long)((time/1000)/60);
        hrs = (long)(((time/1000)/60)/60);

        milliseconds = String.valueOf((long)time);
        if (milliseconds.length() == 2) {
            milliseconds = "0" + milliseconds;
        }
        if (milliseconds.length() <= 1) {
            milliseconds = "0";
        }
        if (milliseconds.length() > 2) {
            milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);
        }

        secs = secs % 60;
        seconds = String.valueOf(secs);
        if (secs == 0) {
            seconds = "00";
        }
        if (secs < 10 && secs > 0) {
            seconds = "0" + seconds;
        }

        mins = mins % 60;
        minutes = String.valueOf(mins);
        if (mins == 0) {
            minutes = "00";
        }
        if (mins < 10 && mins > 0) {
            minutes = "0" + minutes;
        }

        ((TextView)findViewById(R.id.textViewStopwatchCount)).setText(minutes + ":" + seconds + "." + milliseconds);
    }

    private Runnable startTimer = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            mHandler.postDelayed(this,REFRESH_RATE);
        }
    };

}
