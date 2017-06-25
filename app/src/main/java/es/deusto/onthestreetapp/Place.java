package es.deusto.onthestreetapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by kevin on 11/3/17.
 */

public class Place implements Serializable, Cloneable{

    private int id;
    private String nombre;
    private String direccion;
    private double lat =0.0;
    private double lng =0.0;
    private String desc ;
    private String imageURI;
    private ArrayList<Contact> lcontacts;
    private double comparedDistance = 0;

    public Place(String direccion, String nombre, int id, double lat, double lng, String desc) {
        this.direccion = direccion;
        this.nombre = nombre;
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.desc = desc;
    }

    public Place(String direccion, String nombre, int id, double lat, double lng, String desc, String imageURI) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.lat = lat;
        this.lng = lng;
        this.desc = desc;
        this.imageURI = imageURI;
    }

    public Place(String direccion, String nombre, double lat, double lng, String desc) {
        this.direccion = direccion;
        this.nombre = nombre;
        this.lat = lat;
        this.lng = lng;
        this.desc = desc;
    }

    public Place(String nombre, String direccion, String desc) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImageURI() {
        return imageURI;
    }

    public void setImageURI(String imageURI) {
        this.imageURI = imageURI;
    }

    public double getComparedDistance() {
        return comparedDistance;
    }

    public void setComparedDistance(double comparedDistance) {
        this.comparedDistance = comparedDistance;
    }
}
