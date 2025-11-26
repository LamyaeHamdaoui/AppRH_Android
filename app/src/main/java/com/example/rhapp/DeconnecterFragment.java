package com.example.rhapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DeconnecterFragment extends DialogFragment {

    // 1. Interface de communication
    // Cette interface permet au Fragment de "notifier" l'Activité (ProfileActivity)
    // que la déconnexion a été confirmée.
    public interface LogoutListener {
        void onLogoutConfirmed();
    }

    private LogoutListener listener;

    // 2. Attachement de l'écouteur à l'activité hôte
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Assurez-vous que l'Activity implémente l'interface
            listener = (LogoutListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " doit implémenter LogoutListener");
        }
        // Pour un DialogFragment, il est souvent préférable de définir un style de dialogue
        // C'est pourquoi on utilise setStyle.
        // Correction : Utilisez androidx.appcompat.R.style
        setStyle(DialogFragment.STYLE_NO_TITLE, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);
    }

    // 3. Création de la vue du fragment
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate la mise en page XML que vous avez fournie
        View view = inflater.inflate(R.layout.fragment_deconnecter, container, false);

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        // Gestion du clic sur Annuler
        btnCancel.setOnClickListener(v -> {
            // Ferme le dialogue sans rien faire
            dismiss();
        });

        // Gestion du clic sur Se déconnecter
        btnLogout.setOnClickListener(v -> {
            // 1. Notifie l'activité que l'utilisateur a confirmé
            if (listener != null) {
                listener.onLogoutConfirmed();
            }
            // 2. Ferme le dialogue
            dismiss();
        });

        return view;
    }
}