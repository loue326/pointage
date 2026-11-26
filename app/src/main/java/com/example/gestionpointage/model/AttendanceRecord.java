package com.example.gestionpointage.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Une journee de pointage : arrivee + sortie + toutes les pauses associees.
 */
public class AttendanceRecord {

    public int id;
    public int userId;
    public String userName;
    public String job;
    public String date;          // yyyy-MM-dd
    public String arrivalTime;   // HH:mm:ss (null si non pointe)
    public String exitTime;      // HH:mm:ss (null si toujours present)

    public List<BreakRecord> breaks = new ArrayList<>();

    public AttendanceRecord() {
    }

    public boolean isOpen() {
        return exitTime == null || exitTime.isEmpty();
    }

    /**
     * Duree totale des pauses (en minutes).
     */
    public long getBreakMinutes() {
        long total = 0;
        for (BreakRecord b : breaks) {
            total += b.durationMinutes();
        }
        return total;
    }

    /**
     * Duree totale de travail (heure arrivee -> heure sortie - pauses).
     * Retourne -1 si la journee n'est pas terminee.
     */
    public long getWorkedMinutes() {
        if (arrivalTime == null) return -1;
        long start = toMinutes(arrivalTime);
        long end = exitTime == null ? -1 : toMinutes(exitTime);
        if (end < 0) return -1;
        return Math.max(0, end - start - getBreakMinutes());
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
