package com.example.gestionpointage.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Session de l'utilisateur connecte (SharedPreferences).
 * On y conserve l'identifiant et le role afin d'eviter une reconnexion
 * a chaque ouverture de l'application.
 */
public class SessionManager {

    private static final String PREFS = "app_session";
    private static final String KEY_ID = "user_id";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_LOGGED = "logged_in";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(int id, String email, String name, String role) {
        prefs.edit()
                .putInt(KEY_ID, id)
                .putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putString(KEY_ROLE, role)
                .putBoolean(KEY_LOGGED, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED, false);
    }

    public int getUserId() {
        return prefs.getInt(KEY_ID, -1);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "");
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "");
    }

    public boolean isAdmin() {
        return "admin".equals(getRole());
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
