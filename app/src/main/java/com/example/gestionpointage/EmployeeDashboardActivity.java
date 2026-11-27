package com.example.gestionpointage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.AttendanceRecord;
import com.example.gestionpointage.model.BreakRecord;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.model.User;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

import java.util.List;

/**
 * Tableau de bord de l'employé.
 */
public class EmployeeDashboardActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper db;
    private TextView statusLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);

        session = new SessionManager(this);
        db = new DatabaseHelper(this);

        // ----- En-tête -----
        TextView avatar = findViewById(R.id.avatar);
        TextView userName = findViewById(R.id.userName);
        TextView userJob = findViewById(R.id.userJob);
        TextView todayDate = findViewById(R.id.todayDate);
        statusLabel = findViewById(R.id.statusLabel);

        avatar.setText(session.getName().length() >= 2
                ? session.getName().substring(0, 1).toUpperCase()
                + session.getName().substring(1, 2).toUpperCase()
                : "?");
        userName.setText(session.getName());

        User me = db.getUserById(session.getUserId());
        userJob.setText(me == null || me.job == null || me.job.isEmpty()
                ? "Employé" : me.job);

        todayDate.setText(Ui.isoToLong(Ui.nowIso()));

        // ----- Navigation -----
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        statusLabel.setOnClickListener(v ->
                startActivity(new Intent(this, PointageActivity.class)));

        findViewById(R.id.menuPointage).setOnClickListener(v ->
                startActivity(new Intent(this, PointageActivity.class)));

        findViewById(R.id.menuBreaks).setOnClickListener(v ->
                startActivity(new Intent(this, PointageActivity.class)));

        findViewById(R.id.menuHistory).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        findViewById(R.id.menuAgenda).setOnClickListener(v ->
                startActivity(new Intent(this, LeaveActivity.class)));

        findViewById(R.id.menuMyLeaves).setOnClickListener(v ->
                startActivity(new Intent(this, LeaveActivity.class)));

        findViewById(R.id.menuAddLeave).setOnClickListener(v ->
                startActivity(new Intent(this, AddLeaveActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    /** Mise à jour de la carte du jour + dernière activité */
    private void refresh() {

        int userId = session.getUserId();

        TextView arrival = findViewById(R.id.todayArrival);
        TextView exit = findViewById(R.id.todayExit);
        TextView breaks = findViewById(R.id.todayBreaks);
        TextView lastTitle = findViewById(R.id.lastActivityTitle);
        TextView lastSub = findViewById(R.id.lastActivitySub);
        TextView lastIcon = findViewById(R.id.lastActivityIcon);
        TextView leavesSubtitle = findViewById(R.id.myLeavesSubtitle);

        AttendanceRecord today = db.getTodayAttendance(userId);

        if (today == null) {
            arrival.setText("--:--");
            exit.setText("--:--");
            breaks.setText("0");
            statusLabel.setText("Vous n'avez pas encore pointé");
            statusLabel.setBackgroundResource(R.drawable.chip_pending);
            statusLabel.setTextColor(getColor(R.color.warning));

            lastIcon.setText("⏰");
            lastTitle.setText("Aucune activité");
            lastSub.setText("Commencez par pointer votre arrivée");
        } else {
            arrival.setText(DatabaseHelper.shortTime(today.arrivalTime));
            exit.setText(today.exitTime == null ? "en cours" : DatabaseHelper.shortTime(today.exitTime));
            breaks.setText(String.valueOf(today.breaks.size()));

            if (today.exitTime == null) {
                statusLabel.setText("Journée en cours — vous êtes en poste ✅");
                statusLabel.setBackgroundResource(R.drawable.chip_approved);
                statusLabel.setTextColor(getColor(R.color.success));
            } else {
                statusLabel.setText("Journée terminée — à demain 👋");
                statusLabel.setBackgroundResource(R.drawable.chip_approved);
                statusLabel.setTextColor(getColor(R.color.success));
            }

            // Dernière activité
            BreakRecord openBreak = db.getOpenBreak(userId);
            if (openBreak != null) {
                lastIcon.setText("☕");
                lastTitle.setText("Pause " + openBreak.type.toLowerCase() + " en cours");
                lastSub.setText("Depuis " + DatabaseHelper.shortTime(openBreak.startTime));
            } else if (!today.breaks.isEmpty()) {
                BreakRecord last = today.breaks.get(0);
                lastIcon.setText(last.getEmoji());
                lastTitle.setText("Dernière pause : " + last.type);
                lastSub.setText(DatabaseHelper.shortTime(last.startTime)
                        + " → " + (last.isOpen() ? "en cours" : DatabaseHelper.shortTime(last.endTime)));
            } else if (today.exitTime != null) {
                lastIcon.setText("🌙");
                lastTitle.setText("Sortie enregistrée");
                lastSub.setText("à " + DatabaseHelper.shortTime(today.exitTime));
            } else {
                lastIcon.setText("🌅");
                lastTitle.setText("Arrivée enregistrée");
                lastSub.setText("à " + DatabaseHelper.shortTime(today.arrivalTime));
            }
        }

        // Nombre de demandes en attente
        int pending = db.countLeaves(LeaveRequest.PENDING, userId);
        int total = db.countLeaves(LeaveRequest.PENDING, userId)
                + db.countLeaves(LeaveRequest.APPROVED, userId)
                + db.countLeaves(LeaveRequest.REJECTED, userId);

        leavesSubtitle.setText(pending > 0
                ? pending + " en attente sur " + total
                : (total + " demande" + (total > 1 ? "s" : "")));
    }

    private void logout() {
        new SessionManager(this).logout();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
