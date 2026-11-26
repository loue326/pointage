package com.example.gestionpointage;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;

public class TestDatabaseActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resultText = new TextView(this);
        resultText.setPadding(30, 50, 30, 30);
        resultText.setTextSize(18);

        setContentView(resultText);

        databaseHelper = new DatabaseHelper(this);

        // Tester le compte administrateur
        String adminRole = databaseHelper.login(
                "admin@gmail.com",
                "admin123"
        );

        // Tester le compte employe
        String employeeRole = databaseHelper.login(
                "employee@gmail.com",
                "emp123"
        );

        if ("admin".equals(adminRole) &&
                "employee".equals(employeeRole)) {

            resultText.setText(
                    "SUCCESS !\n\n" +
                    "Base SQLite : OK\n" +
                    "Compte Admin : OK\n" +
                    "Compte Employe : OK"
            );

        } else {
            resultText.setText("Erreur de configuration SQLite");
        }
    }
}
