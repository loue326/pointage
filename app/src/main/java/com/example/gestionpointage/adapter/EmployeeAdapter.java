package com.example.gestionpointage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.R;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.User;
import com.example.gestionpointage.util.Ui;

import java.util.List;

/**
 * Liste des employés (espace administrateur).
 */
public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.Holder> {

    public interface OnUserClick {
        void onClick(User user);
    }

    private List<User> items;
    private OnUserClick listener;
    private final DatabaseHelper db;

    public EmployeeAdapter(List<User> items, DatabaseHelper db) {
        this.items = items;
        this.db = db;
    }

    public void setOnUserClick(OnUserClick listener) {
        this.listener = listener;
    }

    public void update(List<User> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        User user = items.get(position);

        holder.avatar.setText(user.getInitials());
        holder.name.setText(user.name);
        holder.job.setText(user.job == null || user.job.isEmpty() ? user.email : user.job);

        boolean present = db.hasAttendanceToday(user.id);

        if (present) {
            boolean inside = db.isClockedIn(user.id);

            holder.status.setText(inside ? "Présent" : "Sorti");
            holder.status.setBackgroundResource(inside ? R.drawable.chip_approved : R.drawable.chip_rejected);
            holder.status.setTextColor(holder.itemView.getContext().getColor(
                    inside ? R.color.success : R.color.danger));

            com.example.gestionpointage.model.AttendanceRecord today = db.getTodayAttendance(user.id);
            holder.arrival.setText(today != null && today.arrivalTime != null
                    ? "Arrivée " + DatabaseHelper.shortTime(today.arrivalTime)
                    : "Pointé aujourd'hui");
        } else {
            holder.status.setText("Absent");
            holder.status.setBackgroundResource(R.drawable.chip_pending);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.warning));
            holder.arrival.setText("Non pointé aujourd'hui");
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView avatar, name, job, status, arrival;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.empAvatar);
            name = itemView.findViewById(R.id.empName);
            job = itemView.findViewById(R.id.empJob);
            status = itemView.findViewById(R.id.empStatus);
            arrival = itemView.findViewById(R.id.empArrival);
        }
    }
}
