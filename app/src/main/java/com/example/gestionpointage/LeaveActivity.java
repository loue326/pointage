package com.example.gestionpointage;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.DayAdapter;
import com.example.gestionpointage.adapter.LeaveAdapter;
import com.example.gestionpointage.api.Holiday;
import com.example.gestionpointage.api.HolidayService;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Agenda : calendrier de sélection des dates + liste des demandes de congé.
 */
public class LeaveActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;

    private DayAdapter dayAdapter;
    private TextView monthTitle, selectionLabel, selectionSummary, holidayLabel;
    private TextView emptyLeaves;
    private RecyclerView myLeavesList;

    private int year, month;              // month : 0 = janvier
    private String startIso = null;
    private String endIso = null;
    private List<Holiday> holidays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave);

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);

        monthTitle = findViewById(R.id.monthTitle);
        selectionLabel = findViewById(R.id.selectionLabel);
        selectionSummary = findViewById(R.id.selectionSummary);
        holidayLabel = findViewById(R.id.holidayLabel);
        emptyLeaves = findViewById(R.id.emptyLeaves);
        myLeavesList = findViewById(R.id.myLeavesList);

        // ---------- Calendrier ----------
        RecyclerView grid = findViewById(R.id.calendarGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 7));

        dayAdapter = new DayAdapter(year, month);
        grid.setAdapter(dayAdapter);

        dayAdapter.setOnDayClick(this::onDaySelected);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> changeMonth(-1));
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> changeMonth(1));
        monthTitle.setText(dayAdapter.getTitle());

        // ---------- Jours fériés : API externe ----------
        loadHolidays();

        // ---------- Nouvelle demande ----------
        findViewById(R.id.btnNewLeave).setOnClickListener(v -> {

            if (startIso == null || endIso == null) {
                Ui.toast(this, "Sélectionnez d'abord vos dates dans l'agenda.");
                return;
            }

            Intent intent = new Intent(this, AddLeaveActivity.class);
            intent.putExtra("start", startIso);
            intent.putExtra("end", endIso);
            startActivity(intent);
        });

        // ---------- Mes demandes ----------
        myLeavesList.setLayoutManager(new LinearLayoutManager(this));
        myLeavesList.setAdapter(new LeaveAdapter(new java.util.ArrayList<>(), false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMyLeaves();
    }

    // =========================================================
    //  Calendrier
    // =========================================================

    private void changeMonth(int delta) {
        month += delta;
        if (month > 11) {
            month = 0;
            year++;
        } else if (month < 0) {
            month = 11;
            year--;
        }

        dayAdapter.rebuild(year, month);
        monthTitle.setText(dayAdapter.getTitle());
    }

    private void onDaySelected(String isoDate) {

        if (startIso == null || endIso != null) {
            // Nouvelle sélection
            startIso = isoDate;
            endIso = null;
        } else if (isoDate.compareTo(startIso) <= 0) {
            startIso = isoDate;
            endIso = null;
        } else {
            endIso = isoDate;
        }

        dayAdapter.setSelection(startIso, endIso);
        updateSelectionLabels();
    }

    private void updateSelectionLabels() {

        if (startIso == null) {
            selectionLabel.setText("Touchez une date pour commencer");
            selectionSummary.setText("Sélectionnez vos dates dans l'agenda");
            holidayLabel.setVisibility(View.GONE);
            return;
        }

        if (endIso == null) {
            selectionLabel.setText("Début : " + Ui.isoToFr(startIso) + "  —  choisissez la fin");
            selectionSummary.setText("Début le " + Ui.isoToLong(startIso));
        } else {
            long days = LeaveRequest.dateDiffDays(startIso, endIso);
            selectionLabel.setText(Ui.isoToFr(startIso) + " → " + Ui.isoToFr(endIso)
                    + "  (" + days + " jour" + (days > 1 ? "s" : "") + ")");
            selectionSummary.setText(days + " jour" + (days > 1 ? "s" : "")
                    + " demandé" + (days > 1 ? "s" : ""));
        }

        // Jour férié dans la sélection ?
        if (holidays != null) {
            Holiday h = HolidayService.find(holidays, startIso);
            if (h != null) {
                holidayLabel.setText("⚠ " + h.localName + " le " + Ui.isoToFr(h.date)
                        + " (jour férié)");
                holidayLabel.setVisibility(View.VISIBLE);
                return;
            }
        }
        holidayLabel.setVisibility(View.GONE);
    }

    private void loadHolidays() {
        HolidayService.fetch(new HolidayService.Listener() {
            @Override
            public void onResult(List<Holiday> result) {
                if (isFinishing() || isDestroyed()) return;
                holidays = result;
                dayAdapter.setHolidays(result);
                updateSelectionLabels();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                Ui.toast(LeaveActivity.this,
                        "Impossible de charger les jours fériés : " + message);
            }
        });
    }

    // =========================================================
    //  Mes demandes
    // =========================================================

    private void refreshMyLeaves() {
        List<LeaveRequest> leaves = db.getLeavesOf(session.getUserId());

        myLeavesList.setAdapter(new LeaveAdapter(leaves, false));
        emptyLeaves.setVisibility(leaves.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
