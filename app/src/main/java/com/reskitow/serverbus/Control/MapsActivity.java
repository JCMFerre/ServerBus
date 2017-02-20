package com.reskitow.serverbus.Control;

import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

/**
 * Mapa y activity principal, en la cual se mostrarán las últimas posiciones de los autobuses.
 */
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Este método llama a la tarea asíncrona y obtiene la ultimas localizaciones.
     */
    private void lanzarRecuperarUltimasPosiciones() {
        new ObtenerUltimasPosiciones().execute();
    }

    /**
     * Obtiene la IP de las preferencias, comprueba si no está introducida o está mal formada.
     */
    private void comprobarIp() {
        String ip = obtenerIpPrefs();
        if (ip == null || !isIPCorrecta(ip)) {
            lanzarPrefs();
        } else {
            lanzarRecuperarUltimasPosiciones();
        }
    }

    /**
     * Obtenemos el puerto del servidor de las preferencias, si no está seteado por defecto es 8080.
     *
     * @return Puerto del servidor.
     */
    private String obtenerPuertoServidor() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_puerto_servidor), "8080");
    }

    /**
     * Comprueba si la ip es correcta (IP v4).
     *
     * @param ip Ip a comprobar.
     * @return Devuelve true si está bien formada y false al revés.
     */
    private boolean isIPCorrecta(String ip) {
        return PATTERN_IP.matcher(ip).matches();
    }

    /**
     * Obtiene la IP guardada en las preferencias.
     *
     * @return IP guardada.
     */
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

    /**
     * Lanza la activity de preferencias.
     */
    private void lanzarPrefs() {
        startActivity(new Intent(this, PreferenciasActivity.class));
    }

    /**
     * Listener que implementa OnMapReadyCallback, le llega el mapa cuando está listo para ser usado,
     * si da algún problema con emulador o versiones anteriores hay que cambiar el gradle, y poner una
     * versión menor del servicio de mapas de google..
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (rutas != null) {
            cargarRutasMapa();
        }
    }

    /**
     * Este método es llamado cuando el mapa ya está listo y se ha realizado la tarea asíncrona,
     * añade los marcadores (Marker) al mapa y posiciona la cámara del mapa en la última LatLng obtenida.
     */
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

    /**
     * Este método es llamado cuando termina la tarea asíncrona, si el mapa ya está listo llama al método
     * cargarRutasMapa() y setea la lista que le llega como atributo de clase.
     *
     * @param rutas Últimas posiciones de los autobuses.
     */
    private void actualizarMapa(List<Ruta> rutas) {
        this.rutas = rutas;
        if (mMap != null) {
            cargarRutasMapa();
        }
    }

    /**
     * Listener de los marcadores, la primera vez que pulsamos un marcador se setea como una variable global,
     * si vuelve a pulsar el mismo lanza la activity de mapas que traza las rutas, obtiene la matricula
     * del título del marcador pulsado.
     *
     * @param marker Marcador pulsado.
     * @return Por defecto false.
     */
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

    /**
     * Clase que gestiona la tarea asíncrona y obtiene las últimas posiciones de los autobuses.
     */
    private class ObtenerUltimasPosiciones extends AsyncTask<Void, Void, List<Ruta>> {

        private String ipServidor;
        private String puerto;
        private View vistaCargando;

        /**
         * Método que se ejecuta en el hilo principal, aquí seteamos los atributos a nuestro gusto
         * y se aprovecha para mostrar una vista mientras realiza la tarea.
         */
        @Override
        protected void onPreExecute() {
            MapsActivity.this.noPuedoPulsarMenu = true;
            ipServidor = MapsActivity.this.obtenerIpPrefs();
            puerto = MapsActivity.this.obtenerPuertoServidor();
            vistaCargando = findViewById(R.id.layout_sincronizando);
            vistaCargando.setVisibility(View.VISIBLE);
        }

        /**
         * Método el cual se ejecuta en segundo plano, si queremos mostrar algo por el hilo principal
         * habría que llamar a publishProgress que este llama al onProgresUpdate. Aquí recuperamos las
         * ultimas posiciones.
         * <p>
         * No se utilizan métodos ni clases @Deprecated, se utiliza HttpURLConnection y para formar
         * la URL SringBuffer porque al estar manejando varios hilos que sea sincronizado.
         *
         * @param params
         * @return Las rutas obtenidas, si es null es que ha ocurrido un error.
         */
        @Override
        protected List<Ruta> doInBackground(Void... params) {
            List<Ruta> rutas = new ArrayList<>();
            StringBuffer url = new StringBuffer().append("http://")
                    .append(ipServidor).append(":").append(puerto)
                    .append("/ServicioRestAutobus/webresources/rutas/ultimasPosiciones");
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-type", "application/json");
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
            } catch (IOException e) {
                e.printStackTrace();
                rutas = null;
            } catch (JSONException e) {
                e.printStackTrace();
                rutas = null;
            }
            return rutas;
        }

        /**
         * Método que se ejecuta después del doInBackground, le llegan las rutas que devuelve el método anterior mencionado,
         * si llega como null es que algo ha salido mal, lo mostramos por pantalla, y ocultamos las vistas
         * que salían mientras cargaba.
         *
         * @param rutas Rutas obtenidas o null si ha ocurrido algún error.
         */
        @Override
        protected void onPostExecute(List<Ruta> rutas) {
            if (rutas == null) {
                Toast.makeText(MapsActivity.this, MapsActivity.this.getString(R.string.error_rest), Toast.LENGTH_SHORT).show();
            } else {
                MapsActivity.this.actualizarMapa(rutas);
            }
            vistaCargando.setVisibility(View.GONE);
            MapsActivity.this.noPuedoPulsarMenu = false;
        }
    }
}
