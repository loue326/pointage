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
import com.example.gestionpointage.model.User;
import com.example.gestionpointage.util.Ui;

import java.util.List;

/**
 * Détail d'un employé : pointages ET pauses de chaque utilisateur.
 */
public class UserDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        DatabaseHelper db = new DatabaseHelper(this);
        int userId = getIntent().getIntExtra("userId", -1);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        User user = db.getUserById(userId);

        if (user == null) {
            Ui.toast(this, "Employé introuvable.");
            finish();
            return;
        }

        // ----- En-tête -----
        TextView screenTitle = findViewById(R.id.screenTitle);
        TextView subtitle = findViewById(R.id.headerSubtitle);
        TextView avatar = findViewById(R.id.userAvatar);
        TextView userName = findViewById(R.id.userName);
        TextView userJob = findViewById(R.id.userJob);
        TextView todayChip = findViewById(R.id.userTodayChip);

        screenTitle.setText("Pointages & pauses");
        subtitle.setText("Détail de l'employé");
        avatar.setText(user.getInitials());
        userName.setText(user.name);
        userJob.setText((user.job == null || user.job.isEmpty() ? "" : user.job + " • ")
                + user.email);

        // ----- Statut du jour -----
        AttendanceRecord today = db.getTodayAttendance(userId);

        if (today == null) {
            todayChip.setText("Non pointé aujourd'hui");
            todayChip.setBackgroundResource(R.drawable.chip_pending);
            todayChip.setTextColor(getColor(R.color.warning));
        } else if (today.exitTime == null) {
            todayChip.setText("Présent depuis " + DatabaseHelper.shortTime(today.arrivalTime));
            todayChip.setBackgroundResource(R.drawable.chip_approved);
            todayChip.setTextColor(getColor(R.color.success));
        } else {
            todayChip.setText("Sorti à " + DatabaseHelper.shortTime(today.exitTime));
            todayChip.setBackgroundResource(R.drawable.chip_rejected);
            todayChip.setTextColor(getColor(R.color.danger));
        }

        // ----- Statistiques -----
        List<AttendanceRecord> records = db.getHistory(userId, 90);

        int days = records.size();
        int breaks = 0;
        for (AttendanceRecord r : records) breaks += r.breaks.size();

        ((TextView) findViewById(R.id.statDays)).setText(String.valueOf(days));
        ((TextView) findViewById(R.id.statBreaks)).setText(String.valueOf(breaks));
        ((TextView) findViewById(R.id.statPresent)).setText(today == null ? "Non" : "Oui");

        // ----- Historique (pointages + pauses) -----
        RecyclerView list = findViewById(R.id.detailList);
        TextView empty = findViewById(R.id.emptyDetail);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new AttendanceAdapter(records));

        if (records.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);
        }
    }
}
