package com.example.gestionpointage;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Formulaire d'envoi d'une demande de congé à l'administrateur.
 */
public class AddLeaveActivity extends AppCompatActivity {

    private TextView inputStart, inputEnd, daysLabel, formError;
    private EditText inputReason;
    private ChipGroup typeGroup;
    private ProgressBar progress;

    private String startDate = null;
    private String endDate = null;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_leave);

        inputStart = findViewById(R.id.inputStartDate);
        inputEnd = findViewById(R.id.inputEndDate);
        inputReason = findViewById(R.id.inputReason);
        daysLabel = findViewById(R.id.daysLabel);
        formError = findViewById(R.id.formError);
        typeGroup = findViewById(R.id.typeGroup);
        progress = findViewById(R.id.sendProgress);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendRequest());

        inputStart.setOnClickListener(v -> pickDate(true));
        inputEnd.setOnClickListener(v -> pickDate(false));

        // Dates pré-remplies depuis l'agenda
        String start = getIntent().getStringExtra("start");
        String end = getIntent().getStringExtra("end");

        if (start != null) startDate = start;
        if (end != null) endDate = end;

        updateDateLabels();
    }

    // =========================================================
    //  Sélecteur de date
    // =========================================================

    private void pickDate(final boolean isStart) {

        Calendar calendar = Calendar.getInstance();

        if (isStart && startDate != null) {
            setDate(calendar, startDate);
        } else if (!isStart && endDate != null) {
            setDate(calendar, endDate);
        } else if (!isStart && startDate != null) {
            setDate(calendar, startDate);
        }

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {

            String picked = String.format(Locale.US, "%04d-%02d-%02d",
                    year, month + 1, dayOfMonth);

            if (isStart) {
                startDate = picked;
                // Si la fin est antérieure, on la remet à jour
                if (endDate != null && endDate.compareTo(startDate) < 0) {
                    endDate = startDate;
                }
            } else {
                endDate = picked;
            }

            updateDateLabels();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 86400000L);
        dialog.show();
    }

    private void setDate(Calendar calendar, String isoDate) {
        try {
            java.util.Date d = iso.parse(isoDate);
            if (d != null) calendar.setTime(d);
        } catch (Exception ignored) {
        }
    }

    private void updateDateLabels() {

        inputStart.setText(startDate == null ? "Choisir" : Ui.isoToFr(startDate));
        inputStart.setTextColor(getColor(startDate == null
                ? R.color.text_light : R.color.text_dark));

        inputEnd.setText(endDate == null ? "Choisir" : Ui.isoToFr(endDate));
        inputEnd.setTextColor(getColor(endDate == null
                ? R.color.text_light : R.color.text_dark));

        if (startDate != null && endDate != null) {
            long days = LeaveRequestDays();
            daysLabel.setText("📅 " + days + " jour" + (days > 1 ? "s" : "")
                    + " demandé" + (days > 1 ? "s" : ""));
        } else {
            daysLabel.setText("Sélectionnez les dates de début et de fin");
        }
    }

    private long LeaveRequestDays() {
        return com.example.gestionpointage.model.LeaveRequest
                .dateDiffDays(startDate, endDate);
    }

    // =========================================================
    //  Envoi
    // =========================================================

    private void sendRequest() {

        hideError();

        String reason = inputReason.getText().toString().trim();
        String type = selectedType();

        if (startDate == null || endDate == null) {
            showError("Veuillez choisir les dates de début et de fin.");
            return;
        }

        if (endDate.compareTo(startDate) < 0) {
            showError("La date de fin doit être postérieure à la date de début.");
            return;
        }

        if (reason.length() < 5) {
            showError("Veuillez indiquer un motif (5 caractères minimum).");
            return;
        }

        final int userId = new SessionManager(this).getUserId();
        final String fType = type;
        final String fStart = startDate;
        final String fEnd = endDate;
        final String fReason = reason;

        setBusy(true);

        new Thread(() -> {

            DatabaseHelper db = new DatabaseHelper(AddLeaveActivity.this);
            boolean ok = db.addLeave(userId, fType, fStart, fEnd, fReason);

            handler.post(() -> {
                setBusy(false);

                if (isFinishing() || isDestroyed()) return;

                if (ok) {
                    Ui.toast(this, "Demande envoyée à l'administrateur ✅");
                    finish();
                } else {
                    showError("Impossible d'envoyer la demande, réessayez.");
                }
            });
        }).start();
    }

    private String selectedType() {
        int id = typeGroup.getCheckedChipId();

        if (id == R.id.typeSick) return "Congé maladie";
        if (id == R.id.typeUnpaid) return "Sans solde";
        if (id == R.id.typeOther) return "Autre congé";
        return "Congé annuel";
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnSend).setEnabled(!busy);
        findViewById(R.id.btnSend).setAlpha(busy ? 0.6f : 1f);
    }

    private void showError(String message) {
        formError.setText(message);
        formError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        formError.setVisibility(View.GONE);
    }
}
