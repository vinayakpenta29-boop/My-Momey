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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private RadioGroup categoryGroup;

    public static class Entry implements EntryBase {
        public int amount;
        public String note;
        public String date;
        public String category;   // "", "Interest", "EMI", "BC"

        public Entry(int amount, String note, String date, String category) {
            this.amount = amount;
            this.note = note;
            this.date = date;
            this.category = category;
        }

        @Override public int getAmount() { return amount; }
        @Override public String getNote() { return note; }
        @Override public String getDate() { return date; }
        public String getCategory() { return category; }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("amount", amount);
            obj.put("note", note);
            obj.put("date", date);
            obj.put("category", category);   // may be empty string
            return obj;
        }
        public static Entry fromJSON(JSONObject obj) throws JSONException {
            return new Entry(
                obj.getInt("amount"),
                obj.optString("note"),
                obj.optString("date"),
                obj.has("category") ? obj.getString("category") : ""   // old data = normal
            );
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
        categoryGroup = v.findViewById(R.id.radioGroupCategory);

        addButton.setBackgroundResource(R.drawable.orange_rounded_button);

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String noteStr = noteInput.getText().toString().trim();

            // No default; empty string means normal entry
            int checkedId = categoryGroup.getCheckedRadioButtonId();
            String category = "";
            if (checkedId != -1) {
                RadioButton selected = v.findViewById(checkedId);
                category = selected.getText().toString();  // "Interest", "EMI", "BC"
            }

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                    Entry entry = new Entry(amount, noteStr, dateStr, category);
                    if (!givenMap.containsKey(name)) {
                        givenMap.put(name, new ArrayList<>());
                    }
                    givenMap.get(name).add(entry);
                    saveMap(getContext());
                    Toast.makeText(getContext(), "Added " + name + ": ₹" + amount, Toast.LENGTH_SHORT).show();
                    nameInput.setText("");
                    amountInput.setText("");
                    noteInput.setText("");
                    // do not change radio selection; user chooses each time
                    notifySummaryUpdate();
                    updateBalanceList();

                    try {
                        ReceivedFragment receivedFragment = (ReceivedFragment) getActivity()
                            .getSupportFragmentManager()
                            .findFragmentByTag("f1");
                        if (receivedFragment != null) {
                            receivedFragment.updateBalanceList();
                        }
                    } catch (Exception ignored) {}
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

    public void updateBalanceList() {
        if (layoutBalanceList == null) return;
        layoutBalanceList.removeAllViews();

        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;
        ArrayList<String> names = new ArrayList<>(givenMap.keySet());
        Collections.sort(names);

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);

            int totalGave = 0;
            int totalReceived = 0;

            // GIVEN side: only normal entries (category == "")
            for (GivenFragment.Entry e : givenMap.get(name)) {
                if (TextUtils.isEmpty(e.category)) {
                    totalGave += e.getAmount();
                }
            }

            // RECEIVED side: only normal entries
            if (receivedMap != null && receivedMap.containsKey(name)) {
                for (ReceivedFragment.Entry e : receivedMap.get(name)) {
                    if (TextUtils.isEmpty(e.category)) {
                        totalReceived += e.getAmount();
                    }
                }
            }

            int netBalance = totalGave - totalReceived;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Name
            TextView nameTv = new TextView(getContext());
            nameTv.setText(name);
            nameTv.setTextSize(16);
            nameTv.setTextColor(0xFF000000);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(nameTv);

            // Green (total gave)
            TextView greenLabel = new TextView(getContext());
            greenLabel.setText("₹" + totalGave);
            greenLabel.setTextSize(14);
            greenLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            greenLabel.setPadding(16, 2, 16, 2);
            greenLabel.setBackgroundResource(R.drawable.balance_label_green);
            greenLabel.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams greenParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            greenParams.setMargins(12, 0, 0, 0);
            greenLabel.setLayoutParams(greenParams);
            row.addView(greenLabel);

            // Spacer
            View spacer = new View(getContext());
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1f);
            spacer.setLayoutParams(spacerParams);
            row.addView(spacer);

            // "Balance :" label
            TextView balanceText = new TextView(getContext());
            balanceText.setText("Balance : ");
            balanceText.setTextSize(14);
            balanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
            balanceText.setTextColor(0xFFB0B0B0);
            row.addView(balanceText);

            // Pink (net balance)
            TextView balanceLabel = new TextView(getContext());
            balanceLabel.setText("₹" + netBalance);
            balanceLabel.setTextSize(14);
            balanceLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            balanceLabel.setPadding(16, 2, 16, 2);
            balanceLabel.setBackgroundResource(R.drawable.balance_label_pink);
            balanceLabel.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams pinkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pinkParams.setMargins(8, 0, 0, 0);
            balanceLabel.setLayoutParams(pinkParams);
            row.addView(balanceLabel);

            layoutBalanceList.addView(row);

            if (i != names.size() - 1) {
                addDividerWithMargin(layoutBalanceList, 1);
            }
        }
    }

    private void addDividerWithMargin(LinearLayout layout, int thicknessDp) {
        View line = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(thicknessDp));
        int pxMargin = dpToPx(4);
        params.setMargins(pxMargin, 0, pxMargin, 0);
        line.setLayoutParams(params);
        line.setBackgroundColor(0xFFD1D1D1);
        layout.addView(line);
    }

    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
