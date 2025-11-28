package com.example.moneytracker;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.app.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;     // STEP 3: for scrollable Interest list
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReceivedFragment extends Fragment {

    private EditText nameInput, amountInput, noteInput;
    private Button addButton;
    private RadioGroup categoryGroup;
    private LinearLayout layoutBalanceList;
    private ImageButton btnMoreTopReceived; // top-right BC menu

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
            obj.put("category", category);
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

    public static HashMap<String, ArrayList<Entry>> receivedMap = new HashMap<>();

    private static final String PREFS_NAME = "MoneyTrackerPrefs";
    private static final String RECEIVED_KEY = "received_data";

    private static class Bcscheme {
        String name;
        int months;
        String startDate;
        List<String> scheduleDates = new ArrayList<>();
    }

    private static final HashMap<String, ArrayList<BcScheme>> bcMap = new HashMap<>();

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
        layoutBalanceList = v.findViewById(R.id.layoutBalanceList);
        categoryGroup = v.findViewById(R.id.radioGroupCategory);
        btnMoreTopReceived = v.findViewById(R.id.btnMoreTopReceived);

        addButton.setBackgroundResource(R.drawable.orange_rounded_button);

        // Top-right BC three-dots menu (placeholder for now)
        if (btnMoreTopReceived != null) {
            btnMoreTopReceived.setOnClickListener(this::showBcManagerMenu);
        }

        // Make RadioGroup togglable: tap again to unselect
        categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            private int lastCheckedId = -1;

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) return;
                if (checkedId == lastCheckedId) {
                    group.clearCheck();      // unselect when tapping same option
                    lastCheckedId = -1;
                } else {
                    lastCheckedId = checkedId;
                }
            }
        });

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String noteStr = noteInput.getText().toString().trim();

            // No default; empty = normal
            int checkedId = categoryGroup.getCheckedRadioButtonId();
            String category = "";
            if (checkedId != -1) {
                RadioButton selected = v.findViewById(checkedId);
                category = selected.getText().toString();   // "Interest", "EMI", "BC"
            }

            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                    Entry entry = new Entry(amount, noteStr, dateStr, category);
                    if (!receivedMap.containsKey(name)) {
                        receivedMap.put(name, new ArrayList<>());
                    }
                    receivedMap.get(name).add(entry);
                    saveMap(getContext());
                    Toast.makeText(getContext(), "Added " + name + ": ₹" + amount, Toast.LENGTH_SHORT).show();
                    nameInput.setText("");
                    amountInput.setText("");
                    noteInput.setText("");
                    categoryGroup.clearCheck();
                    notifySummaryUpdate();
                    updateBalanceList();

                    try {
                        GivenFragment givenFragment = (GivenFragment) getActivity()
                            .getSupportFragmentManager()
                            .findFragmentByTag("f0");
                        if (givenFragment != null) {
                            givenFragment.updateBalanceList();
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

    // Placeholder BC menu
    private void showBcMenu(View anchor) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add("Add BC");
        menu.getMenu().add("view BC List");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Add BC".equals(title)) {
                showAddBcDialog();
            } else if ("View BC List".equals(title)) {
                showBcListDialog();
            }
            return true;
        });
        menu.show();
    }

    // STEP 4: dialog to create a new BC scheme
    private void showAddBcDialog() {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        root.setPadding(pad, pad, pad, pad);

        EditText etAccount = new EditText(getContext());
        etAccount.setHint("Account Name (optional)");
        root.addView(etAccount);

        EditText etBcName = new EditText(getContext());
        etBcName.setHint("BC Name");
        root.addView(etBcName);

        EditText etMonths = new EditText(getContext());
        etMonths.setHint("Months");
        etMonths.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(etMonths);

        EditText etStartDate = new EditText(getContext());
        etStartDate.setHint("Start / Due Date (dd/MM/yyyy)");
        etStartDate.setFocusable(false);
        etStartDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    getContext(),
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        String d = String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etStartDate.setText(d);
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });
        root.addView(etStartDate);

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Add BC Scheme")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String accountName = etAccount.getText().toString().trim();
                    String bcName = etBcName.getText().toString().trim();
                    String monthsStr = etMonths.getText().toString().trim();
                    String startDate = etStartDate.getText().toString().trim();

                    if (TextUtils.isEmpty(bcName) ||
                            TextUtils.isEmpty(monthsStr) ||
                            TextUtils.isEmpty(startDate)) {
                        Toast.makeText(getContext(),
                                "BC Name, Months and Start Date are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int months;
                    try {
                        months = Integer.parseInt(monthsStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid months", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BcScheme scheme = new BcScheme();
                    scheme.name = bcName;
                    scheme.months = months;
                    scheme.startDate = startDate;
                    generateBcSchedule(scheme);  // fill scheduleDates

                    String key = TextUtils.isEmpty(accountName) ? "_GLOBAL_" : accountName;
                    ArrayList<BcScheme> list = bcMap.get(key);
                    if (list == null) {
                        list = new ArrayList<>();
                        bcMap.put(key, list);
                    }
                    list.add(scheme);

                    Toast.makeText(getContext(), "BC added", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // STEP 4: create monthly schedule dates for a BC scheme
    private void generateBcSchedule(BcScheme scheme) {
        scheme.scheduleDates.clear();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(df.parse(scheme.startDate));
        } catch (ParseException e) {
            return;
        }
        for (int i = 0; i < scheme.months; i++) {
            scheme.scheduleDates.add(df.format(c.getTime()));
            c.add(Calendar.MONTH, 1);
        }
    }

    // STEP 4: list all BC schemes and open details
    private void showBcListDialog() {
        if (bcMap.isEmpty()) {
            Toast.makeText(getContext(), "No BC schemes added", Toast.LENGTH_SHORT).show();
            return;
        }

        // Flatten map into labels and references
        List<String> labels = new ArrayList<>();
        List<BcScheme> schemes = new ArrayList<>();
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (BcScheme s : list) {
                String label = (key.equals("_GLOBAL_") ? "" : key + " - ") + s.name;
                labels.add(label);
                schemes.add(s);
            }
        }
        if (labels.isEmpty()) {
            Toast.makeText(getContext(), "No BC schemes added", Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("BC Schemes")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    BcScheme s = schemes.get(which);
                    showBcDetailsDialog(s);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // STEP 4: show one BC scheme with its schedule dates (checkbox-like list, not persisted)
    private void showBcDetailsDialog(BcScheme scheme) {
        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        scrollView.addView(container);

        for (String date : scheme.scheduleDates) {
            TextView tv = new TextView(getContext());
            tv.setText("□ " + date);  // visual checkbox (no state persistence yet)
            tv.setTextSize(14);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            container.addView(tv);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("BC: " + scheme.name)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }


    public void updateBalanceList() {
        if (layoutBalanceList == null) return;
        layoutBalanceList.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> givenMap = GivenFragment.givenMap;
        ArrayList<String> names = new ArrayList<>(receivedMap.keySet());
        Collections.sort(names);

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);

            int totalReceived = 0;
            int totalGave = 0;

            // RECEIVED side: only normal entries (category == "")
            for (ReceivedFragment.Entry e : receivedMap.get(name)) {
                if (TextUtils.isEmpty(e.category)) {
                    totalReceived += e.getAmount();
                }
            }

            // GIVEN side: only normal entries
            if (givenMap != null && givenMap.containsKey(name)) {
                for (GivenFragment.Entry e : givenMap.get(name)) {
                    if (TextUtils.isEmpty(e.category)) {
                        totalGave += e.getAmount();
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
            nameTv.setTextColor(0xFF252525);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(nameTv);

            // Green box (total received)
            TextView greenLabel = new TextView(getContext());
            greenLabel.setText("₹" + totalReceived);
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

            // Spacer to push balance section to right
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

            // Pink box (net balance)
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

            // Per-person three-dots menu
            ImageView morePerson = new ImageView(getContext());
            LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                    dpToPx(24), dpToPx(24));
            moreParams.setMargins(dpToPx(8), 0, 0, 0);
            morePerson.setLayoutParams(moreParams);
            morePerson.setImageResource(R.drawable.ic_more_vert);
            row.addView(morePerson);

            morePerson.setOnClickListener(v -> showPersonMenu(v, name));

            layoutBalanceList.addView(row);

            if (i != names.size() - 1) {
                addDividerWithMargin(layoutBalanceList, 1);
            }
        }
    }

    // Per-person menu: Delete + Notes (Interest)
    private void showPersonMenu(View anchor, String name) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add("Delete Entry");
        menu.getMenu().add("Notes (Interest)");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Delete Entry".equals(title)) {
                showDeleteEntriesDialog(name);
            } else if ("Notes (Interest)".equals(title)) {
                showInterestNotesDialog(name);   // STEP 3: open Interest notes
            }
            return true;
        });
        menu.show();
    }

    private void showDeleteEntriesDialog(String name) {
        ArrayList<Entry> list = receivedMap.get(name); // fix: use receivedMap here
        if (list == null || list.isEmpty()) {
            Toast.makeText(getContext(), "No entries to delete for " + name, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[list.size()];
        boolean[] checked = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("₹").append(e.amount);
            if (!TextUtils.isEmpty(e.note)) sb.append("  ").append(e.note);
            if (!TextUtils.isEmpty(e.date)) sb.append("  ").append(e.date);
            labels[i] = sb.toString();
            checked[i] = false;
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete entries for " + name)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean anySelected = false;
                    for (boolean b : checked) {
                        if (b) { anySelected = true; break; }
                    }
                    if (!anySelected) {
                        Toast.makeText(getContext(), "No entries selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (int i = list.size() - 1; i >= 0; i--) {
                        if (checked[i]) list.remove(i);
                    }
                    if (list.isEmpty()) {
                        receivedMap.remove(name);
                    }
                    saveMap(getContext());
                    notifySummaryUpdate();
                    updateBalanceList();

                    try {
                        GivenFragment givenFragment = (GivenFragment) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentByTag("f0");
                        if (givenFragment != null) {
                            givenFragment.updateBalanceList();
                        }
                    } catch (Exception ignored) {}

                    Toast.makeText(getContext(), "Deleted selected entries for " + name,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // STEP 3: show only Interest entries for this person (Received side)
    private void showInterestNotesDialog(String name) {
        ArrayList<Entry> list = receivedMap.get(name);
        if (list == null || list.isEmpty()) {
            Toast.makeText(getContext(), "No entries for " + name, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Entry> interestEntries = new ArrayList<>();
        for (Entry e : list) {
            if ("Interest (%)".equalsIgnoreCase(e.category) ||
                "Interest".equalsIgnoreCase(e.category)) {
                interestEntries.add(e);
            }
        }

        if (interestEntries.isEmpty()) {
            Toast.makeText(getContext(), "No Interest entries for " + name, Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        scrollView.addView(container);

        for (Entry e : interestEntries) {
            TextView tv = new TextView(getContext());
            StringBuilder sb = new StringBuilder();
            sb.append("₹").append(e.amount);
            if (!TextUtils.isEmpty(e.note)) sb.append("  • ").append(e.note);
            if (!TextUtils.isEmpty(e.date)) sb.append("  • ").append(e.date);
            tv.setText(sb.toString());
            tv.setTextSize(14);
            tv.setTextColor(0xFF1976D2);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            container.addView(tv);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Interest (Vyaj) – " + name)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
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
