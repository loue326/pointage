package com.example.gestionpointage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gestionpointage.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmployeeDashboardActivity extends Activity {

    DatabaseHelper dbHelper;
    SQLiteDatabase db;

    TextView txtStatus, txtTimer, txtWelcome;

    Button btnCheckIn, btnStartBreak;
    Button btnEndBreak, btnCheckOut, btnLeaveRequests, btnLogout;

    int userId;
    int attendanceId = -1;
    int breakId = -1;

    CountDownTimer pauseTimer;

    boolean isOnBreak = false;
    String selectedBreakType = "";

    final long BREAK_DURATION = 10 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_employee_dashboard);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
            userId = preferences.getInt("user_id", -1);
        }

        txtStatus = findViewById(R.id.txtStatus);
        txtTimer = findViewById(R.id.txtTimer);
        txtWelcome = findViewById(R.id.txtWelcome);

        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnStartBreak = findViewById(R.id.btnStartBreak);
        btnEndBreak = findViewById(R.id.btnEndBreak);
        btnCheckOut = findViewById(R.id.btnCheckOut);
        btnLeaveRequests = findViewById(R.id.btnLeaveRequests);
        btnLogout = findViewById(R.id.btnLogout);

        if (userId == -1) {
            Toast.makeText(this,
                "Utilisateur introuvable",
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String name = dbHelper.getUserName(userId);
        if (name != null && !name.isEmpty()) {
            txtWelcome.setText("Bienvenue, " + name);
        } else {
            txtWelcome.setText("Bienvenue, Employé");
        }

        btnCheckIn.setOnClickListener(v -> checkIn());

        btnStartBreak.setOnClickListener(v -> startBreak());

        btnEndBreak.setOnClickListener(v -> endBreak());

        btnCheckOut.setOnClickListener(v -> checkOut());

        btnLeaveRequests.setOnClickListener(v -> {
            Intent intent = new Intent(
                EmployeeDashboardActivity.this,
                LeaveRequestsActivity.class
            );
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());

        restoreTodayAttendance();
    }

    // Obtenir la date actuelle
    private String getCurrentDate() {
        return new SimpleDateFormat(
            "yyyy-MM-dd", Locale.getDefault()
        ).format(new Date());
    }

    // Obtenir l'heure actuelle
    private String getCurrentTime() {
        return new SimpleDateFormat(
            "HH:mm:ss", Locale.getDefault()
        ).format(new Date());
    }

    // Vérifier le pointage du jour
    private void restoreTodayAttendance() {

        Cursor cursor = db.rawQuery(
            "SELECT id, arrival_time, exit_time " +
            "FROM attendance WHERE user_id=? AND date=?",
            new String[]{
                String.valueOf(userId),
                getCurrentDate()
            }
        );

        if (cursor.moveToFirst()) {

            attendanceId = cursor.getInt(0);

            String arrival = cursor.getString(1);
            String exit = cursor.getString(2);

            if (exit != null && !exit.isEmpty()) {

                txtStatus.setText("Journée terminée");

                disableAllButtons();

            } else {

                txtStatus.setText("Arrivée : " + arrival);

                btnCheckIn.setEnabled(false);

                restoreOpenBreak();
            }
        }

        cursor.close();
    }

    // Enregistrer l'arrivée
    private void checkIn() {

        if (attendanceId != -1) {
            showMessage("Vous avez déjà pointé votre arrivée.");
            return;
        }

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("date", getCurrentDate());
        values.put("arrival_time", getCurrentTime());
        values.putNull("exit_time");

        long result = db.insert(
            "attendance", null, values
        );

        if (result != -1) {

            attendanceId = (int) result;

            txtStatus.setText(
                "Arrivée enregistrée à " + getCurrentTime()
            );

            btnCheckIn.setEnabled(false);

            showMessage("Arrivée enregistrée avec succès.");

        } else {

            showMessage("Erreur lors du pointage.");
        }
    }

    // Choisir le type de pause
    private void startBreak() {

        if (attendanceId == -1) {
            showMessage("Vous devez pointer votre arrivée.");
            return;
        }

        if (isOnBreak) {
            showMessage("Vous êtes déjà en pause.");
            return;
        }

        String[] types = {
            "Pause normale",
            "Pause déjeuner",
            "Pause personnelle",
            "Pause exceptionnelle"
        };

        new AlertDialog.Builder(this)
            .setTitle("Choisir le type de pause")
            .setItems(types, (dialog, which) -> {

                selectedBreakType = types[which];

                saveBreak();

            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // Enregistrer la pause dans SQLite
    private void saveBreak() {

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("attendance_id", attendanceId);
        values.put("type", selectedBreakType);
        values.put("start_time", getCurrentTime());
        values.putNull("end_time");

        long result = db.insert("breaks", null, values);

        if (result != -1) {

            breakId = (int) result;
            isOnBreak = true;

            btnStartBreak.setEnabled(false);
            btnEndBreak.setEnabled(true);

            txtStatus.setText(
                "En pause : " + selectedBreakType
            );

            startPauseTimer();

            showMessage(
                selectedBreakType + " enregistrée."
            );

        } else {

            showMessage("Erreur lors de l'enregistrement.");
        }
    }

    // Chronomètre de 10 minutes
    private void startPauseTimer() {

        if (pauseTimer != null) {
            pauseTimer.cancel();
        }

        pauseTimer = new CountDownTimer(
            BREAK_DURATION, 1000
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;

                txtTimer.setText(
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutes, seconds
                    )
                );
            }

            @Override
            public void onFinish() {

                txtTimer.setText("00:00");

                new AlertDialog.Builder(
                    EmployeeDashboardActivity.this
                )
                .setTitle("Fin de pause")
                .setMessage(
                    "Vos 10 minutes de pause sont terminées. " +
                    "Veuillez reprendre le travail."
                )
                .setPositiveButton("OK", null)
                .show();
            }

        }.start();
    }

    // Restaurer une pause non terminée
    private void restoreOpenBreak() {

        Cursor cursor = db.rawQuery(
            "SELECT id, type FROM breaks " +
            "WHERE attendance_id=? AND user_id=? " +
            "AND end_time IS NULL ORDER BY id DESC LIMIT 1",
            new String[]{
                String.valueOf(attendanceId),
                String.valueOf(userId)
            }
        );

        if (cursor.moveToFirst()) {

            breakId = cursor.getInt(0);
            selectedBreakType = cursor.getString(1);
            if (selectedBreakType == null) {
                selectedBreakType = "pause";
            }
            isOnBreak = true;

            txtStatus.setText("En pause : " + selectedBreakType);

            btnStartBreak.setEnabled(false);
            btnEndBreak.setEnabled(true);

            showMessage(
                "Une pause (" + selectedBreakType + ") est déjà en cours. " +
                "Reprenez le travail."
            );
        }

        cursor.close();
    }

    // Terminer la pause
    private void endBreak() {

        if (!isOnBreak || breakId == -1) {
            showMessage("Aucune pause en cours.");
            return;
        }

        if (pauseTimer != null) {
            pauseTimer.cancel();
        }

        ContentValues values = new ContentValues();

        values.put("end_time", getCurrentTime());

        int result = db.update(
            "breaks",
            values,
            "id=? AND end_time IS NULL",
            new String[]{String.valueOf(breakId)}
        );

        if (result > 0) {

            isOnBreak = false;
            breakId = -1;

            txtTimer.setText("10:00");

            txtStatus.setText(
                selectedBreakType.isEmpty()
                    ? "Vous avez repris le travail."
                    : "Retour de " + selectedBreakType + " enregistré."
            );

            selectedBreakType = "";

            btnStartBreak.setEnabled(true);
            btnEndBreak.setEnabled(false);

            showMessage("Retour de pause enregistré.");

        } else {

            showMessage("Erreur lors du retour de pause.");
        }
    }

    // Enregistrer la sortie
    private void checkOut() {

        if (attendanceId == -1) {
            showMessage("Vous devez pointer votre arrivée.");
            return;
        }

        if (isOnBreak) {
            showMessage("Terminez votre pause avant la sortie.");
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Voulez-vous terminer votre journée ?")
            .setPositiveButton("Oui", (dialog, which) -> {

                ContentValues values = new ContentValues();

                values.put("exit_time", getCurrentTime());

                int result = db.update(
                    "attendance",
                    values,
                    "id=? AND exit_time IS NULL",
                    new String[]{String.valueOf(attendanceId)}
                );

                if (result > 0) {

                    txtStatus.setText("Journée terminée");

                    disableAllButtons();

                    showMessage("Sortie enregistrée avec succès.");

                } else {

                    showMessage("Erreur lors du pointage de sortie.");
                }
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // Désactiver les boutons après la sortie
    private void disableAllButtons() {

        btnCheckIn.setEnabled(false);
        btnStartBreak.setEnabled(false);
        btnEndBreak.setEnabled(false);
        btnCheckOut.setEnabled(false);
    }

    // Déconnexion
    private void logout() {

        if (pauseTimer != null) {
            pauseTimer.cancel();
        }

        SharedPreferences preferences = getSharedPreferences("user_session", MODE_PRIVATE);
        preferences.edit().clear().apply();

        Intent intent = new Intent(
            EmployeeDashboardActivity.this,
            LoginActivity.class
        );

        startActivity(intent);
        finish();
    }

    // Afficher un message
    private void showMessage(String message) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (pauseTimer != null) {
            pauseTimer.cancel();
        }

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
