package com.example.gestionpointage.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Consommation d'une API externe (Nager.Date) : jours feries du pays.
 */
public interface HolidayApi {

    @GET("api/v3/PublicHolidays/{year}/{country}")
    Call<List<Holiday>> getHolidays(
            @Path("year") int year,
            @Path("country") String country
    );
}
