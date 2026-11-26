package com.example.gestionpointage.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Petits utilitaires d'interface.
 */
public final class Ui {

    private Ui() {
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    /** "2026-01-15" -> "15/01/2026" */
    public static String isoToFr(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return isoDate == null ? "" : isoDate;
        String[] p = isoDate.split("-");
        if (p.length != 3) return isoDate;
        return p[2] + "/" + p[1] + "/" + p[0];
    }

    /** "2026-01-15" -> "15 janv." */
    public static String isoToShort(String isoDate) {
        String[] months = {"janv.", "févr.", "mars", "avr.", "mai", "juin",
                "juil.", "août", "sept.", "oct.", "nov.", "déc."};
        if (isoDate == null || isoDate.length() < 10) return isoDate == null ? "" : isoDate;
        String[] p = isoDate.split("-");
        if (p.length != 3) return isoDate;
        try {
            int m = Integer.parseInt(p[1]) - 1;
            int d = Integer.parseInt(p[2]);
            return d + " " + months[Math.max(0, Math.min(11, m))];
        } catch (Exception e) {
            return isoToFr(isoDate);
        }
    }

    /** "2026-01-15" -> "Lundi 15 janvier" */
    public static String isoToLong(String isoDate) {
        try {
            java.text.SimpleDateFormat in =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("EEEE d MMMM yyyy", java.util.Locale.FRENCH);
            return out.format(in.parse(isoDate));
        } catch (Exception e) {
            return isoDate;
        }
    }

    /** Heures+minutes -> "8h05" */
    public static String minutesToLabel(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h" + (m < 10 ? "0" : "") + m;
    }

    public static String nowIso() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(new java.util.Date());
    }
}
