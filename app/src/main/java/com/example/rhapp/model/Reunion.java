package com.example.rhapp.model;

public class Reunion {

    private String titre;
    private String date;
    private String heure;
    private String departement;
    private String description;

    //  Constructeur vide obligatoire pour Firestore
    public Reunion() {}

    //  Constructeur avec args
    public Reunion(String titre, String date, String heure, String departement, String description) {
        this.titre = titre;
        this.date = date;
        this.heure = heure;
        this.departement = departement;
        this.description = description;
    }

    // Getters et setters
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHeure() { return heure; }
    public void setHeure(String heure) { this.heure = heure; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


    private String participants;

    public String getParticipants() { return participants; }
    public void setParticipants(String participants) { this.participants = participants; }


}
