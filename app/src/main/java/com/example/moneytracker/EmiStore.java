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

public class EmiStore {

    // One EMI scheme
    public static class EmiScheme {
        public String name;
        public int months;
        public String startDate;              // dd/MM/yyyy
        public List<String> scheduleDates = new ArrayList<>();

        // Unique id used by "Belongs to EMI" spinner: key + "|" + name
        public String id = "";

        // How many installments are paid (tick first paidCount boxes)
        public int paidCount = 0;

        // Installment info
        // "FIXED" -> use fixedAmount for all months
        // "RANDOM" -> use monthlyAmounts[i] for month i
        // "NONE" -> no amount info
        public String installmentType = "NONE";
        public int fixedAmount = 0;
        public List<Integer> monthlyAmounts = new ArrayList<>();
    }

    private static final String PREFS_NAME = "MoneyTrackerPrefs";
    private static final String EMI_KEY = "emi_data_json";

    // key = account name or "_GLOBAL_"
    private static final HashMap<String, ArrayList<EmiScheme>> emiMap = new HashMap<>();

    // Get full map (read‑only reference)
    public static HashMap<String, ArrayList<EmiScheme>> getEmiMap() {
        return emiMap;
    }

    // Add a scheme under an account key
    public static void addScheme(String key, EmiScheme scheme) {
        ArrayList<EmiScheme> list = emiMap.get(key);
        if (list == null) {
            list = new ArrayList<>();
            emiMap.put(key, list);
        }
        // build stable id if not set
        if (TextUtils.isEmpty(scheme.id)) {
            scheme.id = key + "|" + scheme.name;
        }
        list.add(scheme);
    }

    // Helper: mark one installment as done for given scheme id
    // Just increase paidCount, so checkboxes tick in order.
    public static void markEmiInstallmentDone(String emiId, String unusedDate) {
        if (TextUtils.isEmpty(emiId)) return;
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (EmiScheme s : list) {
                if (emiId.equals(s.id)) {
                    if (s.paidCount < s.months) {
                        s.paidCount++;
                    }
                    return;
                }
            }
        }
    }

    // Save all EMI schemes to SharedPreferences
    public static void save(Context context) {
        JSONObject root = new JSONObject();
        try {
            for (String key : emiMap.keySet()) {
                JSONArray arr = new JSONArray();
                ArrayList<EmiScheme> list = emiMap.get(key);
                if (list == null) continue;
                for (EmiScheme s : list) {
                    JSONObject o = new JSONObject();
                    o.put("name", s.name);
                    o.put("months", s.months);
                    o.put("startDate", s.startDate);

                    // id
                    if (TextUtils.isEmpty(s.id)) {
                        s.id = key + "|" + s.name;
                    }
                    o.put("id", s.id);

                    // schedule dates
                    JSONArray dates = new JSONArray();
                    for (String d : s.scheduleDates) {
                        dates.put(d);
                    }
                    o.put("schedule", dates);

                    // paid count
                    o.put("paidCount", s.paidCount);

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

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(EMI_KEY, root.toString()).apply();
    }

    // Load EMI schemes from SharedPreferences
    public static void load(Context context) {
        emiMap.clear();
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(EMI_KEY, "");
        if (TextUtils.isEmpty(json)) return;

        try {
            JSONObject root = new JSONObject(json);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = root.getJSONArray(key);
                ArrayList<EmiScheme> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    EmiScheme s = new EmiScheme();
                    s.name = o.optString("name");
                    s.months = o.optInt("months");
                    s.startDate = o.optString("startDate");
                    s.id = o.optString("id", key + "|" + s.name);

                    // schedule dates
                    s.scheduleDates = new ArrayList<>();
                    JSONArray dates = o.optJSONArray("schedule");
                    if (dates != null) {
                        for (int j = 0; j < dates.length(); j++) {
                            s.scheduleDates.add(dates.getString(j));
                        }
                    }

                    // paid count
                    s.paidCount = o.optInt("paidCount", 0);

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
                emiMap.put(key, list);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
