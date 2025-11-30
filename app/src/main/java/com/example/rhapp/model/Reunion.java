package com.example.rhapp.model;

public class Reunion {

    private String id;
    private String titre;
    private String date;
    private String heure;
    private String lieu;
    private String departement;
    private String description;
    private String participants;
    private int confirmedCount; // compteur des confirmations
    private boolean confirmed = false; // indique si l'employé a confirmé
    private String planifiePar; // nom ou ID du RH qui a planifié

    // Constructeur vide obligatoire pour Firestore
    public Reunion() {}

    // Constructeur sans ID, avec planifiePar
    public Reunion(String titre, String date, String heure, String lieu, String departement, String description, String planifiePar) {
        this.titre = titre;
        this.date = date;
        this.heure = heure;
        this.lieu = lieu;
        this.departement = departement;
        this.description = description;
        this.planifiePar = planifiePar;
    }

    // Constructeur avec ID
    public Reunion(String id, String titre, String date, String heure, String lieu, String departement, String description, String planifiePar) {
        this.id = id;
        this.titre = titre;
        this.date = date;
        this.heure = heure;
        this.lieu = lieu;
        this.departement = departement;
        this.description = description;
        this.planifiePar = planifiePar;
    }

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getHeure() { return heure; }
    public void setHeure(String heure) { this.heure = heure; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getParticipants() { return participants; }
    public void setParticipants(String participants) { this.participants = participants; }

    public int getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(int confirmedCount) { this.confirmedCount = confirmedCount; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public String getPlanifiePar() { return planifiePar; }
    public void setPlanifiePar(String planifiePar) { this.planifiePar = planifiePar; }

    private String leaderNomComplet;

    public String getLeaderNomComplet() {
        return leaderNomComplet;
    }

    public void setLeaderNomComplet(String leaderNomComplet) {
        this.leaderNomComplet = leaderNomComplet;
    }



    private int participantsCount = 0;

    public int getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }

}
