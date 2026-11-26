package com.example.gestionpointage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.R;
import com.example.gestionpointage.model.AttendanceRecord;
import com.example.gestionpointage.model.BreakRecord;
import com.example.gestionpointage.util.Ui;

import java.util.List;

/**
 * Historique des journees de pointage (avec les pauses de chaque journee).
 */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.Holder> {

    private List<AttendanceRecord> items;

    public AttendanceAdapter(List<AttendanceRecord> items) {
        this.items = items;
    }

    public void update(List<AttendanceRecord> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AttendanceRecord record = items.get(position);

        holder.date.setText(Ui.isoToLong(record.date));

        // Travail effectif
        long worked = record.getWorkedMinutes();
        holder.worked.setText(worked >= 0
                ? Ui.minutesToLabel(worked) + " de travail"
                : "Journée en cours");

        // Statut
        if (record.isOpen()) {
            holder.status.setText("En cours");
            holder.status.setBackgroundResource(R.drawable.chip_pending);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.warning));
        } else {
            holder.status.setText("Terminé");
            holder.status.setBackgroundResource(R.drawable.chip_approved);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.success));
        }

        holder.arrival.setText("🌅 "
                + (record.arrivalTime == null ? "--:--" : DatabaseHelperTime(record.arrivalTime)));
        holder.exit.setText("🌙 "
                + (record.exitTime == null ? "en cours" : DatabaseHelperTime(record.exitTime)));

        // Pauses de la journée
        holder.breaksContainer.removeAllViews();

        if (record.breaks == null || record.breaks.isEmpty()) {
            holder.breaksContainer.setVisibility(View.GONE);
        } else {
            holder.breaksContainer.setVisibility(View.VISIBLE);

            TextView title = new TextView(holder.itemView.getContext());
            title.setText("Pauses (" + record.breaks.size() + ")");
            title.setTextColor(holder.itemView.getContext().getColor(R.color.text_medium));
            title.setTextSize(12f);
            title.setPadding(0, 0, 0, 8);
            holder.breaksContainer.addView(title);

            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            for (BreakRecord b : record.breaks) {
                View row = inflater.inflate(R.layout.item_break_mini,
                        holder.breaksContainer, false);

                TextView emoji = row.findViewById(R.id.miniEmoji);
                TextView type = row.findViewById(R.id.miniType);
                TextView hours = row.findViewById(R.id.miniHours);

                emoji.setText(b.getEmoji());
                type.setText(b.type);
                hours.setText(DatabaseHelperTime(b.startTime)
                        + " → " + (b.isOpen() ? "en cours" : DatabaseHelperTime(b.endTime))
                        + "  •  " + b.durationMinutes() + " min");

                holder.breaksContainer.addView(row);
            }
        }
    }

    private static String DatabaseHelperTime(String time) {
        return com.example.gestionpointage.database.DatabaseHelper.shortTime(time);
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView date, worked, status, arrival, exit;
        LinearLayout breaksContainer;

        Holder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.histDate);
            worked = itemView.findViewById(R.id.histWorked);
            status = itemView.findViewById(R.id.histStatus);
            arrival = itemView.findViewById(R.id.histArrival);
            exit = itemView.findViewById(R.id.histExit);
            breaksContainer = itemView.findViewById(R.id.histBreaksContainer);
        }
    }
}
