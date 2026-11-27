package com.example.gestionpointage;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.BreakAdapter;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.AttendanceRecord;
import com.example.gestionpointage.model.BreakRecord;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Pointage : Entrées, Sorties et Pauses multiples par jour.
 * Le Champ Pause affiche : Type, Entrée (Début), Sortie (Fin), et Durée.
 */
public class PointageActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper db;

    private TextView liveClock, liveDate, statusChip;
    private TextView arrivalValue, exitValue;
    private TextView btnArrival, btnExit, btnBreak;
    private TextView breakEmojiHeader, breakState;
    private TextView breakTypeValue, breakStartValue, breakEndValue, breakDurationValue;
    private RecyclerView breaksList;
    private TextView emptyBreaks;

    private BreakAdapter breakAdapter;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final List<BreakRecord> todayBreaks = new ArrayList<>();

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;

            liveClock.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date()));

            // La durée d'une pause en cours boucle en temps réel
            if (db.getOpenBreak(session.getUserId()) != null) {
                refresh();
            }

            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pointage);

        session = new SessionManager(this);
        db = new DatabaseHelper(this);

        liveClock = findViewById(R.id.liveClock);
        liveDate = findViewById(R.id.liveDate);
        statusChip = findViewById(R.id.statusChip);
        arrivalValue = findViewById(R.id.arrivalValue);
        exitValue = findViewById(R.id.exitValue);

        btnArrival = findViewById(R.id.btnArrival);
        btnExit = findViewById(R.id.btnExit);
        btnBreak = findViewById(R.id.btnBreak);

        breakEmojiHeader = findViewById(R.id.breakEmojiHeader);
        breakState = findViewById(R.id.breakState);
        breakTypeValue = findViewById(R.id.breakTypeValue);
        breakStartValue = findViewById(R.id.breakStartValue);
        breakEndValue = findViewById(R.id.breakEndValue);
        breakDurationValue = findViewById(R.id.breakDurationValue);

        breaksList = findViewById(R.id.breaksList);
        emptyBreaks = findViewById(R.id.emptyBreaks);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        breaksList.setLayoutManager(new LinearLayoutManager(this));
        breakAdapter = new BreakAdapter(todayBreaks, false);
        breaksList.setAdapter(breakAdapter);

        liveDate.setText(Ui.isoToLong(Ui.nowIso()));

        btnArrival.setOnClickListener(v -> onArrivalClick());
        btnExit.setOnClickListener(v -> onExitClick());
        btnBreak.setOnClickListener(v -> onBreakClick());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
    }

    // =========================================================
    //  Pointage Entrée
    // =========================================================

    private void onArrivalClick() {
        int userId = session.getUserId();

        if (db.isClockedIn(userId)) {
            AttendanceRecord today = db.getTodayAttendance(userId);
            String arrivalTime = (today != null && today.arrivalTime != null)
                    ? DatabaseHelper.shortTime(today.arrivalTime) : "";
            Ui.toast(this, "Vous êtes déjà en poste (Entrée à " + arrivalTime + "). Pointez votre sortie pour terminer cette session.");
        } else {
            boolean ok = db.markArrival(userId);
            Ui.toast(this, ok ? "Nouvelle entrée enregistrée avec succès 🌅" : "Impossible d'enregistrer l'entrée");
        }

        refresh();
    }

    // =========================================================
    //  Pointage Sortie
    // =========================================================

    private void onExitClick() {
        int userId = session.getUserId();

        if (!db.isClockedIn(userId)) {
            Ui.toast(this, "Vous n'avez pas d'entrée en cours. Pointez d'abord votre entrée !");
            return;
        }

        if (db.hasOpenBreak(userId)) {
            Ui.toast(this, "Terminez d'abord votre pause avant de pointer la sortie.");
            return;
        }

        boolean ok = db.markExit(userId);
        Ui.toast(this, ok ? "Sortie enregistrée 🌙. Vous pourrez re-pointer une entrée plus tard." : "Impossible d'enregistrer la sortie");

        refresh();
    }

    // =========================================================
    //  Pauses & Choisir Type de Pause
    // =========================================================

    private void onBreakClick() {
        int userId = session.getUserId();

        if (db.hasOpenBreak(userId)) {
            boolean ok = db.endBreak(userId);
            Ui.toast(this, ok ? "Pause terminée, bon travail ! 💪" : "Erreur lors de la fin de pause");
            refresh();
            return;
        }

        if (!db.isClockedIn(userId)) {
            boolean arrivalOk = db.markArrival(userId);
            if (arrivalOk) {
                Ui.toast(this, "Entrée enregistrée automatiquement 🌅");
            }
        }

        showBreakTypeDialog();
    }

    /** Dialogue de choix du type de pause */
    private void showBreakTypeDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_break_type, null);
        LinearLayout list = view.findViewById(R.id.breakTypeList);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        for (final String type : DatabaseHelper.BREAK_TYPES) {

            View row = LayoutInflater.from(this).inflate(R.layout.item_break_type, list, false);
            TextView emoji = row.findViewById(R.id.typeEmoji);
            TextView name = row.findViewById(R.id.typeName);

            switch (type) {
                case "Pause normale":
                    emoji.setText("⏸️");
                    break;
                case "Déjeuner":
                    emoji.setText("🍽️");
                    break;
                case "Café":
                    emoji.setText("☕");
                    break;
                case "Personnelle":
                    emoji.setText("💼");
                    break;
                case "Médicale":
                    emoji.setText("🏥");
                    break;
                case "Prière":
                    emoji.setText("🕌");
                    break;
                case "Pause exceptionnelle":
                    emoji.setText("⚡");
                    break;
                default:
                    emoji.setText("☕");
                    break;
            }

            name.setText(type);

            row.setOnClickListener(v -> {
                boolean ok = db.startBreak(session.getUserId(), type);
                Ui.toast(this, ok
                        ? "Pause " + type.toLowerCase() + " commencée ☕"
                        : "Impossible de démarrer la pause");
                dialog.dismiss();
                refresh();
            });

            list.addView(row);
        }

        dialog.show();
    }

    // =========================================================
    //  Affichage & Rafraîchissement du Champ Pause
    // =========================================================

    private void refresh() {

        int userId = session.getUserId();
        AttendanceRecord today = db.getTodayAttendance(userId);

        btnArrival.setEnabled(true);
        btnArrival.setAlpha(1f);

        btnExit.setEnabled(true);
        btnExit.setAlpha(1f);

        btnBreak.setEnabled(true);
        btnBreak.setAlpha(1f);

        if (today == null) {
            statusChip.setText("Non pointé");
            arrivalValue.setText("--:--");
            exitValue.setText("--:--");

            btnArrival.setText("🌅 Pointer Entrée");
            btnArrival.setBackgroundResource(R.drawable.btn_success);

            btnExit.setText("🌙 Pointer Sortie");
            btnExit.setBackgroundResource(R.drawable.btn_danger);

        } else if (db.isClockedIn(userId)) {
            // En session active
            statusChip.setText("En poste ✅");
            arrivalValue.setText(DatabaseHelper.shortTime(today.arrivalTime));
            exitValue.setText("en cours");

            btnArrival.setText("En poste (" + DatabaseHelper.shortTime(today.arrivalTime) + ") ✓");
            btnArrival.setBackgroundResource(R.drawable.btn_soft);

            btnExit.setText("🌙 Pointer Sortie");
            btnExit.setBackgroundResource(R.drawable.btn_danger);

        } else {
            // Session précédente clôturée (prêt pour une nouvelle entrée)
            statusChip.setText("Hors poste (Prêt pour nouvelle entrée)");
            arrivalValue.setText(DatabaseHelper.shortTime(today.arrivalTime));
            exitValue.setText(DatabaseHelper.shortTime(today.exitTime));

            btnArrival.setText("🌅 Pointer Nouvelle Entrée");
            btnArrival.setBackgroundResource(R.drawable.btn_success);

            btnExit.setText("Sortie enregistrée (" + DatabaseHelper.shortTime(today.exitTime) + ") ✓");
            btnExit.setBackgroundResource(R.drawable.btn_soft);
        }

        // Pause en cours ou dernière pause
        BreakRecord open = db.getOpenBreak(userId);
        if (open != null) {
            breakEmojiHeader.setText(open.getEmoji());
            breakState.setText("Pause " + open.type.toLowerCase() + " en cours");
            breakTypeValue.setText(open.type);
            breakStartValue.setText(DatabaseHelper.shortTime(open.startTime));
            breakEndValue.setText("En cours");
            breakDurationValue.setText(open.durationMinutes() + " min");

            btnBreak.setText("🛑 Fin de pause (" + open.type + ")");
            btnBreak.setBackgroundResource(R.drawable.btn_success);
        } else if (today != null && !today.breaks.isEmpty()) {
            BreakRecord last = today.breaks.get(0);
            breakEmojiHeader.setText(last.getEmoji());
            breakState.setText("Dernière pause : " + last.type);
            breakTypeValue.setText(last.type);
            breakStartValue.setText(DatabaseHelper.shortTime(last.startTime));
            breakEndValue.setText(DatabaseHelper.shortTime(last.endTime));
            breakDurationValue.setText(last.durationMinutes() + " min");

            btnBreak.setText("☕ Prenez une pause (Choisir le type)");
            btnBreak.setBackgroundResource(R.drawable.btn_primary);
        } else {
            breakEmojiHeader.setText("☕");
            breakState.setText("Aucune pause enregistrée");
            breakTypeValue.setText("Aucun");
            breakStartValue.setText("--:--");
            breakEndValue.setText("--:--");
            breakDurationValue.setText("0 min");

            btnBreak.setText("☕ Prenez une pause (Choisir le type)");
            btnBreak.setBackgroundResource(R.drawable.btn_primary);
        }

        // Liste des pauses du jour
        todayBreaks.clear();
        if (today != null) {
            todayBreaks.addAll(today.breaks);
        }

        breakAdapter.update(new ArrayList<>(todayBreaks));
        emptyBreaks.setVisibility(todayBreaks.isEmpty() ? View.VISIBLE : View.GONE);
        breaksList.setVisibility(todayBreaks.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
