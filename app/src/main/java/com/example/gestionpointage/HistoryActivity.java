package com.example.gestionpointage;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.AttendanceAdapter;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.AttendanceRecord;
import com.example.gestionpointage.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Historique des pointages de l'employé (avec ses pauses).
 */
public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        SessionManager session = new SessionManager(this);
        DatabaseHelper db = new DatabaseHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.historyList);
        TextView empty = findViewById(R.id.emptyHistory);
        TextView statDays = findViewById(R.id.statDays);
        TextView statBreaks = findViewById(R.id.statBreaks);

        List<AttendanceRecord> records = db.getHistory(session.getUserId(), 60);

        // Statistiques
        int days = records.size();
        int breaks = 0;
        for (AttendanceRecord r : records) breaks += r.breaks.size();

        statDays.setText(String.valueOf(days));
        statBreaks.setText(String.valueOf(breaks));

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new AttendanceAdapter(records));

        if (records.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
    }
}
