package com.example.gestionpointage;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.BreakAdapter;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.BreakRecord;
import com.example.gestionpointage.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Toutes les pauses du jour : chaque employé, son type de pause,
 * son heure de début et de fin (vue administrateur).
 */
public class AdminBreaksActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_breaks);

        DatabaseHelper db = new DatabaseHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView headerDate = findViewById(R.id.headerDate);
        headerDate.setText("Pauses du " + Ui.isoToFr(Ui.nowIso()));

        List<BreakRecord> breaks = db.getBreaksOfDay(DatabaseHelper.today());

        int open = 0;
        for (BreakRecord b : breaks) {
            if (b.isOpen()) open++;
        }

        ((TextView) findViewById(R.id.totalBreaks)).setText(String.valueOf(breaks.size()));
        ((TextView) findViewById(R.id.openBreaks)).setText(String.valueOf(open));

        RecyclerView list = findViewById(R.id.breaksList);
        TextView empty = findViewById(R.id.emptyBreaks);

        list.setLayoutManager(new LinearLayoutManager(this));
        // showUserName = true : on affiche le nom de l'employé pour chaque pause
        list.setAdapter(new BreakAdapter(new ArrayList<>(breaks), true));

        if (breaks.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
        }
    }
}
