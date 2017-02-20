package com.reskitow.serverbus.Control;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.reskitow.serverbus.R;

public class PreferenciasFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);
    }
}
