package es.deusto.onthestreetapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class SQLitePlaceHelper extends SQLiteOpenHelper{

    // Database name and version
    private static final String DATABASE_NAME = "places_db.db";
    private static final int DATABASE_VERSION = 8;

    // Table and columns names (_id is required as primary key)
    private static final String TABLE_PLACES= "places_db";
    private static final String TABLE_CONTACTS= "contacts";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "nombre";
    private static final String COLUMN_ADDRESS = "direccion";
    private static final String COLUMN_LAT = "latitud";
    private static final String COLUMN_LNG = "longitud";
    private static final String COLUMN_DESC = "descripcion";
    private static final String COLUMN_IMAGEURI = "image";

    private static final String COLUMN_CONTACT_ID = "_id";
    private static final String COLUMN_CONTACT_PLACEID = "idplace";
    private static final String COLUMN_CONTACT_NAME = "name";
    private static final String COLUMN_CONTACT_DESC = "description";
    private static final String COLUMN_CONTACT_TEL = "phone";

    // SQL sentence to create the tables
    private static final String DATABASE_CREATE = "create table "
                + TABLE_PLACES + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME
            + " text not null, " + COLUMN_ADDRESS+ " text not null," + COLUMN_LAT+ " real,"+ COLUMN_LNG+ " real,"
            + COLUMN_DESC+ " text not null, "+ COLUMN_IMAGEURI+ " text);";

    private static final String DATABASE_CONTACTS_CREATE = "create table contacts ("+COLUMN_CONTACT_ID+" integer primary key autoincrement, "+
            ""+COLUMN_CONTACT_PLACEID+" integer, "+COLUMN_CONTACT_NAME+" text not null, "+COLUMN_CONTACT_DESC +" text not null, "+COLUMN_CONTACT_TEL+" text not null);";

    public SQLitePlaceHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    // Executed when creating the DB for first time
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
        db.execSQL(DATABASE_CONTACTS_CREATE);
    }

    // Executed when upgrading to a new version
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("StudentDatabase", "Upgrading database from version " + oldVersion + " to " + newVersion + ", deleting old data, creating empty table.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_CONTACTS);
        onCreate(db);
    }

    // Convenience method to store a student in the database
    public int addPlace(Place place){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, place.getNombre());
        values.put(COLUMN_ADDRESS, place.getDireccion());
        values.put(COLUMN_LAT, place.getLat());
        values.put(COLUMN_LNG, place.getLng());
        values.put(COLUMN_DESC, place.getDesc());
        values.put(COLUMN_IMAGEURI, place.getImageURI());

        return (int) db.insert(TABLE_PLACES, null, values);
    }

    // Convenience method to retrieve all the students from the database
    public ArrayList<Place> getPlaces(){
        ArrayList<Place> as = new ArrayList<Place>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_PLACES,
                new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_ADDRESS, COLUMN_LAT,COLUMN_LNG,COLUMN_DESC, COLUMN_IMAGEURI},
                null,
                null,
                null,
                null,
                null);

        // Alternative
        //Cursor cursor = db.rawQuery("select "+ COLUMN_NAME + "," + COLUMN_PHONE_NUMNER + " from " + TABLE_PLACES,null);

        cursor.moveToNext();
        while(!cursor.isAfterLast()){
            int id = cursor.getInt(0);
            String nombre = cursor.getString(1);
            String direccion = cursor.getString(2);
            double lat = cursor.getDouble(3);
            double lng = cursor.getDouble(4);
            String desc = cursor.getString(5);
            String URI = cursor.getString(6);
            as.add(new Place(direccion,nombre,id, lat,lng, desc, URI));

            cursor.moveToNext();
        }
        return as;
    }

    public boolean updatePlace(Place place){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, place.getNombre());
        values.put(COLUMN_ADDRESS, place.getDireccion());
        values.put(COLUMN_LAT, place.getLat());
        values.put(COLUMN_LNG, place.getLng());
        values.put(COLUMN_DESC, place.getDesc());
        values.put(COLUMN_IMAGEURI, place.getImageURI());

        int rows = db.update(TABLE_PLACES, values, "_id="+place.getId(), null);

        return rows!=0;
    }

    public boolean deletePlace(Place place){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PLACES, "_id="+place.getId(), null) > 0;
    }

    public Place getPlace(int id){
        SQLiteDatabase db = getReadableDatabase();
        String where = COLUMN_ID + " = ?";
        String[] whereArgs = {id+""};
        Cursor cursor = db.query(TABLE_PLACES, null, where, whereArgs, null, null, null);
        Place obj = null;
        try {
            if (cursor.moveToFirst()) {
                int id2 = cursor.getInt(0);
                String nombre = cursor.getString(1);
                String direccion = cursor.getString(2);
                double lat = cursor.getDouble(3);
                double lng = cursor.getDouble(4);
                String desc = cursor.getString(5);
                String uri = cursor.getString(6);
                obj = new Place(direccion,nombre,id2, lat,lng, desc, uri);
            }
        } finally {
            cursor.close();
        }
        return obj;
    }

    public int countContacts(int id){
        String countQuery = "SELECT  * FROM " + TABLE_CONTACTS+" WHERE idplace="+ id;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        Log.i("Contador", cursor.getCount()+"");
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

    public ArrayList<Contact> getContacts(Place p){
        ArrayList<Contact> as = new ArrayList<Contact>();

        SQLiteDatabase db = this.getReadableDatabase();
        String where = COLUMN_CONTACT_PLACEID + " = ?";
        String[] whereArgs = {p.getId()+""};

        Cursor cursor = db.query(
                TABLE_CONTACTS,
                new String[]{COLUMN_CONTACT_ID, COLUMN_CONTACT_PLACEID, COLUMN_CONTACT_NAME, COLUMN_CONTACT_DESC, COLUMN_CONTACT_TEL},
                where,
                whereArgs,
                null,
                null,
                null);

        cursor.moveToNext();
        while(!cursor.isAfterLast()){
            int id = cursor.getInt(0);
            int id_place = cursor.getInt(1);
            String nombre = cursor.getString(2);
            String desc = cursor.getString(3);
            String tel = cursor.getString(4);
            as.add(new Contact(nombre, id_place, id, desc, tel));

            cursor.moveToNext();
        }
        return as;
    }

    public Contact addContact(Contact c){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTACT_PLACEID, c.getId_place());
        values.put(COLUMN_CONTACT_NAME, c.getName());
        values.put(COLUMN_CONTACT_DESC, c.getDescription());
        values.put(COLUMN_CONTACT_TEL, c.getPhone());
        int id = (int) db.insert(TABLE_CONTACTS, null, values);
        c.setId(id);
        return c;
    }

    public boolean deleteContact(Contact contact){
        SQLiteDatabase db = this.getWritableDatabase();
        Log.i("ID", contact.getId()+"");
        return db.delete(TABLE_CONTACTS, "_id="+contact.getId(), null) > 0;
    }
}
