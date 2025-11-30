package com.example.rhapp.model;

import com.google.firebase.Timestamp;

public class History {

    private String date;
    private String status;
    private String details;
    private String time;
    private String justification;
    private Timestamp timestamp;

    // Constructeur par défaut (obligatoire pour Firestore)
    public History() {
    }

    // Constructeur principal
    public History(String date, String status, String details, String time, String justification, Timestamp timestamp) {
        this.date = date;
        this.status = status;
        this.details = details;
        this.time = time;
        this.justification = justification;
        this.timestamp = timestamp;
    }

    // Getters
    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    public String getTime() {
        return time;
    }
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getJustification() {
        return justification;
    }
}
