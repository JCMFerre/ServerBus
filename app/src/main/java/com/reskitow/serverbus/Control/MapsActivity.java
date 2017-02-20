package com.reskitow.serverbus.Control;

import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final Pattern PATTERN_IP = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private GoogleMap mMap;
    private List<Ruta> rutas;
    private boolean noPuedoPulsarMenu;
    private Marker markerUltimoPulsado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        comprobarIp();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void lanzarRecuperarUltimasPosiciones() {
        new ObtenerUltimasPosiciones().execute();
    }

    private void comprobarIp() {
        String ip = obtenerIpPrefs();
        if (ip == null || !isIPCorrecta(ip)) {
            lanzarPrefs();
        } else {
            lanzarRecuperarUltimasPosiciones();
        }
    }

    private boolean isIPCorrecta(String ip) {
        return PATTERN_IP.matcher(ip).matches();
    }

    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.key_ip_servidor), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_menu_ajustes:
                lanzarPrefs();
                break;
            case R.id.btn_menu_refrescar:
                if (!noPuedoPulsarMenu) {
                    lanzarRecuperarUltimasPosiciones();
                } else {
                    Toast.makeText(this, R.string.info_graciosa_sincronizar, Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void lanzarPrefs() {
        startActivity(new Intent(this, PreferenciasActivity.class));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (rutas != null) {
            cargarRutasMapa();
        }
    }

    private void cargarRutasMapa() {
        if (rutas.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_hay_resultados, Toast.LENGTH_SHORT).show();
        } else {
            Ruta ultimaRuta = null;
            for (Ruta ruta : rutas) {
                LatLng locRuta = new LatLng(ruta.getLatitud(), ruta.getLongitud());
                mMap.addMarker(new MarkerOptions()
                        .position(locRuta)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.bus))
                        .title(ruta.getMatricula())
                        .snippet(getString(R.string.info_ultima_localizacion) + ": " + ruta.getFecha()));
                ultimaRuta = ruta;
            }
            if (ultimaRuta != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(ultimaRuta.getLatitud(), ultimaRuta.getLongitud()), 16F));
            }
            mMap.setOnMarkerClickListener(this);
        }
    }

    private void actualizarMapa(List<Ruta> rutas) {
        this.rutas = rutas;
        if (mMap != null) {
            cargarRutasMapa();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (markerUltimoPulsado != null && (markerUltimoPulsado == marker || markerUltimoPulsado.equals(marker))) {
            Intent i = new Intent(this, MapsPorMatriculaActivity.class);
            i.putExtra("matricula", marker.getTitle());
            startActivity(i);
        } else {
            markerUltimoPulsado = marker;
            Toast.makeText(this, getString(R.string.info_toast_ver_sesiones) + marker.getTitle() + ".", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private class ObtenerUltimasPosiciones extends AsyncTask<Void, Void, List<Ruta>> {

        private String ipServidor;
        private View vistaCargando;

        @Override
        protected void onPreExecute() {
            ipServidor = MapsActivity.this.obtenerIpPrefs();
            vistaCargando = findViewById(R.id.layout_sincronizando);
            vistaCargando.setVisibility(View.VISIBLE);
            MapsActivity.this.noPuedoPulsarMenu = true;
        }

        @Override
        protected List<Ruta> doInBackground(Void... params) {
            List<Ruta> rutas = new ArrayList<>();
            StringBuffer url = new StringBuffer().append("http://")
                    .append(ipServidor)
                    .append(":8080/ServicioRestAutobus/webresources/rutas/ultimasPosiciones");
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");
                int codigoRespuesta = urlConnection.getResponseCode();
                Log.i("GET", "Response Code :: " + codigoRespuesta);
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
            } catch (IOException e) {
                e.printStackTrace();
                rutas = null;
            } catch (JSONException e) {
                e.printStackTrace();
                rutas = null;
            }
            return rutas;
        }

        @Override
        protected void onPostExecute(List<Ruta> rutas) {
            if (rutas == null) {
                Toast.makeText(MapsActivity.this, "Error", Toast.LENGTH_SHORT).show();
            } else {
                MapsActivity.this.actualizarMapa(rutas);
            }
            vistaCargando.setVisibility(View.GONE);
            MapsActivity.this.noPuedoPulsarMenu = false;
        }
    }
}
