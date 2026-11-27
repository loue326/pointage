package com.example.gestionpointage;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Formulaire d'envoi d'une demande de congé à l'administrateur.
 */
public class AddLeaveActivity extends AppCompatActivity {

    private TextView inputStart, inputEnd, daysLabel, formError;
    private EditText inputReason;
    private ChipGroup typeGroup;
    private ProgressBar progress;
    private ScrollView formScroll;

    private String startDate = null;
    private String endDate = null;
    private boolean isSending = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_leave);

        View root = findViewById(R.id.addLeaveRoot);
        View header = findViewById(R.id.headerLayout);
        View actionsBar = findViewById(R.id.actionsBar);
        formScroll = findViewById(R.id.formScroll);

        applySystemInsets(root, header, actionsBar);

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

        inputReason.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hideError();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Dates pré-remplies depuis l'agenda
        String start = getIntent().getStringExtra("start");
        String end = getIntent().getStringExtra("end");

        if (start != null) startDate = start;
        if (end != null) endDate = end;

        updateDateLabels();
    }

    private void applySystemInsets(View root, View header, View actionsBar) {
        final int headerPadLeft = header.getPaddingLeft();
        final int headerPadTop = header.getPaddingTop();
        final int headerPadRight = header.getPaddingRight();
        final int headerPadBottom = header.getPaddingBottom();

        final int barPadLeft = actionsBar.getPaddingLeft();
        final int barPadTop = actionsBar.getPaddingTop();
        final int barPadRight = actionsBar.getPaddingRight();
        final int barPadBottom = actionsBar.getPaddingBottom();

        final int minTopExtra = Math.round(12 * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int topPad = Math.max(headerPadTop, sysBars.top + minTopExtra);
            header.setPadding(
                    headerPadLeft + sysBars.left,
                    topPad,
                    headerPadRight + sysBars.right,
                    headerPadBottom
            );

            int bottomInset = Math.max(sysBars.bottom, ime.bottom);
            actionsBar.setPadding(
                    barPadLeft + sysBars.left,
                    barPadTop,
                    barPadRight + sysBars.right,
                    barPadBottom + bottomInset
            );

            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    // =========================================================
    //  Sélecteur de date
    // =========================================================

    private void pickDate(final boolean isStart) {
        hideError();

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

        long minDate = System.currentTimeMillis() - 86400000L;
        if (!isStart && startDate != null) {
            try {
                Date startParsed = iso.parse(startDate);
                if (startParsed != null) {
                    minDate = Math.max(minDate, startParsed.getTime());
                }
            } catch (Exception ignored) {
            }
        }
        if (calendar.getTimeInMillis() < minDate) {
            minDate = calendar.getTimeInMillis();
        }
        dialog.getDatePicker().setMinDate(minDate);
        dialog.show();
    }

    private void setDate(Calendar calendar, String isoDate) {
        try {
            Date d = iso.parse(isoDate);
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
        return LeaveRequest.dateDiffDays(startDate, endDate);
    }

    // =========================================================
    //  Envoi
    // =========================================================

    private void sendRequest() {
        if (isSending) return;

        hideError();
        hideKeyboard();

        String reason = inputReason.getText().toString().trim();
        if (reason.isEmpty()) {
            reason = "Demande de congé";
        }
        String type = selectedType();

        if (startDate == null) {
            startDate = DatabaseHelper.today();
        }
        if (endDate == null) {
            endDate = startDate;
        }

        if (endDate.compareTo(startDate) < 0) {
            endDate = startDate;
        }

        final SessionManager session = new SessionManager(this);
        final int sessionUserId = session.getUserId();
        final String sessionEmail = session.getEmail();
        final String fType = type;
        final String fStart = startDate;
        final String fEnd = endDate;
        final String fReason = reason;

        isSending = true;
        setBusy(true);

        new Thread(() -> {
            boolean success;
            DatabaseHelper db = null;
            try {
                db = new DatabaseHelper(AddLeaveActivity.this);
                int userId = sessionUserId;
                if (userId <= 0 && sessionEmail != null && !sessionEmail.trim().isEmpty()) {
                    userId = db.getUserId(sessionEmail);
                }
                if (userId <= 0) {
                    userId = 2; // Default to Loue Bessaadi
                }

                success = db.addLeave(userId, fType, fStart, fEnd, fReason);
            } catch (Exception e) {
                success = false;
            } finally {
                if (db != null) {
                    db.close();
                }
            }

            final boolean ok = success;

            handler.post(() -> {
                isSending = false;
                setBusy(false);

                if (isFinishing() || isDestroyed()) return;

                if (ok) {
                    Ui.toast(this, "Demande envoyée à l'administrateur ✅");
                    finish();
                } else {
                    showError("Impossible d'envoyer la demande.");
                }
            });
        }).start();
    }

    private void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            }
        }
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
        findViewById(R.id.btnCancel).setEnabled(!busy);
    }

    private void showError(String message) {
        formError.setText(message);
        formError.setVisibility(View.VISIBLE);
        if (formScroll != null) {
            formScroll.post(() -> formScroll.smoothScrollTo(0, formError.getBottom()));
        }
    }

    private void hideError() {
        formError.setVisibility(View.GONE);
    }
}
