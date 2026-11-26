package com.example.gestionpointage.api;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Recupere la liste des jours feries depuis l'API externe.
 * Les resultats sont caches en memoire : un seul appel par annee/pays.
 */
public class HolidayService {

    public interface Listener {
        void onResult(List<Holiday> holidays);

        void onError(String message);
    }

    private static final Map<String, List<Holiday>> CACHE = new HashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private HolidayService() {
    }

    /** Pays par defaut : Tunisie (TN) */
    public static void fetch(Listener listener) {
        fetch(Calendar.getInstance().get(Calendar.YEAR), "TN", listener);
    }

    public static void fetch(final int year, final String country, final Listener listener) {

        final String key = year + "_" + country;

        if (CACHE.containsKey(key)) {
            listener.onResult(CACHE.get(key));
            return;
        }

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Call<List<Holiday>> call = ApiClient.holidayApi().getHolidays(year, country);
                    Response<List<Holiday>> response = call.execute();

                    final List<Holiday> result = response.isSuccessful() && response.body() != null
                            ? response.body()
                            : new ArrayList<Holiday>();

                    CACHE.put(key, result);

                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(result);
                        }
                    });

                } catch (final Exception e) {
                    MAIN.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(e.getMessage() == null
                                    ? "Erreur réseau"
                                    : e.getMessage());
                        }
                    });
                }
            }
        });
    }

    /** Retourne le nom du jour ferie pour une date, ou null */
    public static Holiday find(List<Holiday> holidays, String date) {
        if (holidays == null) return null;
        for (Holiday h : holidays) {
            if (h.date != null && h.date.equals(date)) return h;
        }
        return null;
    }
}
