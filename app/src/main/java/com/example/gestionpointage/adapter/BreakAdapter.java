package com.example.gestionpointage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.R;
import com.example.gestionpointage.database.DatabaseHelper;
import com.example.gestionpointage.model.BreakRecord;

import java.util.List;

/**
 * Liste des pauses (pointage du jour ou pauses de tous les employés).
 */
public class BreakAdapter extends RecyclerView.Adapter<BreakAdapter.Holder> {

    public interface OnBreakListener {
        void onBreakClick(BreakRecord record);
    }

    private List<BreakRecord> items;
    private final boolean showUserName;
    private OnBreakListener listener;

    public BreakAdapter(List<BreakRecord> items, boolean showUserName) {
        this.items = items;
        this.showUserName = showUserName;
    }

    public void setOnBreakListener(OnBreakListener listener) {
        this.listener = listener;
    }

    public void update(List<BreakRecord> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_break, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BreakRecord record = items.get(position);

        holder.emoji.setText(record.getEmoji());
        holder.type.setText("Pause " + record.type.toLowerCase());

        String hours = DatabaseHelper.shortTime(record.startTime)
                + " → " + (record.isOpen() ? "en cours" : DatabaseHelper.shortTime(record.endTime));

        if (showUserName) {
            String name = new com.example.gestionpointage.database.DatabaseHelper(holder.itemView.getContext())
                    .userNameOfBreak(record);
            holder.hours.setText(name + "  •  " + hours);
        } else {
            holder.hours.setText(hours);
        }

        if (record.isOpen()) {
            holder.duration.setText(record.durationMinutes() + " min");
            holder.status.setText("En cours");
            holder.status.setBackgroundResource(R.drawable.chip_pending);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.warning));
            holder.emoji.setBackgroundResource(R.drawable.circle_blue);
        } else {
            holder.duration.setText(record.durationMinutes() + " min");
            holder.status.setText("Terminée");
            holder.status.setBackgroundResource(R.drawable.chip_approved);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.success));
            holder.emoji.setBackgroundResource(R.drawable.circle_blue);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onBreakClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView emoji, type, hours, duration, status;

        Holder(@NonNull View itemView) {
            super(itemView);
            emoji = itemView.findViewById(R.id.breakEmoji);
            type = itemView.findViewById(R.id.breakType);
            hours = itemView.findViewById(R.id.breakHours);
            duration = itemView.findViewById(R.id.breakDuration);
            status = itemView.findViewById(R.id.breakStatus);
        }
    }
}
