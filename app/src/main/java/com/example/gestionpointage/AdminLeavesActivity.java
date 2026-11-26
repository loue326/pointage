package com.example.gestionpointage;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.adapter.LeaveAdapter;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.session.SessionManager;
import com.example.gestionpointage.util.Ui;

import java.util.List;

/**
 * Validation des demandes de congé par l'administrateur.
 * Filtres : tous / en attente / acceptés / refusés.
 */
public class AdminLeavesActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private LeaveAdapter adapter;

    private TextView filterAll, filterPending, filterApproved, filterRejected;
    private TextView countLabel, emptyView;

    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_leaves);

        db = new DatabaseHelper(this);
        session = new SessionManager(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        filterAll = findViewById(R.id.filterAll);
        filterPending = findViewById(R.id.filterPending);
        filterApproved = findViewById(R.id.filterApproved);
        filterRejected = findViewById(R.id.filterRejected);
        countLabel = findViewById(R.id.leavesCount);
        emptyView = findViewById(R.id.emptyLeaves);

        RecyclerView list = findViewById(R.id.leavesList);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LeaveAdapter(new java.util.ArrayList<>(), true);
        list.setAdapter(adapter);

        adapter.setOnLeaveAction(new LeaveAdapter.OnLeaveAction() {
            @Override
            public void onAccept(LeaveRequest leave) {
                decide(leave, LeaveRequest.APPROVED);
            }

            @Override
            public void onReject(LeaveRequest leave) {
                decide(leave, LeaveRequest.REJECTED);
            }
        });

        filterAll.setOnClickListener(v -> applyFilter("all"));
        filterPending.setOnClickListener(v -> applyFilter(LeaveRequest.PENDING));
        filterApproved.setOnClickListener(v -> applyFilter(LeaveRequest.APPROVED));
        filterRejected.setOnClickListener(v -> applyFilter(LeaveRequest.REJECTED));

        applyFilter("all");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        refresh();

        // Style des puces de filtre
        filterAll.setSelected("all".equals(filter));
        filterPending.setSelected(LeaveRequest.PENDING.equals(filter));
        filterApproved.setSelected(LeaveRequest.APPROVED.equals(filter));
        filterRejected.setSelected(LeaveRequest.REJECTED.equals(filter));
    }

    private void refresh() {
        List<LeaveRequest> leaves;

        if ("all".equals(currentFilter)) {
            leaves = db.getLeaves(null);
        } else {
            leaves = db.getLeaves(currentFilter);
        }

        adapter.update(leaves);

        countLabel.setText(leaves.size() + " demande" + (leaves.size() > 1 ? "s" : ""));
        emptyView.setVisibility(leaves.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Acceptation ou refus d'une demande */
    private void decide(final LeaveRequest leave, final String status) {

        String message = status.equals(LeaveRequest.APPROVED)
                ? "Accepter la demande de " + leave.userName + " ?"
                : "Refuser la demande de " + leave.userName + " ?";

        new android.app.AlertDialog.Builder(this)
                .setTitle(message)
                .setMessage(leave.type + " du " + Ui.isoToFr(leave.startDate)
                        + " au " + Ui.isoToFr(leave.endDate) + "\n\nMotif : " + leave.reason)
                .setPositiveButton(status.equals(LeaveRequest.APPROVED)
                        ? "Accepter" : "Refuser", (dialog, which) -> {

                    boolean ok = db.updateLeaveStatus(leave.id, status, session.getUserId());

                    Ui.toast(this, ok
                            ? (status.equals(LeaveRequest.APPROVED)
                            ? "Demande acceptée ✅" : "Demande refusée ❌")
                            : "Erreur lors de la mise à jour");

                    refresh();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
