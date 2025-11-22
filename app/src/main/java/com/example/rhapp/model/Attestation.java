// Attestation.java
package com.example.rhapp.model;

import java.util.Date;

public class Attestation {
    private String id;
    private String employeId;
    private String employeNom;
    private String employeDepartement;
    private String typeAttestation;
    private String motif;
    private String statut; // "en_attente", "approuvee", "refusee"
    private Date dateDemande;
    private Date dateTraitement;
    private String motifRefus;
    private String pdfUrl;

    // Constructeur vide REQUIS pour Firebase
    public Attestation() {}

    // Constructeur pour nouvelles demandes
    public Attestation(String employeId, String employeNom, String employeDepartement,
                       String typeAttestation, String motif) {
        this.employeId = employeId;
        this.employeNom = employeNom;
        this.employeDepartement = employeDepartement;
        this.typeAttestation = typeAttestation;
        this.motif = motif;
        this.statut = "en_attente";
        this.dateDemande = new Date();
    }

    // Getters et setters pour tous les champs
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeId() { return employeId; }
    public void setEmployeId(String employeId) { this.employeId = employeId; }

    public String getEmployeNom() { return employeNom; }
    public void setEmployeNom(String employeNom) { this.employeNom = employeNom; }

    public String getEmployeDepartement() { return employeDepartement; }
    public void setEmployeDepartement(String employeDepartement) { this.employeDepartement = employeDepartement; }

    public String getTypeAttestation() { return typeAttestation; }
    public void setTypeAttestation(String typeAttestation) { this.typeAttestation = typeAttestation; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getDateDemande() { return dateDemande; }
    public void setDateDemande(Date dateDemande) { this.dateDemande = dateDemande; }

    public Date getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(Date dateTraitement) { this.dateTraitement = dateTraitement; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
}