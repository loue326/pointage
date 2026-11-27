package com.example.gestionpointage.model;

import java.util.Calendar;

/**
 * Une pause : type, debut et fin.
 */
public class BreakRecord {

    public int id;
    public int userId;
    public int attendanceId;
    public String type;       // Dejeuner, Cafe, Personnelle, Medicale, Priere, Normale, Exceptionnelle
    public String startTime;  // HH:mm:ss
    public String endTime;    // HH:mm:ss (null = pause en cours)

    public BreakRecord() {
    }

    public BreakRecord(int id, int userId, int attendanceId,
                       String type, String startTime, String endTime) {
        this.id = id;
        this.userId = userId;
        this.attendanceId = attendanceId;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isOpen() {
        return endTime == null || endTime.isEmpty();
    }

    /**
     * Emoticone selon le type de pause.
     */
    public String getEmoji() {
        if (type == null) return "⏱";
        String t = type.toLowerCase();
        if (t.contains("déjeuner") || t.contains("dejeuner")) return "🍽️";
        if (t.contains("café") || t.contains("cafe")) return "☕";
        if (t.contains("personnelle")) return "💼";
        if (t.contains("médicale") || t.contains("medicale")) return "🏥";
        if (t.contains("prière") || t.contains("priere")) return "🕌";
        if (t.contains("exceptionnelle")) return "⚡";
        if (t.contains("normale")) return "⏸️";
        return "⏸";
    }

    public long durationMinutes() {
        if (startTime == null) return 0;
        long start = toMinutes(startTime);
        long end = endTime == null ? currentMinutes() : toMinutes(endTime);
        return Math.max(0, end - start);
    }

    private long currentMinutes() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60L
                + c.get(Calendar.MINUTE);
    }

    private static long toMinutes(String hhmmss) {
        if (hhmmss == null || hhmmss.length() < 5) return 0;
        String[] p = hhmmss.split(":");
        try {
            return Long.parseLong(p[0]) * 60 + Long.parseLong(p[1]);
        } catch (Exception e) {
            return 0;
        }
    }
}
