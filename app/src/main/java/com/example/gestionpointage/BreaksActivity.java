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
 * Écran dédié aux Pauses : Choisir le type de pause, voir l'entrée/sortie de pause et la durée.
 */
public class BreaksActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper db;

    private TextView liveClock, liveDate;
    private TextView btnBreak;
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

            if (db.getOpenBreak(session.getUserId()) != null) {
                refresh();
            }

            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breaks);

        session = new SessionManager(this);
        db = new DatabaseHelper(this);

        liveClock = findViewById(R.id.liveClock);
        liveDate = findViewById(R.id.liveDate);

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

    private void onBreakClick() {
        int userId = session.getUserId();

        if (db.hasOpenBreak(userId)) {
            boolean ok = db.endBreak(userId);
            Ui.toast(this, ok ? "Pause terminée, bon travail ! 💪" : "Erreur lors de la fin de pause");
            refresh();
            return;
        }

        // Si l'employé n'a pas pointé son arrivée, on l'auto-pointe pour qu'il puisse prendre sa pause
        if (!db.hasAttendanceToday(userId)) {
            db.markArrival(userId);
        }

        showBreakTypeDialog();
    }

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

    private void refresh() {
        int userId = session.getUserId();
        AttendanceRecord today = db.getTodayAttendance(userId);

        btnBreak.setEnabled(true);
        btnBreak.setAlpha(1f);

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

        todayBreaks.clear();
        if (today != null) {
            todayBreaks.addAll(today.breaks);
        }

        breakAdapter.update(new ArrayList<>(todayBreaks));
        emptyBreaks.setVisibility(todayBreaks.isEmpty() ? View.VISIBLE : View.GONE);
        breaksList.setVisibility(todayBreaks.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
