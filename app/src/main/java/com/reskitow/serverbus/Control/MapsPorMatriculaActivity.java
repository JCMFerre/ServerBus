package com.reskitow.serverbus.Control;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.reskitow.serverbus.BD.BDRutasSQLite;
import com.reskitow.serverbus.Model.Ruta;
import com.reskitow.serverbus.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MapsPorMatriculaActivity extends AppCompatActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener {

    private GoogleMap mMap;
    private String matricula;
    private boolean noPuedoPulsarMenu;
    private boolean sincronizado;
    private BDRutasSQLite bdRutasSQLite;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_por_matricula);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        findViews();
        inicializarBDLocal();
        obtenerMatriculaIntent();
        comprobarSincronizacion();
    }

    private void findViews() {
        spinner = (Spinner) findViewById(R.id.spinner_sesiones);
    }

    private void inicializarBDLocal() {
        bdRutasSQLite = new BDRutasSQLite(this);
    }

    private void comprobarSincronizacion() {
        String ultimaSync = obtenerUltimaSincronizacionPorMatricula();
        if (ultimaSync == null || ultimaSync.equals("null")) {
            lanzarSincronizacion();
        } else {
            // OBTENER DE LA BD LOCAL.
            sincronizado = true;
            Toast.makeText(this, getString(R.string.ultima_sinc_toast_info) + ultimaSync + getString(R.string.ultima_sinc_toast_info2), Toast.LENGTH_LONG).show();
        }
    }

    private void lanzarSincronizacion() {
        new ObtenerTodasLasRutasPorMatricula().execute();
    }

    private String obtenerUltimaSincronizacionPorMatricula() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.ultima_sinc) + matricula, null);
    }

    private void anadirUltimaSincronizacionPorMatricula() {
        String ultimaFechaSinc = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date().getTime());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.ultima_sinc) + matricula, ultimaFechaSinc);
        editor.commit();
    }

    private void obtenerMatriculaIntent() {
        if (matricula == null) {
            matricula = getIntent().getStringExtra("matricula");
        }
    }

    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.key_ip_servidor), null);
    }


    private void actualizarUI() {
        if (mMap != null) {
            List<Ruta> rutasDiferentes = bdRutasSQLite.obtenerSesionesDistinct(matricula);
            String[] adapter = new String[rutasDiferentes.size() + 1];
            adapter[0] = getString(R.string.primera_opcion_spinner);
            for (int i = 1; i < adapter.length; i++) {
                Ruta ruta = rutasDiferentes.get((i - 1));
                adapter[i] = ruta.getIdSesion();
            }
            cargarSpinner(adapter);
        }
    }

    private void cargarSpinner(String[] adapter) {
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, adapter);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
        findViewById(R.id.layout_con_spinner).setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (sincronizado) {
            actualizarUI();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_solo_refrescar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refrescar_solo:
                if (!noPuedoPulsarMenu) {
                    lanzarSincronizacion();
                } else {
                    Toast.makeText(this, R.string.info_graciosa_sincronizar, Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            cargarMapaConSesion(parent.getSelectedItem().toString());
        }
    }

    private void cargarMapaConSesion(String idSesion) {
        anadirPolyline(bdRutasSQLite.obtenerRutasPorIdSesion(idSesion));
    }

    private void anadirPolyline(List<Ruta> rutas) {
        List<LatLng> latsLongs = new ArrayList<>();
        LatLng ultimaPos = null;
        for (Ruta ruta : rutas) {
            ultimaPos = new LatLng(ruta.getLatitud(), ruta.getLongitud());
            latsLongs.add(ultimaPos);
        }
        Random rnd = new Random();
        mMap.addPolyline(new PolylineOptions()
                .addAll(latsLongs)
                .color(Color.rgb(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255))));
        if (ultimaPos != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ultimaPos, 18F));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class ObtenerTodasLasRutasPorMatricula extends AsyncTask<Void, Void, List<Ruta>> {

        private String matriculaTemp;
        private String ipServidor;
        private View vistaCargando;

        @Override
        protected void onPreExecute() {
            this.matriculaTemp = MapsPorMatriculaActivity.this.matricula;
            ipServidor = MapsPorMatriculaActivity.this.obtenerIpPrefs();
            MapsPorMatriculaActivity.this.noPuedoPulsarMenu = true;
            vistaCargando = MapsPorMatriculaActivity.this.findViewById(R.id.layout_sincronizando_por_matricula);
            vistaCargando.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Ruta> doInBackground(Void... params) {
            List<Ruta> rutas = new ArrayList<>();
            try {
                StringBuffer url = new StringBuffer().append("http://")
                        .append(ipServidor).append(":8080/ServicioRestAutobus/webresources/rutas/todasLasRutas/%7B\"matricula\":\"")
                        .append(matriculaTemp).append("\",\"contrasena\":\"estoyadaigual\",\"activo\":false%7D");
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");
                int codigoRespuesta = urlConnection.getResponseCode();
                Log.i("CODIGO", "GET: " + codigoRespuesta);
                if (codigoRespuesta == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    JSONArray arrayJSON = new JSONArray(response.toString());
                    for (int i = 0; i < arrayJSON.length(); i++) {
                        JSONObject jsonActual = arrayJSON.getJSONObject(i);
                        rutas.add(new Ruta(jsonActual.getString("idSesion"), jsonActual.getString("matricula"),
                                jsonActual.getString("fecha"), jsonActual.getDouble("latitud"), jsonActual.getDouble("longitud")));
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                rutas = null;
            } catch (JSONException e) {
                e.printStackTrace();
                rutas = null;
            }
            return rutas;
        }

        @Override
        protected void onPostExecute(List<Ruta> rutas) {
            MapsPorMatriculaActivity.this.noPuedoPulsarMenu = false;
            vistaCargando.setVisibility(View.GONE);
            MapsPorMatriculaActivity.this.sincronizado = true;
            if (rutas == null) {
                Toast.makeText(MapsPorMatriculaActivity.this, "Error", Toast.LENGTH_SHORT).show();
            } else {
                MapsPorMatriculaActivity.this.bdRutasSQLite.anadirRutas(rutas);
                MapsPorMatriculaActivity.this.anadirUltimaSincronizacionPorMatricula();
                MapsPorMatriculaActivity.this.actualizarUI();
            }
        }
    }

}
