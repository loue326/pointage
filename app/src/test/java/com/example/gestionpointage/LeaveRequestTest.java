package com.example.gestionpointage;

import com.example.gestionpointage.model.LeaveRequest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests unitaires pour le calcul de duree et les statuts d'une demande de conge.
 */
public class LeaveRequestTest {

    @Test
    public void dateDiffDays_includesStartAndEnd() {
        assertEquals(1, LeaveRequest.dateDiffDays("2026-11-27", "2026-11-27"));
        assertEquals(2, LeaveRequest.dateDiffDays("2026-11-27", "2026-11-28"));
        assertEquals(5, LeaveRequest.dateDiffDays("2026-03-27", "2026-03-31"));
    }

    @Test
    public void statusLabels_matchExpectedFrenchLabels() {
        LeaveRequest req = new LeaveRequest();
        req.status = LeaveRequest.PENDING;
        assertEquals("En attente", req.getStatusLabel());

        req.status = LeaveRequest.APPROVED;
        assertEquals("Accepté", req.getStatusLabel());

        req.status = LeaveRequest.REJECTED;
        assertEquals("Refusé", req.getStatusLabel());
    }
}
