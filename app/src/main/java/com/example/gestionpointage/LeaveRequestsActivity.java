package com.example.gestionpointage;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.gestionpointage.database.DatabaseHelper;

import java.util.ArrayList;

public class LeaveRequestsActivity extends Activity {

    DatabaseHelper dbHelper;
    SQLiteDatabase db;

    ListView listLeaves;
    Button btnAddLeave;

    int userId;

    ArrayList<String> leaveList;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_leave_requests);

        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        listLeaves = findViewById(R.id.listLeaves);
        btnAddLeave = findViewById(R.id.btnAddLeave);

        userId = getIntent().getIntExtra("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this,
                "Utilisateur introuvable",
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        leaveList = new ArrayList<>();

        adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_1,
            leaveList
        );

        listLeaves.setAdapter(adapter);

        btnAddLeave.setOnClickListener(v -> {
            Intent intent = new Intent(
                LeaveRequestsActivity.this,
                AddLeaveActivity.class
            );

            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        loadLeaveRequests();

        listLeaves.setOnItemClickListener((parent, view, position, id) -> {
            new AlertDialog.Builder(this)
                .setTitle("Détails de la demande")
                .setMessage(leaveList.get(position))
                .setPositiveButton("Fermer", null)
                .show();
        });
    }

    // Charger les demandes de congé de l'employé
    private void loadLeaveRequests() {

        leaveList.clear();

        Cursor cursor = db.rawQuery(
            "SELECT id, start_date, end_date, reason, status " +
            "FROM leaves WHERE user_id=? ORDER BY id DESC",
            new String[]{String.valueOf(userId)}
        );

        if (cursor.moveToFirst()) {

            do {

                int id = cursor.getInt(0);
                String start = cursor.getString(1);
                String end = cursor.getString(2);
                String reason = cursor.getString(3);
                String status = cursor.getString(4);

                String item =
                    "Demande #" + id +
                    "\nDébut : " + start +
                    "\nFin : " + end +
                    "\nMotif : " + reason +
                    "\nStatut : " + status;

                leaveList.add(item);

            } while (cursor.moveToNext());

        } else {

            leaveList.add("Aucune demande de congé.");
        }

        cursor.close();

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && userId != -1) {
            loadLeaveRequests();
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
