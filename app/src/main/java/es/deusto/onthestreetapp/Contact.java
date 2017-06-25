package es.deusto.onthestreetapp;

/**
 * Created by kevin on 19/3/17.
 */

public class Contact {

    private int id;
    private int id_place;
    private String phone;
    private String name;
    private String description;

    public Contact(String name, int id_place, int id, String description, String numero) {
        this.name = name;
        this.id_place = id_place;
        this.id = id;
        this.description = description;
        this.phone = numero;
    }

    public Contact(String name, int id_place, String description, String numero){
        this.name = name;
        this.id_place = id_place;
        this.description = description;
        this.phone = numero;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId_place() {
        return id_place;
    }

    public void setId_place(int id_place) {
        this.id_place = id_place;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
