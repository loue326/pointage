package com.example.gestionpointage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.example.gestionpointage.database.DatabaseHelper;

public class AdminDashboardActivity extends Activity {

    DatabaseHelper dbHelper;
    SQLiteDatabase db;

    TextView txtTotalEmployees;
    TextView txtPresent;
    TextView txtAbsent;
    TextView txtPending;
    TextView txtSectionTitle;

    LinearLayout contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_admin_dashboard);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        txtTotalEmployees = findViewById(R.id.txtTotalEmployees);
        txtPresent = findViewById(R.id.txtPresent);
        txtAbsent = findViewById(R.id.txtAbsent);
        txtPending = findViewById(R.id.txtPending);
        txtSectionTitle = findViewById(R.id.txtSectionTitle);

        contentContainer = findViewById(R.id.contentContainer);

        Button btnEmployees = findViewById(R.id.btnEmployees);
        Button btnLeaves = findViewById(R.id.btnLeaves);
        Button btnAttendance = findViewById(R.id.btnAttendance);
        Button btnBreaks = findViewById(R.id.btnBreaks);

        btnEmployees.setOnClickListener(v -> showEmployees());

        btnLeaves.setOnClickListener(v -> showLeaves());

        btnAttendance.setOnClickListener(v -> showAttendance());

        btnBreaks.setOnClickListener(v -> showBreaks());

        loadStatistics();
    }

    // Statistiques générales
    private void loadStatistics() {

        int total = getCount(
            "SELECT COUNT(*) FROM users WHERE role='employee'"
        );

        int present = getCount(
            "SELECT COUNT(DISTINCT user_id) FROM attendance " +
            "WHERE date = DATE('now','localtime')"
        );

        int pending = getCount(
            "SELECT COUNT(*) FROM leaves WHERE status='pending'"
        );

        int absent = Math.max(0, total - present);

        txtTotalEmployees.setText("Total employés : " + total);

        txtPresent.setText("Présents : " + present);

        txtAbsent.setText("Absents : " + absent);

        txtPending.setText("Congés en attente : " + pending);
    }

    // Compter les résultats SQL
    private int getCount(String sql) {

        Cursor cursor = db.rawQuery(sql, null);

        int result = 0;

        if (cursor.moveToFirst()) {
            result = cursor.getInt(0);
        }

        cursor.close();

        return result;
    }

    // Ajouter un texte dans la liste
    private void addText(String text) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(16);
        view.setPadding(12, 15, 12, 15);

        contentContainer.addView(view);
    }

    // Ajouter un bouton dans la liste
    private void addActionButton(String text, Runnable action) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
            ColorStateList.valueOf(
                Color.rgb(255, 136, 0)
            )
        );

        contentContainer.addView(button);

        button.setOnClickListener(v -> action.run());
    }

    // Préparer l'affichage d'une section
    private void prepareSection(String title) {

        txtSectionTitle.setText(title);

        contentContainer.removeAllViews();
    }

    // Liste des employés
    private void showEmployees() {

        prepareSection("LISTE DES EMPLOYÉS");

        Cursor cursor = db.rawQuery(
            "SELECT id, name, email FROM users " +
            "WHERE role='employee'",
            null
        );

        if (cursor.getCount() == 0) {
            addText("Aucun employé trouvé.");
        }

        while (cursor.moveToNext()) {

            int id = cursor.getInt(0);

            String name = cursor.getString(1);
            String email = cursor.getString(2);

            addText(
                "ID : " + id +
                "\nNom : " + name +
                "\nEmail : " + email
            );
        }

        cursor.close();
    }

    // Demandes de congés
    private void showLeaves() {

        prepareSection("DEMANDES DE CONGÉS");

        Cursor cursor = db.rawQuery(
            "SELECT leaves.id, users.name, " +
            "leaves.start_date, leaves.end_date, " +
            "leaves.reason, leaves.status " +
            "FROM leaves INNER JOIN users " +
            "ON leaves.user_id = users.id " +
            "ORDER BY leaves.id DESC",
            null
        );

        if (cursor.getCount() == 0) {
            addText("Aucune demande de congé.");
        }

        while (cursor.moveToNext()) {

            int id = cursor.getInt(0);

            String name = cursor.getString(1);
            String start = cursor.getString(2);
            String end = cursor.getString(3);
            String reason = cursor.getString(4);
            String status = cursor.getString(5);

            addText(
                "Employé : " + name +
                "\nDébut : " + start +
                "\nFin : " + end +
                "\nMotif : " + reason +
                "\nStatut : " + status
            );

            if ("pending".equalsIgnoreCase(status)) {

                addActionButton("Accepter", () ->
                    confirmLeave(id, "accepted")
                );

                addActionButton("Refuser", () ->
                    confirmLeave(id, "refused")
                );
            }
        }

        cursor.close();
    }

    // Accepter ou refuser une demande
    private void confirmLeave(int id, String status) {

        String message = status.equals("accepted")
            ? "Voulez-vous accepter cette demande ?"
            : "Voulez-vous refuser cette demande ?";

        new AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage(message)
            .setPositiveButton("Oui", (dialog, which) -> {

                SQLiteDatabase writable =
                    dbHelper.getWritableDatabase();

                ContentValues values =
                    new ContentValues();

                values.put("status", status);

                int result = writable.update(
                    "leaves",
                    values,
                    "id=? AND status=?",
                    new String[]{String.valueOf(id), "pending"}
                );

                if (result > 0) {

                    Toast.makeText(
                        this,
                        "Demande mise à jour avec succès",
                        Toast.LENGTH_SHORT
                    ).show();

                } else {

                    Toast.makeText(
                        this,
                        "Cette demande a déjà été traitée",
                        Toast.LENGTH_SHORT
                    ).show();
                }

                loadStatistics();
                showLeaves();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    // Consultation des pointages
    private void showAttendance() {

        prepareSection("HISTORIQUE DES POINTAGES");

        Cursor cursor = db.rawQuery(
            "SELECT users.name, attendance.date, " +
            "attendance.arrival_time, attendance.exit_time " +
            "FROM attendance INNER JOIN users " +
            "ON attendance.user_id = users.id " +
            "ORDER BY attendance.id DESC",
            null
        );

        if (cursor.getCount() == 0) {
            addText("Aucun pointage enregistré.");
        }

        while (cursor.moveToNext()) {

            String name = cursor.getString(0);
            String date = cursor.getString(1);
            String arrival = cursor.getString(2);
            String exit = cursor.getString(3);

            addText(
                "Employé : " + name +
                "\nDate : " + date +
                "\nArrivée : " + arrival +
                "\nSortie : " + exit
            );
        }

        cursor.close();
    }

    // Consultation des pauses
    private void showBreaks() {

        prepareSection("PAUSES DES EMPLOYÉS");

        Cursor cursor = db.rawQuery(
            "SELECT users.name, breaks.type, " +
            "breaks.start_time, breaks.end_time " +
            "FROM breaks INNER JOIN users " +
            "ON breaks.user_id = users.id " +
            "ORDER BY breaks.id DESC",
            null
        );

        if (cursor.getCount() == 0) {
            addText("Aucune pause enregistrée.");
        }

        while (cursor.moveToNext()) {

            String name = cursor.getString(0);
            String type = cursor.getString(1);
            String start = cursor.getString(2);
            String end = cursor.getString(3);

            addText(
                "Employé : " + name +
                "\nType : " + type +
                "\nDébut : " + start +
                "\nFin : " + end
            );
        }

        cursor.close();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null) {
            loadStatistics();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
