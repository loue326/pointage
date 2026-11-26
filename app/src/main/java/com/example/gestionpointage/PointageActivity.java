package com.example.gestionpointage;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
 * Pointage : arrivée, sortie, début/fin de pause et pauses du jour.
 */
public class PointageActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper db;

    private TextView liveClock, liveDate, statusChip;
    private TextView arrivalValue, exitValue, btnClock;
    private TextView breakState, btnBreak;
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
        btnClock = findViewById(R.id.btnClock);
        breakState = findViewById(R.id.breakState);
        btnBreak = findViewById(R.id.btnBreak);
        breaksList = findViewById(R.id.breaksList);
        emptyBreaks = findViewById(R.id.emptyBreaks);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        breaksList.setLayoutManager(new LinearLayoutManager(this));
        breakAdapter = new BreakAdapter(todayBreaks, false);
        breaksList.setAdapter(breakAdapter);

        liveDate.setText(Ui.isoToLong(Ui.nowIso()));

        btnClock.setOnClickListener(v -> onClockClick());
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
    //  Pointage entrée / sortie
    // =========================================================

    private void onClockClick() {
        int userId = session.getUserId();

        if (!db.hasAttendanceToday(userId)) {
            boolean ok = db.markArrival(userId);
            Ui.toast(this, ok ? "Arrivée enregistrée 🌅" : "Impossible d'enregistrer l'arrivée");
        } else if (db.isClockedIn(userId)) {
            if (db.hasOpenBreak(userId)) {
                Ui.toast(this, "Terminez d'abord votre pause avant de pointer la sortie.");
                return;
            }
            boolean ok = db.markExit(userId);
            Ui.toast(this, ok ? "Sortie enregistrée 🌙" : "Impossible d'enregistrer la sortie");
        } else {
            Ui.toast(this, "Vous avez déjà terminé votre journée.");
        }

        refresh();
    }

    // =========================================================
    //  Pauses
    // =========================================================

    private void onBreakClick() {
        int userId = session.getUserId();

        if (!db.isClockedIn(userId)) {
            Ui.toast(this, "Pointez d'abord votre arrivée pour prendre une pause.");
            return;
        }

        if (db.hasOpenBreak(userId)) {
            boolean ok = db.endBreak(userId);
            Ui.toast(this, ok ? "Pause terminée, bon travail ! 💪" : "Erreur lors de la fin de pause");
            refresh();
        } else {
            showBreakTypeDialog();
        }
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
                default:
                    emoji.setText("🕌");
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
    //  Affichage
    // =========================================================

    private void refresh() {

        int userId = session.getUserId();
        AttendanceRecord today = db.getTodayAttendance(userId);

        if (today == null) {
            statusChip.setText("Non pointé");
            arrivalValue.setText("--:--");
            exitValue.setText("--:--");
            btnClock.setText(R.string.btn_arrivee);
            btnClock.setBackgroundResource(R.drawable.btn_success);
            btnClock.setEnabled(true);
            btnClock.setAlpha(1f);
        } else {
            arrivalValue.setText(DatabaseHelper.shortTime(today.arrivalTime));
            exitValue.setText(today.exitTime == null
                    ? "en cours" : DatabaseHelper.shortTime(today.exitTime));

            if (today.exitTime == null) {
                statusChip.setText("En poste");
                btnClock.setText(R.string.btn_sortie);
                btnClock.setBackgroundResource(R.drawable.btn_danger);
                btnClock.setEnabled(true);
                btnClock.setAlpha(1f);
            } else {
                statusChip.setText("Journée terminée");
                btnClock.setText("Journée terminée ✓");
                btnClock.setBackgroundResource(R.drawable.btn_soft);
                btnClock.setEnabled(false);
                btnClock.setAlpha(0.7f);
            }
        }

        // Pause en cours
        BreakRecord open = db.getOpenBreak(userId);
        if (open != null) {
            breakState.setText("Pause " + open.type.toLowerCase()
                    + " depuis " + DatabaseHelper.shortTime(open.startTime)
                    + " (" + open.durationMinutes() + " min)");
            btnBreak.setText(R.string.btn_end_break);
            btnBreak.setBackgroundResource(R.drawable.btn_success);
        } else {
            breakState.setText(db.isClockedIn(userId)
                    ? "Aucune pause en cours"
                    : "Pointez votre arrivée pour pouvoir prendre une pause");
            btnBreak.setText(R.string.btn_start_break);
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
