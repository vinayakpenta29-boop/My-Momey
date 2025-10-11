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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class ReceivedFragment extends Fragment {

    private EditText nameInput, amountInput, noteInput;
    private Button addButton;

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

    public static HashMap<String, ArrayList<Entry>> receivedMap = new HashMap<>();

    private static final String PREFS_NAME = "MoneyTrackerPrefs";
    private static final String RECEIVED_KEY = "received_data";

    public static void saveMap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject root = new JSONObject();
        try {
            for (String name : receivedMap.keySet()) {
                JSONArray jsonArray = new JSONArray();
                for (Entry e : receivedMap.get(name)) {
                    jsonArray.put(e.toJSON());
                }
                root.put(name, jsonArray);
            }
            prefs.edit().putString(RECEIVED_KEY, root.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void loadMap(Context context) {
        receivedMap.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(RECEIVED_KEY, "");
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
                    receivedMap.put(name, entries);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        loadMap(requireContext());
        View v = inflater.inflate(R.layout.fragment_received, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        noteInput = v.findViewById(R.id.editTextNote);
        addButton = v.findViewById(R.id.buttonAdd);

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
                    if (!receivedMap.containsKey(name)) {
                        receivedMap.put(name, new ArrayList<>());
                    }
                    receivedMap.get(name).add(entry);
                    saveMap(getContext());
                    Toast.makeText(getContext(), "Added " + name + ": ₹" + amount, Toast.LENGTH_SHORT).show();
                    nameInput.setText("");
                    amountInput.setText("");
                    noteInput.setText("");
                    notifySummaryUpdate();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Enter both name and amount", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    public static void deleteAccount(Context context, String name) {
        receivedMap.remove(name);
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
