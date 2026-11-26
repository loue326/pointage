package com.example.gestionpointage.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Informations de la base de donnees
    private static final String DATABASE_NAME = "gestion_pointage.db";
    private static final int DATABASE_VERSION = 1;

    // Table utilisateurs
    public static final String TABLE_USERS = "users";

    // Table pointages
    public static final String TABLE_ATTENDANCE = "attendance";

    // Table pauses
    public static final String TABLE_BREAKS = "breaks";

    // Table conges
    public static final String TABLE_LEAVES = "leaves";

    // Table notifications
    public static final String TABLE_NOTIFICATIONS = "notifications";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Creation de la table utilisateurs
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL CHECK(role IN ('admin','employee'))" +
                ")");

        // Creation de la table pointages
        db.execSQL("CREATE TABLE attendance (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "date TEXT NOT NULL, " +
                "arrival_time TEXT, " +
                "exit_time TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ")");

        // Creation de la table pauses
        db.execSQL("CREATE TABLE breaks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "attendance_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "start_time TEXT NOT NULL, " +
                "end_time TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(id), " +
                "FOREIGN KEY(attendance_id) REFERENCES attendance(id)" +
                ")");

        // Creation de la table conges
        db.execSQL("CREATE TABLE leaves (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "start_date TEXT NOT NULL, " +
                "end_date TEXT NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "status TEXT DEFAULT 'pending', " +
                "decision_by INTEGER, " +
                "decision_at TEXT, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES users(id), " +
                "FOREIGN KEY(decision_by) REFERENCES users(id)" +
                ")");

        // Creation des notifications
        db.execSQL("CREATE TABLE notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "message TEXT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "is_read INTEGER DEFAULT 0, " +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ")");

        // Creation des comptes de demonstration
        insertDefaultUsers(db);
    }

    // Ajouter les comptes de demonstration
    private void insertDefaultUsers(SQLiteDatabase db) {

        ContentValues admin = new ContentValues();
        admin.put("name", "Administrateur");
        admin.put("email", "admin@gmail.com");
        admin.put("password", hashPassword("admin123"));
        admin.put("role", "admin");

        db.insert(TABLE_USERS, null, admin);

        ContentValues employee = new ContentValues();
        employee.put("name", "Employe Test");
        employee.put("email", "employee@gmail.com");
        employee.put("password", hashPassword("emp123"));
        employee.put("role", "employee");

        db.insert(TABLE_USERS, null, employee);
    }

    // Crypter le mot de passe avec SHA-256
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b & 0xff));
            }

            return result.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Verifier les identifiants et recuperer le role
    public String login(String email, String password) {

        SQLiteDatabase db = this.getReadableDatabase();

        String hashedPassword = hashPassword(password);

        Cursor cursor = db.rawQuery(
                "SELECT role FROM users WHERE email = ? AND password = ?",
                new String[]{email, hashedPassword}
        );

        String role = null;

        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }

        cursor.close();

        return role;
    }

    public int getUserId(String email) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM users WHERE email = ?",
                new String[]{email}
        );

        int userId = -1;

        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }

        cursor.close();

        return userId;
    }

    public String getUserName(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT name FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        String name = "";

        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }

        cursor.close();

        return name;
    }

    // Recuperer la date actuelle
    private String getCurrentDate() {
        return new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault()
        ).format(new Date());
    }

    // Recuperer l'heure actuelle
    private String getCurrentTime() {
        return new SimpleDateFormat(
                "HH:mm:ss", Locale.getDefault()
        ).format(new Date());
    }

    // Chercher le pointage actif du jour
    public int getTodayAttendanceId(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM attendance " +
                "WHERE user_id = ? AND date = ? AND exit_time IS NULL",
                new String[]{
                        String.valueOf(userId),
                        getCurrentDate()
                }
        );

        int attendanceId = -1;

        if (cursor.moveToFirst()) {
            attendanceId = cursor.getInt(0);
        }

        cursor.close();

        return attendanceId;
    }

    // Verifier si l'employe a deja pointe aujourd'hui
    public boolean hasAttendanceToday(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM attendance WHERE user_id = ? AND date = ?",
                new String[]{
                        String.valueOf(userId),
                        getCurrentDate()
                }
        );

        boolean exists = cursor.moveToFirst();

        cursor.close();

        return exists;
    }

    // Enregistrer l'arrivee
    public boolean markArrival(int userId) {

        if (hasAttendanceToday(userId)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("date", getCurrentDate());
        values.put("arrival_time", getCurrentTime());

        long result = db.insert(TABLE_ATTENDANCE, null, values);

        return result != -1;
    }

    // Verifier si une pause est deja ouverte
    public boolean hasOpenBreak(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT id FROM breaks " +
                "WHERE user_id = ? AND end_time IS NULL",
                new String[]{String.valueOf(userId)}
        );

        boolean exists = cursor.moveToFirst();

        cursor.close();

        return exists;
    }

    // Commencer une pause
    public boolean startBreak(int userId, String type) {

        int attendanceId = getTodayAttendanceId(userId);

        if (attendanceId == -1 || hasOpenBreak(userId)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("attendance_id", attendanceId);
        values.put("type", type);
        values.put("start_time", getCurrentTime());

        long result = db.insert(TABLE_BREAKS, null, values);

        return result != -1;
    }

    // Terminer la pause
    public boolean endBreak(int userId) {

        if (!hasOpenBreak(userId)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("end_time", getCurrentTime());

        int result = db.update(
                TABLE_BREAKS,
                values,
                "user_id = ? AND end_time IS NULL",
                new String[]{String.valueOf(userId)}
        );

        return result > 0;
    }

    // Enregistrer la sortie
    public boolean markExit(int userId) {

        int attendanceId = getTodayAttendanceId(userId);

        if (attendanceId == -1 || hasOpenBreak(userId)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("exit_time", getCurrentTime());

        int result = db.update(
                TABLE_ATTENDANCE,
                values,
                "id = ? AND exit_time IS NULL",
                new String[]{String.valueOf(attendanceId)}
        );

        return result > 0;
    }

    // Recuperer l'historique des activites
    public String getAttendanceHistory(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder history = new StringBuilder();

        Cursor cursor = db.rawQuery(
                "SELECT id, date, arrival_time, exit_time " +
                "FROM attendance WHERE user_id = ? " +
                "ORDER BY id DESC",
                new String[]{String.valueOf(userId)}
        );

        while (cursor.moveToNext()) {

            int attendanceId = cursor.getInt(0);

            String date = cursor.getString(1);
            String arrival = cursor.getString(2);
            String exit = cursor.getString(3);

            history.append("Date : ").append(date).append("\n");

            if (arrival != null) {
                history.append("Arrivée : ").append(arrival).append("\n");
            }

            Cursor breaksCursor = db.rawQuery(
                    "SELECT type, start_time, end_time FROM breaks " +
                    "WHERE attendance_id = ? ORDER BY id DESC",
                    new String[]{String.valueOf(attendanceId)}
            );

            while (breaksCursor.moveToNext()) {

                history.append("Pause : ")
                        .append(breaksCursor.getString(0))
                        .append("\nDébut : ")
                        .append(breaksCursor.getString(1))
                        .append("\nFin : ")
                        .append(breaksCursor.isNull(2)
                                ? "En cours"
                                : breaksCursor.getString(2))
                        .append("\n");
            }

            breaksCursor.close();

            if (exit != null) {
                history.append("Sortie : ").append(exit).append("\n");
            } else {
                history.append("Journée en cours\n");
            }

            history.append("--------------------\n");
        }

        cursor.close();

        if (history.length() == 0) {
            return "Aucune activité enregistrée.";
        }

        return history.toString();
    }

    public boolean addLeave(int userId, String startDate,
                            String endDate, String reason) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("user_id", userId);
        values.put("start_date", startDate);
        values.put("end_date", endDate);
        values.put("reason", reason);
        values.put("status", "pending");
        values.put("created_at", getCurrentDate() + " " + getCurrentTime());

        long result = db.insert(TABLE_LEAVES, null, values);

        return result != -1;
    }

    public String getLeaveHistory(int userId) {

        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder history = new StringBuilder();

        Cursor cursor = db.rawQuery(
                "SELECT start_date, end_date, reason, status " +
                "FROM leaves WHERE user_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(userId)}
        );

        while (cursor.moveToNext()) {

            history.append("Début : ")
                    .append(cursor.getString(0))
                    .append("\n");

            history.append("Fin : ")
                    .append(cursor.getString(1))
                    .append("\n");

            history.append("Motif : ")
                    .append(cursor.getString(2))
                    .append("\n");

            history.append("Statut : ")
                    .append(cursor.getString(3))
                    .append("\n");

            history.append("--------------------\n");
        }

        cursor.close();

        if (history.length() == 0) {
            return "Aucune demande de congé.";
        }

        return history.toString();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ajouter les migrations de base de donnees ici
    }
}
