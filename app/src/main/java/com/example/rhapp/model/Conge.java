package com.example.rhapp.model;

import java.util.Date;

public class Conge {
    private String id;
    private String userId;
    private String userName;
    private String userDepartment;
    private String typeConge;
    private Date dateDebut;
    private Date dateFin;
    private int duree;
    private String motif;
    private String statut;
    private Date dateDemande;

    // Constructeur vide OBLIGATOIRE pour Firestore
    public Conge() {
    }

    public Conge(String userId, String userName, String userDepartment, String typeConge,
                 Date dateDebut, Date dateFin, int duree, String motif, String statut) {
        this.userId = userId;
        this.userName = userName;
        this.userDepartment = userDepartment;
        this.typeConge = typeConge;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.duree = duree;
        this.motif = motif;
        this.statut = statut;
        this.dateDemande = new Date();
    }

    // Getters et Setters (TOUS doivent être présents)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserDepartment() { return userDepartment; }
    public void setUserDepartment(String userDepartment) { this.userDepartment = userDepartment; }

    public String getTypeConge() { return typeConge; }
    public void setTypeConge(String typeConge) { this.typeConge = typeConge; }

    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }

    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getDateDemande() { return dateDemande; }
    public void setDateDemande(Date dateDemande) { this.dateDemande = dateDemande; }
}