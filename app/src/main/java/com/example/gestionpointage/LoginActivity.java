package com.example.gestionpointage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Initialiser les composants
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Initialiser SQLite
        databaseHelper = new DatabaseHelper(this);

        // Bouton connexion
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Verifier les champs
        if (email.isEmpty() || password.isEmpty()) {

            Toast.makeText(
                    this,
                    "Veuillez remplir tous les champs",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Verifier les identifiants dans SQLite
        String role = databaseHelper.login(email, password);

        if (role != null) {

            int userId = databaseHelper.getUserId(email);

            // Enregistrer la session
            SharedPreferences preferences =
                    getSharedPreferences("user_session", MODE_PRIVATE);

            preferences.edit()
                    .putInt("user_id", userId)
                    .putString("user_email", email)
                    .putString("user_role", role)
                    .apply();

            Toast.makeText(
                    this,
                    "Connexion réussie !",
                    Toast.LENGTH_SHORT
            ).show();

            // Redirection selon le role
            if (role.equals("admin")) {

                Intent intent = new Intent(
                        LoginActivity.this,
                        AdminDashboardActivity.class
                );

                startActivity(intent);

            } else {

                Intent intent = new Intent(
                        LoginActivity.this,
                        EmployeeDashboardActivity.class
                );

                intent.putExtra("user_id", userId);

                startActivity(intent);
            }

            finish();

        } else {

            Toast.makeText(
                    this,
                    "Email ou mot de passe incorrect",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
