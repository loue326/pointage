package com.example.gestionpointage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.api.Holiday;
import com.example.gestionpointage.api.HolidayService;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

import java.util.Calendar;
import java.util.List;

/**
 * Tableau de bord de l'administrateur.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        SessionManager session = new SessionManager(this);
        db = new DatabaseHelper(this);

        TextView avatar = findViewById(R.id.avatar);
        TextView userName = findViewById(R.id.userName);

        avatar.setText("AD");
        userName.setText(session.getName());

        // Déconnexion
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            session.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Sections
        findViewById(R.id.actionUsers).setOnClickListener(v ->
                startActivity(new Intent(this, AdminUsersActivity.class)));

        findViewById(R.id.actionLeaves).setOnClickListener(v ->
                startActivity(new Intent(this, AdminLeavesActivity.class)));

        findViewById(R.id.actionBreaks).setOnClickListener(v ->
                startActivity(new Intent(this, AdminBreaksActivity.class)));

        findViewById(R.id.holidayCard).setOnClickListener(v ->
                loadHolidays(true));

        // Jours fériés via l'API externe
        loadHolidays(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        TextView employees = findViewById(R.id.statEmployees);
        TextView present = findViewById(R.id.statPresent);
        TextView pending = findViewById(R.id.statPending);
        TextView breaks = findViewById(R.id.statBreaks);
        TextView leavesSubtitle = findViewById(R.id.leavesSubtitle);

        int nbEmployees = db.countUsers("employee");
        int nbPending = db.countPendingLeaves();

        employees.setText(String.valueOf(nbEmployees));
        present.setText(db.countPresentToday() + "/" + nbEmployees);
        pending.setText(String.valueOf(nbPending));
        breaks.setText(String.valueOf(db.countBreaksToday()));

        leavesSubtitle.setText(nbPending > 0
                ? nbPending + " demande" + (nbPending > 1 ? "s" : "") + " à traiter"
                : "Accepter ou refuser les demandes");
    }

    /** Consommation de l'API externe : jours fériés du pays courant */
    private void loadHolidays(final boolean showResult) {

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        HolidayService.fetch(currentYear, "TN", new HolidayService.Listener() {
            @Override
            public void onResult(List<Holiday> holidays) {
                if (isFinishing() || isDestroyed()) return;

                TextView title = findViewById(R.id.holidayTitle);
                TextView subtitle = findViewById(R.id.holidaySubtitle);

                if (holidays.isEmpty()) {
                    title.setText("Aucun jour férié trouvé");
                    subtitle.setText("Réponse vide de l'API");
                } else {
                    Holiday next = nextHoliday(holidays);
                    title.setText(holidays.size() + " jours fériés en "
                            + currentYear + (next != null ? " 🌍" : ""));
                    subtitle.setText(next != null
                            ? "Prochain : " + next.localName + " (" + Ui.isoToFr(next.date) + ")"
                            : "Source : date.nager.at");
                }

                if (showResult) {
                    Ui.toast(AdminDashboardActivity.this,
                            holidays.size() + " jours fériés chargés depuis l'API ✓");
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;

                TextView title = findViewById(R.id.holidayTitle);
                TextView subtitle = findViewById(R.id.holidaySubtitle);

                title.setText("API indisponible");
                subtitle.setText(message);

                if (showResult) {
                    Ui.toast(AdminDashboardActivity.this,
                            "Erreur API : " + message);
                }
            }
        });
    }

    private Holiday nextHoliday(List<Holiday> holidays) {
        String today = Ui.nowIso();
        Holiday best = null;

        for (Holiday h : holidays) {
            if (h.date == null) continue;
            if (h.date.compareTo(today) >= 0) {
                if (best == null || h.date.compareTo(best.date) < 0) {
                    best = h;
                }
            }
        }
        return best;
    }
}
