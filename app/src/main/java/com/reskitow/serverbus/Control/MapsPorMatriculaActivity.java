package com.reskitow.serverbus.Control;

import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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

public class MapsPorMatriculaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String matricula;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_por_matricula);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        obtenerMatriculaIntent();
        new ObtenerTodasLasRutasPorMatricula().execute();
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private class ObtenerTodasLasRutasPorMatricula extends AsyncTask<Void, Void, List<Ruta>> {

        private String matriculaTemp;
        private String ipServidor;

        @Override
        protected void onPreExecute() {
            this.matriculaTemp = MapsPorMatriculaActivity.this.matricula;
            ipServidor = MapsPorMatriculaActivity.this.obtenerIpPrefs();
        }

        @Override
        protected List<Ruta> doInBackground(Void... params) {
            List<Ruta> rutas = new ArrayList<>();
            try {
                StringBuffer url = new StringBuffer().append("http://")
                        .append(ipServidor).append(":8080/ServicioRestAutobus/webresources/rutas/todasLasRutas/%7B")
                        .append(matriculaTemp).append("%7D");
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                int codigoRespuesta = urlConnection.getResponseCode();
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
            if (rutas == null) {
                Toast.makeText(MapsPorMatriculaActivity.this, "Error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MapsPorMatriculaActivity.this, "Length: " + rutas.size(), Toast.LENGTH_SHORT).show();
            }
        }

    }
}
