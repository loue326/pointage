package com.example.gestionpointage.model;

/**
 * Une demande de conge.
 */
public class LeaveRequest {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    public int id;
    public int userId;
    public String userName;   // rempli par la requete admin
    public String job;
    public String type;       // Conge annuel, Maladie, Sans solde...
    public String startDate;  // yyyy-MM-dd
    public String endDate;    // yyyy-MM-dd
    public String reason;
    public String status;     // pending / approved / rejected
    public String createdAt;

    public LeaveRequest() {
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public boolean isApproved() {
        return APPROVED.equals(status);
    }

    public String getStatusLabel() {
        switch (status == null ? PENDING : status) {
            case APPROVED:
                return "Accepté";
            case REJECTED:
                return "Refusé";
            default:
                return "En attente";
        }
    }

    /**
     * Nombre de jours demandes (debut et fin incluses).
     */
    public long getDaysCount() {
        return dateDiffDays(startDate, endDate);
    }

    public static long dateDiffDays(String start, String end) {
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d1 = f.parse(start);
            java.util.Date d2 = f.parse(end);
            long diff = d2.getTime() - d1.getTime();
            return Math.max(1, diff / (1000L * 60 * 60 * 24) + 1);
        } catch (Exception e) {
            return 1;
        }
    }
}
