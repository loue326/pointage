package com.example.gestionpointage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.User;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

/**
 * Connexion employé / administrateur.
 * Le rôle est choisi avec deux puces au-dessus du formulaire.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private TextView chipEmployee, chipAdmin, errorMessage, demoHint;
    private ProgressBar progress;
    private boolean adminSelected = false;
    private boolean passwordVisible = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        chipEmployee = findViewById(R.id.chipEmployee);
        chipAdmin = findViewById(R.id.chipAdmin);
        errorMessage = findViewById(R.id.errorMessage);
        progress = findViewById(R.id.loginProgress);
        demoHint = findViewById(R.id.demoHint);
        TextView btnToggle = findViewById(R.id.btnTogglePassword);
        TextView btnLogin = findViewById(R.id.btnLogin);

        selectRole(false);

        chipEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole(false);
            }
        });

        chipAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole(true);
            }
        });

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordVisible = !passwordVisible;
                inputPassword.setInputType(passwordVisible
                        ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                inputPassword.setSelection(inputPassword.getText().length());
                btnToggle.setText(passwordVisible ? "Masquer" : getString(R.string.btn_show_password));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
            }
        });
    }

    /** Change le rôle sélectionné et le texte d'aide */
    private void selectRole(boolean admin) {
        adminSelected = admin;

        chipEmployee.setSelected(!admin);
        chipAdmin.setSelected(admin);

        chipEmployee.setAlpha(admin ? 0.65f : 1f);
        chipAdmin.setAlpha(admin ? 1f : 0.65f);

        demoHint.setText(admin ? R.string.demo_admin : R.string.demo_emp);
        hideError();
    }

    /** Vérification des identifiants dans un thread séparé */
    private void attemptLogin() {

        final String email = inputEmail.getText().toString().trim();
        final String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre e-mail et votre mot de passe.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("L'adresse e-mail n'est pas valide.");
            return;
        }

        hideError();
        setBusy(true);

        new Thread(new Runnable() {
            @Override
            public void run() {

                final DatabaseHelper db = new DatabaseHelper(LoginActivity.this);
                String role = db.login(email, password);

                handler.post(new Runnable() {
                    @Override
                    public void run() {

                        if (isFinishing() || isDestroyed()) return;

                        if (role == null) {
                            setBusy(false);
                            showError("Identifiants incorrects. Essayez les comptes de démonstration.");
                            return;
                        }

                        boolean isAdmin = "admin".equals(role);

                        if (isAdmin != adminSelected) {
                            setBusy(false);
                            showError(adminSelected
                                    ? "Ce compte est un compte employé, pas administrateur."
                                    : "Ce compte est un compte administrateur, pas employé.");
                            return;
                        }

                        // Enregistrement de la session
                        int userId = db.getUserId(email);
                        User user = db.getUserById(userId);

                        new SessionManager(LoginActivity.this).saveSession(
                                userId, email, user.name, role);

                        setBusy(false);

                        Intent intent = new Intent(LoginActivity.this, isAdmin
                                ? AdminDashboardActivity.class
                                : EmployeeDashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        }).start();
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnLogin).setEnabled(!busy);
        findViewById(R.id.btnLogin).setAlpha(busy ? 0.6f : 1f);
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorMessage.setVisibility(View.GONE);
    }
}
