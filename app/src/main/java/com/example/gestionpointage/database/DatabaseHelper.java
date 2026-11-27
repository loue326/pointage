package com.example.gestionpointage.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.gestionpointage.model.AttendanceRecord;
import com.example.gestionpointage.model.BreakRecord;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Base de donnees locale SQLite.
 *
 * Tables :
 *   users       -> comptes (admin / employee)
 *   attendance  -> pointages (arrivee / sortie)
 *   breaks      -> pauses (type, debut, fin)
 *   leaves      -> demandes de conge
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gestion_pointage.db";
    private static final int DATABASE_VERSION = 6;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String TABLE_BREAKS = "breaks";
    public static final String TABLE_LEAVES = "leaves";

    // Types de pause proposés a l'employe
    public static final String[] BREAK_TYPES = {
            "Pause normale",
            "Déjeuner",
            "Café",
            "Personnelle",
            "Médicale",
            "Prière",
            "Pause exceptionnelle"
    };

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

        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT NOT NULL UNIQUE, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "job TEXT)");

        // Plusieurs pointages possibles par jour (sortie puis nouvelle entrée)
        db.execSQL("CREATE TABLE " + TABLE_ATTENDANCE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "date TEXT NOT NULL, " +
                "arrival_time TEXT, " +
                "exit_time TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_BREAKS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "attendance_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "start_time TEXT NOT NULL, " +
                "end_time TEXT, " +
                "FOREIGN KEY(user_id) REFERENCES users(id), " +
                "FOREIGN KEY(attendance_id) REFERENCES attendance(id)" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_LEAVES + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "type TEXT, " +
                "start_date TEXT NOT NULL, " +
                "end_date TEXT NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "status TEXT DEFAULT 'pending', " +
                "decision_by INTEGER, " +
                "decision_at TEXT, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ")");

        insertDefaultUsers(db);
        insertDemoData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAVES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BREAKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // =========================================================
    //  Utilitaires
    // =========================================================

    /** Cryptage SHA-256 des mots de passe */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b & 0xff));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    public static String now() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public static String nowFull() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    /** Transforme "08:30:15" en "08:30" */
    public static String shortTime(String time) {
        if (time == null || time.length() < 5) return time == null ? "--:--" : time;
        return time.substring(0, 5);
    }

    // =========================================================
    //  Comptes par defaut + donnees de demonstration
    // =========================================================

    private void insertDefaultUsers(SQLiteDatabase db) {
        insertUser(db, "Administrateur", "admin@gmail.com", "admin123", "admin", "Direction");
        insertUser(db, "Loue Bessaadi", "loue@gmail.com", "emp123", "employee", "Développeur");
        insertUser(db, "Amani", "amani@gmail.com", "emp123", "employee", "Ingénieur");
        insertUser(db, "Balkis", "balkis@gmail.com", "emp123", "employee", "Designer");
    }

    private void insertUser(SQLiteDatabase db, String name, String email,
                            String password, String role, String job) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", hashPassword(password));
        values.put("role", role);
        values.put("job", job);
        db.insert(TABLE_USERS, null, values);
    }

    /** Quelques pointages / pauses / conges pour montrer l'application en action */
    private void insertDemoData(SQLiteDatabase db) {

        String[] days = {"-4", "-3", "-2", "-1"};
        String[][] hours = {
                {"08:02:00", "16:35:00"},
                {"07:58:00", "16:41:00"},
                {"08:15:00", "17:05:00"},
                {"08:05:00", "16:50:00"}
        };

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (int dayIdx = 0; dayIdx < days.length; dayIdx++) {

            Date d = new Date(System.currentTimeMillis()
                    + Long.parseLong(days[dayIdx]) * 24L * 3600L * 1000L);
            String date = dayFormat.format(d);

            for (int userId = 2; userId <= 4; userId++) {

                ContentValues att = new ContentValues();
                att.put("user_id", userId);
                att.put("date", date);
                att.put("arrival_time", hours[dayIdx][0]);
                att.put("exit_time", hours[dayIdx][1]);
                long attendanceId = db.insert(TABLE_ATTENDANCE, null, att);

                if (attendanceId == -1) continue;

                ContentValues b = new ContentValues();
                b.put("user_id", userId);
                b.put("attendance_id", attendanceId);
                b.put("type", "Déjeuner");
                b.put("start_time", "12:1" + dayIdx + ":00");
                b.put("end_time", "13:0" + dayIdx + ":00");
                db.insert(TABLE_BREAKS, null, b);

                if (userId % 2 == 0) {
                    ContentValues b2 = new ContentValues();
                    b2.put("user_id", userId);
                    b2.put("attendance_id", attendanceId);
                    b2.put("type", "Café");
                    b2.put("start_time", "10:1" + dayIdx + ":00");
                    b2.put("end_time", "10:2" + dayIdx + ":00");
                    db.insert(TABLE_BREAKS, null, b2);
                }
            }
        }

        // Demandes de conge de demonstration
        ContentValues leave1 = new ContentValues();
        leave1.put("user_id", 2); // Loue Bessaadi
        leave1.put("type", "Congé annuel");
        leave1.put("start_date", shiftDays(5));
        leave1.put("end_date", shiftDays(8));
        leave1.put("reason", "Vacances familiales");
        leave1.put("status", "pending");
        leave1.put("created_at", nowFull());
        db.insert(TABLE_LEAVES, null, leave1);

        ContentValues leave2 = new ContentValues();
        leave2.put("user_id", 3); // Amani
        leave2.put("type", "Congé maladie");
        leave2.put("start_date", shiftDays(2));
        leave2.put("end_date", shiftDays(3));
        leave2.put("reason", "Rendez-vous médical");
        leave2.put("status", "pending");
        leave2.put("created_at", nowFull());
        db.insert(TABLE_LEAVES, null, leave2);
    }

    private String shiftDays(int delta) {
        Date d = new Date(System.currentTimeMillis() + delta * 24L * 3600L * 1000L);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    // =========================================================
    //  AUTHENTIFICATION
    // =========================================================

    /** Retourne le role ("admin"/"employee") ou null si echec */
    public String login(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT role FROM users WHERE email = ? AND password = ?",
                new String[]{email.trim(), hashPassword(password)});
        String role = null;
        if (cursor.moveToFirst()) role = cursor.getString(0);
        cursor.close();
        return role;
    }

    public int getUserId(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ?",
                new String[]{email.trim()});
        int id = -1;
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, name, email, password, role, job FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)});
        User u = null;
        if (c.moveToFirst()) {
            u = new User(c.getInt(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.isNull(5) ? "" : c.getString(5));
        }
        c.close();
        return u;
    }

    public String getUserName(int userId) {
        User u = getUserById(userId);
        return u == null ? "" : u.name;
    }

    public List<User> getUsersByRole(String role) {
        SQLiteDatabase db = getReadableDatabase();
        List<User> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT id, name, email, password, role, job FROM users " +
                        "WHERE role = ? ORDER BY name COLLATE NOCASE",
                new String[]{role});
        while (c.moveToNext()) {
            list.add(new User(c.getInt(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.isNull(5) ? "" : c.getString(5)));
        }
        c.close();
        return list;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM users WHERE email = ?",
                new String[]{email.trim()});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    /** Ajout d'un employe par l'administrateur */
    public boolean addUser(String name, String email, String password,
                           String role, String job) {
        if (emailExists(email)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("email", email);
        v.put("password", hashPassword(password));
        v.put("role", role);
        v.put("job", job);
        return db.insert(TABLE_USERS, null, v) != -1;
    }

    // =========================================================
    //  POINTAGE
    // =========================================================

    private int getAttendanceId(int userId, boolean onlyOpen) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = onlyOpen
                ? "SELECT id FROM attendance WHERE user_id = ? AND date = ? AND exit_time IS NULL ORDER BY id DESC LIMIT 1"
                : "SELECT id FROM attendance WHERE user_id = ? AND date = ? ORDER BY id DESC LIMIT 1";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(userId), today()});
        int id = -1;
        if (c.moveToFirst()) id = c.getInt(0);
        c.close();
        return id;
    }

    public boolean hasAttendanceToday(int userId) {
        return getAttendanceId(userId, false) != -1;
    }

    public boolean isClockedIn(int userId) {
        return getAttendanceId(userId, true) != -1;
    }

    public boolean markArrival(int userId) {
        if (isClockedIn(userId)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("date", today());
        v.put("arrival_time", now());
        return db.insert(TABLE_ATTENDANCE, null, v) != -1;
    }

    public boolean markExit(int userId) {
        int attendanceId = getAttendanceId(userId, true);
        if (attendanceId == -1 || hasOpenBreak(userId)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("exit_time", now());
        return db.update(TABLE_ATTENDANCE, v, "id = ?",
                new String[]{String.valueOf(attendanceId)}) > 0;
    }

    /** Pointage du jour avec toutes les pauses associees */
    public AttendanceRecord getTodayAttendance(int userId) {
        AttendanceRecord record = getAttendanceById(getAttendanceId(userId, false));
        if (record != null) {
            User u = getUserById(userId);
            if (u != null) {
                record.userName = u.name;
                record.job = u.job;
            }
        }
        return record;
    }

    private AttendanceRecord getAttendanceById(int attendanceId) {
        if (attendanceId == -1) return null;
        SQLiteDatabase db = getReadableDatabase();
        AttendanceRecord record = null;
        Cursor c = db.rawQuery(
                "SELECT id, user_id, date, arrival_time, exit_time " +
                        "FROM attendance WHERE id = ?",
                new String[]{String.valueOf(attendanceId)});
        if (c.moveToFirst()) {
            record = new AttendanceRecord();
            record.id = c.getInt(0);
            record.userId = c.getInt(1);
            record.date = c.getString(2);
            record.arrivalTime = c.isNull(3) ? null : c.getString(3);
            record.exitTime = c.isNull(4) ? null : c.getString(4);
            record.breaks = getBreaksByAttendance(attendanceId);
        }
        c.close();
        return record;
    }

    private List<BreakRecord> getBreaksByAttendance(int attendanceId) {
        SQLiteDatabase db = getReadableDatabase();
        List<BreakRecord> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT id, user_id, attendance_id, type, start_time, end_time " +
                        "FROM breaks WHERE attendance_id = ? ORDER BY id DESC",
                new String[]{String.valueOf(attendanceId)});
        while (c.moveToNext()) {
            list.add(new BreakRecord(c.getInt(0), c.getInt(1), c.getInt(2),
                    c.getString(3), c.getString(4), c.isNull(5) ? null : c.getString(5)));
        }
        c.close();
        return list;
    }

    /** Historique complet d'un employe (journee la plus recente en premier) */
    public List<AttendanceRecord> getHistory(int userId, int limit) {
        SQLiteDatabase db = getReadableDatabase();
        List<AttendanceRecord> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT id FROM attendance WHERE user_id = ? " +
                        "ORDER BY date DESC, id DESC LIMIT ?",
                new String[]{String.valueOf(userId), String.valueOf(limit)});
        while (c.moveToNext()) {
            AttendanceRecord r = getAttendanceById(c.getInt(0));
            if (r != null) {
                User u = getUserById(userId);
                if (u != null) {
                    r.userName = u.name;
                    r.job = u.job;
                }
                list.add(r);
            }
        }
        c.close();
        return list;
    }

    // =========================================================
    //  PAUSES
    // =========================================================

    public boolean hasOpenBreak(int userId) {
        return getOpenBreak(userId) != null;
    }

    public BreakRecord getOpenBreak(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, user_id, attendance_id, type, start_time, end_time " +
                        "FROM breaks WHERE user_id = ? AND end_time IS NULL " +
                        "ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(userId)});
        BreakRecord b = null;
        if (c.moveToFirst()) {
            b = new BreakRecord(c.getInt(0), c.getInt(1), c.getInt(2),
                    c.getString(3), c.getString(4), c.isNull(5) ? null : c.getString(5));
        }
        c.close();
        return b;
    }

    public boolean startBreak(int userId, String type) {
        int attendanceId = getAttendanceId(userId, true);
        if (attendanceId == -1 || hasOpenBreak(userId)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("attendance_id", attendanceId);
        v.put("type", type);
        v.put("start_time", now());
        return db.insert(TABLE_BREAKS, null, v) != -1;
    }

    public boolean endBreak(int userId) {
        if (!hasOpenBreak(userId)) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("end_time", now());
        return db.update(TABLE_BREAKS, v, "user_id = ? AND end_time IS NULL",
                new String[]{String.valueOf(userId)}) > 0;
    }

    /** Toutes les pauses d'une journee, tous employes confondus */
    public List<BreakRecord> getBreaksOfDay(String date) {
        SQLiteDatabase db = getReadableDatabase();
        List<BreakRecord> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT b.id, b.user_id, b.attendance_id, b.type, b.start_time, b.end_time " +
                        "FROM breaks b JOIN attendance a ON a.id = b.attendance_id " +
                        "WHERE a.date = ? ORDER BY b.id DESC",
                new String[]{date});
        while (c.moveToNext()) {
            list.add(new BreakRecord(c.getInt(0), c.getInt(1), c.getInt(2),
                    c.getString(3), c.getString(4), c.isNull(5) ? null : c.getString(5)));
        }
        c.close();
        return list;
    }

    /** Pseudo : nom de l'employe lie a une pause */
    public String userNameOfBreak(BreakRecord b) {
        return getUserName(b.userId);
    }

    public int countOpenBreaksToday() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM breaks WHERE end_time IS NULL", null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    // =========================================================
    //  STATISTIQUES (tableau de bord)
    // =========================================================

    public int countUsers(String role) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = ?",
                new String[]{role});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int countPresentToday() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(DISTINCT user_id) FROM attendance WHERE date = ?",
                new String[]{today()});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int countClockedInToday() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM attendance WHERE date = ? AND exit_time IS NULL",
                new String[]{today()});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int countPendingLeaves() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM leaves WHERE status = 'pending'", null);
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    public int countLeaves(String status, Integer userId) {
        SQLiteDatabase db = getReadableDatabase();
        if (userId == null) {
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM leaves WHERE status = ?", new String[]{status});
            int n = c.moveToFirst() ? c.getInt(0) : 0;
            c.close();
            return n;
        }
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM leaves WHERE status = ? AND user_id = ?",
                new String[]{status, String.valueOf(userId)});
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    public int countBreaksToday() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM breaks b JOIN attendance a ON a.id = b.attendance_id " +
                        "WHERE a.date = ?", new String[]{today()});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    /** Nombre de jours d'arrivee moyens d'un employe (utilise pour le % du dashboard) */
    public int countAttendanceOf(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM attendance WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        int n = 0;
        if (c.moveToFirst()) n = c.getInt(0);
        c.close();
        return n;
    }

    // =========================================================
    //  CONGES
    // =========================================================

    public boolean addLeave(int userId, String type, String startDate,
                            String endDate, String reason) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("type", type);
        v.put("start_date", startDate);
        v.put("end_date", endDate);
        v.put("reason", reason);
        v.put("status", LeaveRequest.PENDING);
        v.put("created_at", nowFull());
        return db.insert(TABLE_LEAVES, null, v) != -1;
    }

    public boolean updateLeaveStatus(int leaveId, String status, int adminId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("status", status);
        v.put("decision_by", adminId);
        v.put("decision_at", nowFull());
        return db.update(TABLE_LEAVES, v, "id = ?",
                new String[]{String.valueOf(leaveId)}) > 0;
    }

    /** Conges d'un employe (tous statuts) */
    public List<LeaveRequest> getLeavesOf(int userId) {
        return queryLeaves("l.user_id = ? ", new String[]{String.valueOf(userId)},
                "l.id DESC");
    }

    /** Conges filtres pour la page administrateur */
    public List<LeaveRequest> getLeaves(String status) {
        if (status == null) {
            return queryLeaves("1 = 1", new String[]{}, "l.id DESC");
        }
        return queryLeaves("l.status = ?", new String[]{status}, "l.id DESC");
    }

    private List<LeaveRequest> queryLeaves(String where, String[] args, String order) {
        SQLiteDatabase db = getReadableDatabase();
        List<LeaveRequest> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT l.id, l.user_id, u.name, u.job, l.type, l.start_date, " +
                        "l.end_date, l.reason, l.status, l.created_at " +
                        "FROM leaves l LEFT JOIN users u ON u.id = l.user_id " +
                        "WHERE " + where + " ORDER BY " + order, args);
        while (c.moveToNext()) {
            LeaveRequest l = new LeaveRequest();
            l.id = c.getInt(0);
            l.userId = c.getInt(1);
            l.userName = c.isNull(2) ? "" : c.getString(2);
            l.job = c.isNull(3) ? "" : c.getString(3);
            l.type = c.isNull(4) ? "Congé" : c.getString(4);
            l.startDate = c.getString(5);
            l.endDate = c.getString(6);
            l.reason = c.getString(7);
            l.status = c.getString(8);
            l.createdAt = c.isNull(9) ? "" : c.getString(9);
            list.add(l);
        }
        c.close();
        return list;
    }

    public LeaveRequest getLeaveById(int leaveId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, user_id, type, start_date, end_date, reason, status " +
                        "FROM leaves WHERE id = ?",
                new String[]{String.valueOf(leaveId)});
        LeaveRequest l = null;
        if (c.moveToFirst()) {
            l = new LeaveRequest();
            l.id = c.getInt(0);
            l.userId = c.getInt(1);
            l.type = c.getString(2);
            l.startDate = c.getString(3);
            l.endDate = c.getString(4);
            l.reason = c.getString(5);
            l.status = c.getString(6);
        }
        c.close();
        return l;
    }

    /** Suppression d'une demande encore en attente */
    public boolean deleteLeave(int leaveId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_LEAVES, "id = ? AND status = 'pending'",
                new String[]{String.valueOf(leaveId)}) > 0;
    }
}
