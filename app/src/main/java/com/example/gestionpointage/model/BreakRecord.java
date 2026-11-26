package com.example.gestionpointage.model;

/**
 * Une pause : type, debut et fin.
 */
public class BreakRecord {

    public int id;
    public int userId;
    public int attendanceId;
    public String type;       // Dejeuner, Cafe, Personnelle, Medicale, Priere
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
        switch (type.toLowerCase()) {
            case "dejeuner":
            case "déjeuner":
                return "🍽️";
            case "cafe":
            case "café":
                return "☕";
            case "personnelle":
                return "💼";
            case "medicale":
            case "médicale":
                return "🏥";
            case "priere":
            case "prière":
                return "🕌";
            default:
                return "⏸";
        }
    }

    public long durationMinutes() {
        if (startTime == null) return 0;
        long start = toMinutes(startTime);
        long end = endTime == null ? currentMinutes() : toMinutes(endTime);
        return Math.max(0, end - start);
    }

    private long currentMinutes() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60L
                + c.get(java.util.Calendar.MINUTE);
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
