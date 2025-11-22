package com.example.moneytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class GivenFragment extends Fragment {

    private EditText nameInput, amountInput, noteInput;
    private Button addButton;
    private LinearLayout layoutBalanceList;

    public static class Entry implements EntryBase {
        public int amount;
        public String note;
        public String date;

        public Entry(int amount, String note, String date) {
            this.amount = amount;
            this.note = note;
            this.date = date;
        }
        @Override public int getAmount() { return amount; }
        @Override public String getNote() { return note; }
        @Override public String getDate() { return date; }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("amount", amount);
            obj.put("note", note);
            obj.put("date", date);
            return obj;
        }
        public static Entry fromJSON(JSONObject obj) throws JSONException {
            return new Entry(obj.getInt("amount"), obj.optString("note"), obj.optString("date"));
        }
    }

    public static HashMap<String, ArrayList<Entry>> givenMap = new HashMap<>();

    private static final String PREFS_NAME = "MoneyTrackerPrefs";
    private static final String GIVEN_KEY = "given_data";

    public static void saveMap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject();
        try {
            for (String name : givenMap.keySet()) {
                JSONArray jsonArray = new JSONArray();
                for (Entry e : givenMap.get(name)) {
                    jsonArray.put(e.toJSON());
                }
                root.put(name, jsonArray);
            }
            prefs.edit().putString(GIVEN_KEY, root.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void loadMap(Context context) {
        givenMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(GIVEN_KEY, "");
        if (!TextUtils.isEmpty(data)) {
            try {
                JSONObject root = new JSONObject(data);
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String name = keys.next();
                    JSONArray arr = root.getJSONArray(name);
                    ArrayList<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        entries.add(Entry.fromJSON(arr.getJSONObject(i)));
                    }
                    givenMap.put(name, entries);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        loadMap(requireContext());
        View v = inflater.inflate(R.layout.fragment_given, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        noteInput = v.findViewById(R.id.editTextNote);
        addButton = v.findViewById(R.id.buttonAdd);

        layoutBalanceList = v.findViewById(R.id.layoutBalanceList);

        addButton.setBackgroundResource(R.drawable.orange_rounded_button);

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String noteStr = noteInput.getText().toString().trim();
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                    Entry entry = new Entry(amount, noteStr, dateStr);
                    if (!givenMap.containsKey(name)) {
                        givenMap.put(name, new ArrayList<>());
                    }
                    givenMap.get(name).add(entry);
                    saveMap(getContext());
                    Toast.makeText(getContext(), "Added " + name + ": ₹" + amount, Toast.LENGTH_SHORT).show();
                    nameInput.setText("");
                    amountInput.setText("");
                    noteInput.setText("");
                    notifySummaryUpdate();
                    updateBalanceList();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Enter both name and amount", Toast.LENGTH_SHORT).show();
            }
        });

        updateBalanceList();
        return v;
    }

    private void updateBalanceList() {
        if (layoutBalanceList == null) return;
        layoutBalanceList.removeAllViews();

        ArrayList<String> names = new ArrayList<>(givenMap.keySet());
        Collections.sort(names);

        for (String name : names) {
            int total = 0;
            for (Entry e : givenMap.get(name)) {
                total += e.getAmount();
            }

            // Pink label on left (total added)
            TextView totalLabel = new TextView(getContext());
            totalLabel.setText("₹" + total);
            totalLabel.setTextSize(14);
            totalLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            totalLabel.setPadding(18, 4, 18, 4);
            totalLabel.setBackgroundResource(R.drawable.balance_label_pink);
            totalLabel.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            totalParams.setMargins(0, 0, 12, 0);
            totalLabel.setLayoutParams(totalParams);

            // Name
            TextView nameTv = new TextView(getContext());
            nameTv.setText(name);
            nameTv.setTextSize(16);
            nameTv.setTextColor(0xFF252525);
            nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Green label on right (current balance)
            TextView balanceTv = new TextView(getContext());
            balanceTv.setText("₹" + total);
            balanceTv.setTextSize(14);
            balanceTv.setTypeface(null, android.graphics.Typeface.BOLD);
            balanceTv.setPadding(18, 4, 18, 4);
            balanceTv.setBackgroundResource(R.drawable.balance_label_green);
            balanceTv.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            balanceParams.setMargins(12, 0, 0, 0);
            balanceTv.setLayoutParams(balanceParams);

            // Row: [PinkLabel]  Name  [GreenLabel]
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            row.addView(totalLabel);
            row.addView(nameTv);
            row.addView(balanceTv);

            layoutBalanceList.addView(row);
        }
    }

    public static void deleteAccount(Context context, String name) {
        givenMap.remove(name);
        saveMap(context);
    }

    private void notifySummaryUpdate() {
        if (getActivity() != null) {
            SummaryFragment fragment = (SummaryFragment) getActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag("f2");
            if (fragment != null) {
                fragment.refreshView();
            }
        }
    }
}
