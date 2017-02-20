package com.reskitow.serverbus.BD;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.reskitow.serverbus.Model.Ruta;

import java.util.ArrayList;
import java.util.List;

public class BDRutasSQLite extends SQLiteOpenHelper {

    private Context context;

    private static final String NOMBRE_BD = "DB_RUTAS";
    private static final String TABLA_RUTAS = "RUTAS_AUTOBUSES";
    private static final String KEY_IDSESION_RUTAS = "ID_SESION";
    private static final String KEY_MATRICULA_RUTAS = "MATRICULA";
    private static final String KEY_FECHA_RUTAS = "FECHA";
    private static final String KEY_LATITUD_RUTAS = "LATITUD";
    private static final String KEY_LONGITUD_RUTAS = "LONGITUD";

    public BDRutasSQLite(Context context) {
        super(context, NOMBRE_BD, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLA_RUTAS + " (" + KEY_IDSESION_RUTAS + " TEXT, "
                + KEY_MATRICULA_RUTAS + " TEXT, " + KEY_FECHA_RUTAS + " TEXT , "
                + KEY_LATITUD_RUTAS + " REAL, " + KEY_LONGITUD_RUTAS + " REAL)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //
    }

    public void anadirRutas(List<Ruta> rutas) {
        if (!rutas.isEmpty()) {
            SQLiteDatabase db = this.getWritableDatabase();
            Ruta rutaTemp = rutas.get(rutas.size() - 1);
            eliminarRegistrosTempPorMatricula(rutaTemp.getMatricula(), db);
            for (Ruta ruta : rutas) {
                db.insert(TABLA_RUTAS, null, prepararContentValuesRuta(ruta));
            }
            db.close();
        }
    }

    private void eliminarRegistrosTempPorMatricula(String matricula, SQLiteDatabase db) {
        db.delete(TABLA_RUTAS, KEY_MATRICULA_RUTAS + " = ?", new String[]{matricula});
    }

    private ContentValues prepararContentValuesRuta(Ruta ruta) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_IDSESION_RUTAS, ruta.getIdSesion());
        contentValues.put(KEY_MATRICULA_RUTAS, ruta.getMatricula());
        contentValues.put(KEY_FECHA_RUTAS, ruta.getFecha());
        contentValues.put(KEY_LATITUD_RUTAS, ruta.getLatitud());
        contentValues.put(KEY_LONGITUD_RUTAS, ruta.getLongitud());
        return contentValues;
    }

    public List<Ruta> obtenerRutasPorIdSesion(String idSesion) {
        List<Ruta> rutas = new ArrayList<>();
        String query = "SELECT * FROM " + TABLA_RUTAS + " WHERE " + KEY_IDSESION_RUTAS + " = ? ORDER BY " + KEY_FECHA_RUTAS;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{idSesion});
        if (cursor.moveToFirst()) {
            do {
                rutas.add(obtenerRutaPorCursor(cursor));
            } while (cursor.moveToNext());
        }
        return rutas;
    }

    public List<Ruta> obtenerSesionesDistinct(String matricula) {
        List<Ruta> rutas = new ArrayList<>();
        String query = "SELECT * FROM " + TABLA_RUTAS + " WHERE " + KEY_MATRICULA_RUTAS + " = ? GROUP BY " + KEY_IDSESION_RUTAS + " ORDER BY " + KEY_FECHA_RUTAS;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{matricula.toUpperCase()});
        if (cursor.moveToFirst()) {
            do {
                rutas.add(obtenerRutaPorCursor(cursor));
            } while (cursor.moveToNext());
        }
        return rutas;
    }

    private Ruta obtenerRutaPorCursor(Cursor cursor) {
        return new Ruta(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getDouble(4));
    }
}
