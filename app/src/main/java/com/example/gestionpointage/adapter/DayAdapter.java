package com.example.gestionpointage.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionpointage.R;
import com.example.gestionpointage.api.Holiday;
import com.example.gestionpointage.util.Ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Grille du calendrier (7 colonnes).
 * Gère :
 *  - le premier jour du mois aligné sur le lundi
 *  - la sélection début / fin de congé
 *  - les jours fériés récupérés depuis l'API externe
 *  - le jour courant
 */
public class DayAdapter extends RecyclerView.Adapter<DayAdapter.Holder> {

    public interface OnDayClick {
        void onDayClick(String isoDate);
    }

    private static final class Cell {
        final boolean empty;
        final int day;
        final String iso;

        Cell(boolean empty, int day, String iso) {
            this.empty = empty;
            this.day = day;
            this.iso = iso;
        }
    }

    private final List<Cell> cells = new ArrayList<>();
    private final Map<String, Holiday> holidays = new HashMap<>();

    private int year;
    private int month;              // 0 = janvier
    private String startIso;        // début de la sélection
    private String endIso;          // fin de la sélection
    private OnDayClick listener;

    public DayAdapter(int year, int month) {
        rebuild(year, month);
    }

    public void setOnDayClick(OnDayClick listener) {
        this.listener = listener;
    }

    public void setHolidays(List<Holiday> list) {
        holidays.clear();
        if (list != null) {
            for (Holiday h : list) {
                if (h.date != null) holidays.put(h.date, h);
            }
        }
        notifyDataSetChanged();
    }

    public void setSelection(String startIso, String endIso) {
        this.startIso = startIso;
        this.endIso = endIso;
        notifyDataSetChanged();
    }

    public void rebuild(int year, int month) {
        this.year = year;
        this.month = month;
        cells.clear();

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstWeekday = calendar.get(Calendar.DAY_OF_WEEK);
        // Lundi = 2, Dimanche = 1  ->  decalage
        int offset = firstWeekday == Calendar.SUNDAY ? 6 : firstWeekday - 2;

        for (int i = 0; i < offset; i++) {
            cells.add(new Cell(true, -1, null));
        }

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            String iso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
            cells.add(new Cell(false, day, iso));
        }

        while (cells.size() % 7 != 0) {
            cells.add(new Cell(true, -1, null));
        }

        notifyDataSetChanged();
    }

    public String getTitle() {
        String[] names = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        return names[month] + " " + year;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Cell cell = cells.get(position);

        if (cell.empty) {
            holder.label.setVisibility(View.INVISIBLE);
            holder.label.setText("");
            holder.label.setOnClickListener(null);
            return;
        }

        holder.label.setVisibility(View.VISIBLE);
        holder.label.setText(String.valueOf(cell.day));

        String todayIso = Ui.nowIso();

        // Priorité : début/fin > plage > jour férié > aujourd'hui > normal
        boolean isEdge = cell.iso.equals(startIso) || cell.iso.equals(endIso);
        boolean inRange = startIso != null && endIso != null
                && cell.iso.compareTo(startIso) > 0
                && cell.iso.compareTo(endIso) < 0;
        boolean isHoliday = holidays.containsKey(cell.iso);
        boolean isToday = cell.iso.equals(todayIso);

        if (isEdge) {
            holder.label.setBackgroundResource(R.drawable.day_selected);
            holder.label.setTextColor(holder.itemView.getContext().getColor(R.color.white));
        } else if (inRange) {
            holder.label.setBackgroundResource(R.drawable.day_range);
            holder.label.setTextColor(holder.itemView.getContext().getColor(R.color.cal_range_text));
        } else if (isHoliday) {
            holder.label.setBackgroundResource(R.drawable.day_holiday);
            holder.label.setTextColor(holder.itemView.getContext().getColor(R.color.warning));
        } else if (isToday) {
            holder.label.setBackgroundResource(R.drawable.day_today);
            holder.label.setTextColor(holder.itemView.getContext().getColor(R.color.primary));
        } else {
            holder.label.setBackgroundResource(R.drawable.day_normal);
            holder.label.setTextColor(holder.itemView.getContext().getColor(R.color.text_dark));
        }

        holder.label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDayClick(cell.iso);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.dayNumber);
        }
    }
}
