package com.example.paddlehelper;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class FragmentPreferences extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);
    }
}
