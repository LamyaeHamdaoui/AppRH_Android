package com.example.rhapp;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DailyPresenceUpdateWorker extends Worker {

    private static final String TAG = "DailyUpdateWorker";
    private static final String COLLECTION_USERS = "Users";
    private static final String COLLECTION_PRESENCE = "PresenceHistory";

    public DailyPresenceUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Démarrage de la tâche de mise à jour quotidienne.");

        // Récupérer la date du jour (qui est le nouveau jour après minuit)
        SimpleDateFormat rawFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String todayDate = rawFormat.format(new Date());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference usersCollection = db.collection(COLLECTION_USERS);

        // Récupérer tous les employés
        usersCollection.get().addOnSuccessListener(queryDocumentSnapshots -> {

            // Pour chaque employé, créer une entrée "unmarked" pour le nouveau jour
            for (QueryDocumentSnapshot userDoc : queryDocumentSnapshots) {
                String userId = userDoc.getId();

                Map<String, Object> presenceEntry = new HashMap<>();
                presenceEntry.put("userId", userId);
                presenceEntry.put("date", todayDate);
                // Statut par défaut : Non marqué, qui sera considéré absent si non mis à jour
                presenceEntry.put("status", "unmarked");
                // Utilisez com.google.firebase.Timestamp.now() si vous avez les dépendances Firestore complètes
                // Sinon, utilisez un champ de type String ou Long.
                // Ici, on utilise un placeholder pour simplifier.
                // presenceEntry.put("timestamp", new Date().getTime());

                // Utiliser userId_date comme ID de document pour garantir l'unicité
                db.collection(COLLECTION_PRESENCE)
                        .document(userId + "_" + todayDate)
                        .set(presenceEntry)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Présence initialisée pour " + userId + " le " + todayDate))
                        .addOnFailureListener(e -> Log.e(TAG, "Échec de l'initialisation pour " + userId, e));
            }
        });

        // Succès de la tâche (même si Firestore est asynchrone, la planification est lancée)
        return Result.success();
    }
}