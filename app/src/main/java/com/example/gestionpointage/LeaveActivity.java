package com.example.gestionpointage;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LeaveActivity extends AppCompatActivity {

    private Button btnStartDate, btnEndDate;
    private Button btnSubmitLeave, btnBack;

    private EditText etReason;

    private DatabaseHelper databaseHelper;

    private int userId;

    private String startDate = "";
    private String endDate = "";

    private SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_leave);

        databaseHelper = new DatabaseHelper(this);

        SharedPreferences preferences =
                getSharedPreferences("user_session", MODE_PRIVATE);

        userId = preferences.getInt("user_id", -1);

        String role = preferences.getString("user_role", "");

        if (userId == -1 || !role.equals("employee")) {
            finish();
            return;
        }

        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnSubmitLeave = findViewById(R.id.btnSubmitLeave);
        btnBack = findViewById(R.id.btnBack);

        etReason = findViewById(R.id.etReason);

        // Choisir la date de debut
        btnStartDate.setOnClickListener(v -> showDatePicker(true));

        // Choisir la date de fin
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        // Envoyer la demande
        btnSubmitLeave.setOnClickListener(v -> submitLeave());

        // Retour
        btnBack.setOnClickListener(v -> finish());
    }

    // Afficher le calendrier
    private void showDatePicker(boolean isStartDate) {

        Calendar calendar = Calendar.getInstance();

        if (isStartDate && !startDate.isEmpty()) {
            calendar.setTime(Date.valueOf(startDate));
        } else if (!isStartDate && !endDate.isEmpty()) {
            calendar.setTime(Date.valueOf(endDate));
        }

        long minimumDate = System.currentTimeMillis();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();

                    selected.set(year, month, dayOfMonth);

                    String date = dateFormat.format(
                            selected.getTime()
                    );

                    if (isStartDate) {

                        startDate = date;

                        btnStartDate.setText(date);

                        if (!endDate.isEmpty()
                                && endDate.compareTo(startDate) < 0) {

                            endDate = "";
                            btnEndDate.setText("Choisir la date de fin");
                        }

                    } else {

                        if (startDate.isEmpty()) {
                            showMessage(
                                    "Choisissez d'abord la date de début."
                            );
                            return;
                        }

                        if (date.compareTo(startDate) < 0) {
                            showMessage(
                                    "La date de fin doit être après la date de début."
                            );
                            return;
                        }

                        endDate = date;
                        btnEndDate.setText(date);
                    }

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMinDate(minimumDate);

        datePicker.show();
    }

    // Enregistrer la demande
    private void submitLeave() {

        String reason = etReason.getText().toString().trim();

        if (startDate.isEmpty() || endDate.isEmpty()
                || reason.isEmpty()) {

            showMessage("Veuillez remplir tous les champs.");
            return;
        }

        boolean success = databaseHelper.addLeave(
                userId,
                startDate,
                endDate,
                reason
        );

        if (success) {

            showMessage("Demande envoyée avec succès !");

            finish();

        } else {

            showMessage("Erreur lors de l'envoi de la demande.");
        }
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
