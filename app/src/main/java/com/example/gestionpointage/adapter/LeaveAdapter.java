package com.example.gestionpointage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.R;
import com.example.gestionpointage.model.LeaveRequest;
import com.example.gestionpointage.util.Ui;

import java.util.List;
import java.util.Locale;

/**
 * Liste des demandes de congé.
 * Utilisé côté employé (sans boutons) et côté administrateur (avec Accepter / Refuser).
 */
public class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.Holder> {

    public interface OnLeaveAction {
        void onAccept(LeaveRequest leave);

        void onReject(LeaveRequest leave);
    }

    private List<LeaveRequest> items;
    private final boolean withActions;
    private OnLeaveAction listener;

    public LeaveAdapter(List<LeaveRequest> items, boolean withActions) {
        this.items = items;
        this.withActions = withActions;
    }

    public void setOnLeaveAction(OnLeaveAction listener) {
        this.listener = listener;
    }

    public void update(List<LeaveRequest> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leave, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        LeaveRequest leave = items.get(position);

        holder.type.setText(leave.type == null ? "Congé" : leave.type);

        String dates = Ui.isoToShort(leave.startDate) + " → " + Ui.isoToShort(leave.endDate);
        if (leave.userName != null && !leave.userName.isEmpty()) {
            holder.dates.setText(leave.userName + "  •  " + dates);
        } else {
            holder.dates.setText(dates);
        }

        holder.reason.setText("Motif : " + leave.reason);
        holder.days.setText(String.format(Locale.FRANCE, "%d jour%s",
                leave.getDaysCount(), leave.getDaysCount() > 1 ? "s" : ""));

        // Statut coloré
        switch (leave.status == null ? LeaveRequest.PENDING : leave.status) {
            case LeaveRequest.APPROVED:
                holder.status.setText("Accepté");
                holder.status.setBackgroundResource(R.drawable.chip_approved);
                holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.success));
                break;
            case LeaveRequest.REJECTED:
                holder.status.setText("Refusé");
                holder.status.setBackgroundResource(R.drawable.chip_rejected);
                holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.danger));
                break;
            default:
                holder.status.setText("En attente");
                holder.status.setBackgroundResource(R.drawable.chip_pending);
                holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.warning));
                break;
        }

        // Boutons d'action (admin uniquement et demande en attente)
        if (withActions && leave.isPending()) {
            holder.actions.setVisibility(View.VISIBLE);

            holder.btnAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onAccept(leave);
                }
            });
            holder.btnReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onReject(leave);
                }
            });
        } else {
            holder.actions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView type, dates, status, reason, days, btnAccept, btnReject;
        LinearLayout actions;

        Holder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.leaveType);
            dates = itemView.findViewById(R.id.leaveDates);
            status = itemView.findViewById(R.id.leaveStatus);
            reason = itemView.findViewById(R.id.leaveReason);
            days = itemView.findViewById(R.id.leaveDays);
            actions = itemView.findViewById(R.id.leaveActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
