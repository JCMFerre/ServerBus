package com.reskitow.serverbus.Control;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity que gestiona el fragment de preferencias.
 */
public class PreferenciasActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferenciasFragment())
                .commit();
    }
}
