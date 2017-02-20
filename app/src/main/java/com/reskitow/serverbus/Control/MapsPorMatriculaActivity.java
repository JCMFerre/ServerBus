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

/**
 * Activity que gestiona con un spinner las rutas a mostrar (Polyline).
 */
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

    /**
     * Obtiene el spinner con el método findViewById y lo guarda en la variable global spinner.
     */
    private void findViews() {
        spinner = (Spinner) findViewById(R.id.spinner_sesiones);
    }

    /**
     * Crea y guarda una instancia de la clase BDRutasSQLite para poder gestionar la BD interna.
     */
    private void inicializarBDLocal() {
        bdRutasSQLite = new BDRutasSQLite(this);
    }

    /**
     * Cada vez que consulta al servidor Rest, guarda la hora de sincronización en las preferencias
     * si al obtener la hora no da null, significa que ya se ha consultado, por tanto muestra los
     * datos locales que tiene en la BD interna, en caso de querer refrescar los datos tan solo se tiene
     * que dar al botón de la toolbar.
     * <p>
     * Si no hay ningún dato que pueda utilizar en la BD interna lanza la tarea asíncrona, si hay algún dato
     * muestra un Toast con la última fecha de sincronización.
     */
    private void comprobarSincronizacion() {
        String ultimaSync = obtenerUltimaSincronizacionPorMatricula();
        if (ultimaSync == null || ultimaSync.equals("null")) {
            lanzarSincronizacion();
        } else {
            sincronizado = true;
            Toast.makeText(this, getString(R.string.ultima_sinc_toast_info) + ultimaSync + getString(R.string.ultima_sinc_toast_info2), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Lanza la tarea asíncrona.
     */
    private void lanzarSincronizacion() {
        new ObtenerTodasLasRutasPorMatricula().execute();
    }

    /**
     * Devuelve la última hora de sincronización o null si nunca se ha sincronizado.
     *
     * @return Última fecha de sincronización o null si no existe.
     */
    private String obtenerUltimaSincronizacionPorMatricula() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.ultima_sinc) + matricula, null);
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
     * Añade o actualiza si ya existe la última fecha de sincronización.
     * <p>
     * La key de las preferencias es por defecto para todas, ultima_sync_{MATRICULA}. El campo
     * que cambia es {MATRICULA} por la matricula que esté utilizando actualmente.
     */
    private void anadirUltimaSincronizacionPorMatricula() {
        String ultimaFechaSinc = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date().getTime());
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.ultima_sinc) + matricula, ultimaFechaSinc);
        editor.commit();
    }

    /**
     * Obtiene la matricula del intent que ha lanzado la activity principal a través del título del marcador.
     */
    private void obtenerMatriculaIntent() {
        if (matricula == null) {
            matricula = getIntent().getStringExtra("matricula");
        }
    }

    /**
     * Obtiene la IP seteada en las preferencias.
     *
     * @return La IP en cuestión o null si no existe.
     */
    public String obtenerIpPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(this.getString(R.string.key_ip_servidor), null);
    }

    /**
     * Método que actualiza la interfaz de usuario, obtiene las rutas de la BD interna, (Al sincronizar
     * se guardan las rutas en la interna para posteriormente poder utilizarlas).
     * <p>
     * Crea un adaptador a partir del id de sesión y lo llama al método cargarSpinner(adaptador).
     */
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

    /**
     * Carga el array de String's en un adaptador y lo setea al spinner, además hace visible al layout
     * que contiene el spinner para que el usuario pueda interactuar con él.
     *
     * @param adapter Array de strings que contendrá el adaptador.
     */
    private void cargarSpinner(String[] adapter) {
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, adapter);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(this);
        findViewById(R.id.layout_con_spinner).setVisibility(View.VISIBLE);
    }

    /**
     * Método que carga el mapa listo, si las rutas están sincronizadas llama al método actualizarUI();
     *
     * @param googleMap Mapa cargado y listo para usar.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (sincronizado) {
            actualizarUI();
        }
    }

    /**
     * Infla el menú.
     *
     * @param menu
     * @return true por defecto.
     */
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

    /**
     * Listener del spinner, si se selecciona una opción que no es la primera (Selecciona un ...), carga
     * la ruta con el id del item seleccionado llamando al método cargarMapaConSesion.
     *
     * @param parent   Adaptador del spinner
     * @param view     Vista seleccionada
     * @param position Posición del adaptador pulsada.
     * @param id       Id de la vista.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != 0) {
            cargarMapaConSesion(parent.getSelectedItem().toString());
        }
    }

    /**
     * Añade la polyline llamando al método anadirPolyLine y le pasa un lista de rutas recuperadas
     * de la BD interna filtrada por el idSesion que le llega.
     *
     * @param idSesion ID de sesión con la que filtrara la ruta.
     */
    private void cargarMapaConSesion(String idSesion) {
        anadirPolyline(bdRutasSQLite.obtenerRutasPorIdSesion(idSesion));
    }

    /**
     * Añade una polyline entre todas las posiciones de la ruta, el color es generado aleatoriamente
     * y las posiciones se obtienen con una lista de objetos LatLng que setean la latitud y longitud de
     * cada ruta, por último mueve la cámara a la última LatLng creada.
     *
     * @param rutas Rutas de las cuales se obtendrán la latitud y longitud, y crearan los objetos LatLng.
     */
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
        // Obliga a implementar y no le encuentro funcionalidad...
    }

    /**
     * Clase que recupera todas las rutas relacionadas con una matrícula.
     */
    private class ObtenerTodasLasRutasPorMatricula extends AsyncTask<Void, Void, List<Ruta>> {

        private String matriculaTemp;
        private String puerto;
        private String ipServidor;
        private View vistaCargando;

        /**
         * Método que se ejecuta en el hilo principal, aquí seteamos los atributos a nuestro gusto
         * y se aprovecha para mostrar una vista mientras realiza la tarea.
         */
        @Override
        protected void onPreExecute() {
            MapsPorMatriculaActivity.this.noPuedoPulsarMenu = true;
            this.matriculaTemp = MapsPorMatriculaActivity.this.matricula;
            puerto = MapsPorMatriculaActivity.this.obtenerPuertoServidor();
            ipServidor = MapsPorMatriculaActivity.this.obtenerIpPrefs();
            vistaCargando = MapsPorMatriculaActivity.this.findViewById(R.id.layout_sincronizando_por_matricula);
            vistaCargando.setVisibility(View.VISIBLE);
        }

        /**
         * Método el cual se ejecuta en segundo plano, si queremos mostrar algo por el hilo principal
         * habría que llamar a publishProgress que este llama al onProgresUpdate. Aquí recuperamos las
         * rutas relacionadas con la matricula.
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
            try {
                StringBuffer url = new StringBuffer().append("http://").append(ipServidor).append(":")
                        .append(puerto).append("/ServicioRestAutobus/webresources/rutas/todasLasRutas/%7B\"matricula\":\"")
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

        /**
         * Método que se ejecuta después del doInBackground, le llegan las rutas que devuelve el método anterior mencionado,
         * si llega como null es que algo ha salido mal, lo mostramos por pantalla, y ocultamos las vistas
         * que salían mientras cargaba.
         * <p>
         * Si no ha ocurrido ningún error añade las rutas a la BD interna.
         *
         * @param rutas Rutas obtenidas o null si ha ocurrido algún error.
         */
        @Override
        protected void onPostExecute(List<Ruta> rutas) {
            MapsPorMatriculaActivity.this.noPuedoPulsarMenu = false;
            vistaCargando.setVisibility(View.GONE);
            MapsPorMatriculaActivity.this.sincronizado = true;
            if (rutas == null) {
                Toast.makeText(MapsPorMatriculaActivity.this, MapsPorMatriculaActivity.this.getString(R.string.error_rest)
                        , Toast.LENGTH_SHORT).show();
            } else {
                MapsPorMatriculaActivity.this.bdRutasSQLite.anadirRutas(rutas);
                MapsPorMatriculaActivity.this.anadirUltimaSincronizacionPorMatricula();
                MapsPorMatriculaActivity.this.actualizarUI();
            }
        }
    }

}
