package com.example.gestionpointage.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model d'une fete legale renvoyee par l'API externe :
 * https://date.nager.at/api/v3/PublicHolidays/{annee}/{pays}
 */
public class Holiday {

    @SerializedName("date")
    public String date;          // yyyy-MM-dd

    @SerializedName("localName")
    public String localName;     // nom en francais / local

    @SerializedName("name")
    public String englishName;

    @SerializedName("countryCode")
    public String countryCode;

    @SerializedName("global")
    public boolean global;

    public Holiday() {
    }

    public static class Holder {
        public List<Holiday> items;
    }
}
