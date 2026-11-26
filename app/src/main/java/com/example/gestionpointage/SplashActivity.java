package com.example.gestionpointage;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionpointage.session.SessionManager;

/**
 * Ecran de demarrage : verifie la session et redirige.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                SessionManager session = new SessionManager(SplashActivity.this);

                Intent intent;
                if (session.isLoggedIn()) {
                    intent = session.isAdmin()
                            ? new Intent(SplashActivity.this, AdminDashboardActivity.class)
                            : new Intent(SplashActivity.this, EmployeeDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }

                startActivity(intent);
                finish();
            }
        }, 1200);
    }
}
