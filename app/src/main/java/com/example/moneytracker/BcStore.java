package com.example.moneytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BcStore {

    // One BC scheme
    public static class BcScheme {
        public String name;
        public int months;
        public String startDate;              // dd/MM/yyyy
        public List<String> scheduleDates = new ArrayList<>();

        // Installment info
        // "FIXED" -> use fixedAmount for all months
        // "RANDOM" -> use monthlyAmounts[i] for month i
        // "NONE" -> no amount info
        public String installmentType = "NONE";
        public int fixedAmount = 0;
        public List<Integer> monthlyAmounts = new ArrayList<>();
    }

    private static final String PREFS_NAME = "MoneyTrackerPrefs";
    private static final String BC_KEY = "bc_data_json";

    // key = account name or "_GLOBAL_"
    private static final HashMap<String, ArrayList<BcScheme>> bcMap = new HashMap<>();

    // Get full map (read‑only reference)
    public static HashMap<String, ArrayList<BcScheme>> getBcMap() {
        return bcMap;
    }

    // Add a scheme under an account key
    public static void addScheme(String key, BcScheme scheme) {
        ArrayList<BcScheme> list = bcMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
            bcMap.put(key, list);
        }
        list.add(scheme);
    }

    // Save all BC schemes to SharedPreferences (call after Add / Delete)
    public static void save(Context context) {
        JSONObject root = new JSONObject();
        try {
            for (String key : bcMap.keySet()) {
                JSONArray arr = new JSONArray();
                ArrayList<BcScheme> list = bcMap.get(key);
                if (list == null) continue;
                for (BcScheme s : list) {
                    JSONObject o = new JSONObject();
                    o.put("name", s.name);
                    o.put("months", s.months);
                    o.put("startDate", s.startDate);

                    // schedule dates
                    JSONArray dates = new JSONArray();
                    for (String d : s.scheduleDates) {
                        dates.put(d);
                    }
                    o.put("schedule", dates);

                    // installment fields
                    o.put("installmentType", s.installmentType);
                    o.put("fixedAmount", s.fixedAmount);
                    JSONArray amts = new JSONArray();
                    for (int a : s.monthlyAmounts) {
                        amts.put(a);
                    }
                    o.put("monthlyAmounts", amts);

                    arr.put(o);
                }
                root.put(key, arr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(BC_KEY, root.toString()).apply();
    }

    // Load BC schemes from SharedPreferences (call once in onCreateView of fragments)
    public static void load(Context context) {
        bcMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(BC_KEY, "");
        if (TextUtils.isEmpty(json)) return;

        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = root.getJSONArray(key);
                ArrayList<BcScheme> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    BcScheme s = new BcScheme();
                    s.name = o.optString("name");
                    s.months = o.optInt("months");
                    s.startDate = o.optString("startDate");

                    // schedule dates
                    s.scheduleDates = new ArrayList<>();
                    JSONArray dates = o.optJSONArray("schedule");
                    if (dates != null) {
                        for (int j = 0; j < dates.length(); j++) {
                            s.scheduleDates.add(dates.getString(j));
                        }
                    }

                    // installment fields
                    s.installmentType = o.optString("installmentType", "NONE");
                    s.fixedAmount = o.optInt("fixedAmount", 0);
                    s.monthlyAmounts = new ArrayList<>();
                    JSONArray amts = o.optJSONArray("monthlyAmounts");
                    if (amts != null) {
                        for (int j = 0; j < amts.length(); j++) {
                            s.monthlyAmounts.add(amts.getInt(j));
                        }
                    }

                    list.add(s);
                }
                bcMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
