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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

// BcStore imports
import com.example.moneytracker.BcStore;
import com.example.moneytracker.BcStore.BcScheme;

public class GivenFragment extends Fragment {

    private EditText nameInput, amountInput, noteInput;
    private Button addButton;
    private LinearLayout layoutBalanceList;
    private RadioGroup categoryGroup;
    private ImageButton btnMoreTopGiven; // top-right BC menu

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
                obj.has("category") ? obj.getString("category") : ""
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
        BcStore.load(requireContext());   // load BC data

        View v = inflater.inflate(R.layout.fragment_given, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        noteInput = v.findViewById(R.id.editTextNote);
        addButton = v.findViewById(R.id.buttonAdd);
        layoutBalanceList = v.findViewById(R.id.layoutBalanceList);
        categoryGroup = v.findViewById(R.id.radioGroupCategory);
        btnMoreTopGiven = v.findViewById(R.id.btnMoreTopGiven);

        addButton.setBackgroundResource(R.drawable.orange_rounded_button);

        if (btnMoreTopGiven != null) {
            btnMoreTopGiven.setOnClickListener(this::showBcManagerMenu);
        }

        // Make RadioGroup togglable: tap again to unselect
        categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            private int lastCheckedId = -1;

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) return;
                if (checkedId == lastCheckedId) {
                    group.clearCheck();
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
                    categoryGroup.clearCheck();
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

    // BC Manager main menu (top-right three dots)
    private void showBcManagerMenu(View anchor) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add("Add BC");
        menu.getMenu().add("View BC List");
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

    // Dialog to create a new BC scheme + INSTALLMENT options
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

        // INSTALLMENT: label
        TextView instLabel = new TextView(getContext());
        instLabel.setText("Installment Option");
        instLabel.setPadding(0, dpToPx(8), 0, dpToPx(4));
        root.addView(instLabel);

        // INSTALLMENT: two buttons
        LinearLayout instLayout = new LinearLayout(getContext());
        instLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnFixed = new Button(getContext());
        btnFixed.setText("Fixed Amount Installment");
        instLayout.addView(btnFixed);

        Button btnRandom = new Button(getContext());
        btnRandom.setText("Random Amount Installment");
        instLayout.addView(btnRandom);

        root.addView(instLayout);

        // INSTALLMENT: holders for choice
        final String[] instType = new String[] { "NONE" };   // "FIXED" / "RANDOM" / "NONE"
        final int[] fixedAmountHolder = new int[] { 0 };
        final List<Integer> randomAmountsHolder = new ArrayList<>();

        // INSTALLMENT: Fixed amount dialog
        btnFixed.setOnClickListener(v2 -> {
            final EditText input = new EditText(getContext());
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Installment amount");

            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Fixed Amount Installment")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        String amtStr = input.getText().toString().trim();
                        try {
                            int val = Integer.parseInt(amtStr);
                            instType[0] = "FIXED";
                            fixedAmountHolder[0] = val;
                            randomAmountsHolder.clear();
                            Toast.makeText(getContext(),
                                    "Fixed installment set: ₹" + val,
                                    Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });

        // INSTALLMENT: Random amounts dialog
        btnRandom.setOnClickListener(v2 -> {
            String monthsStr = etMonths.getText().toString().trim();
            int m;
            try {
                m = Integer.parseInt(monthsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Enter Months first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (m <= 0) {
                Toast.makeText(getContext(), "Months must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            ScrollView scroll = new ScrollView(getContext());
            LinearLayout listLayout = new LinearLayout(getContext());
            listLayout.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(listLayout);

            EditText[] amountInputs = new EditText[m];
            for (int i = 0; i < m; i++) {
                EditText et = new EditText(getContext());
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setHint((i + 1) + " installment amount");
                listLayout.addView(et);
                amountInputs[i] = et;
            }

            new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Random Amount Installments")
                    .setView(scroll)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        List<Integer> list = new ArrayList<>();
                        for (int i = 0; i < m; i++) {
                            String s = amountInputs[i].getText().toString().trim();
                            try {
                                list.add(Integer.parseInt(s));
                            } catch (NumberFormatException e) {
                                Toast.makeText(getContext(),
                                        "Invalid amount at position " + (i + 1),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        instType[0] = "RANDOM";
                        fixedAmountHolder[0] = 0;
                        randomAmountsHolder.clear();
                        randomAmountsHolder.addAll(list);
                        Toast.makeText(getContext(),
                                "Random installments set",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

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

                    // INSTALLMENT: copy user choice into scheme
                    scheme.installmentType = instType[0];
                    if ("FIXED".equals(instType[0])) {
                        scheme.fixedAmount = fixedAmountHolder[0];
                        scheme.monthlyAmounts.clear();
                    } else if ("RANDOM".equals(instType[0])) {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                        scheme.monthlyAmounts.addAll(randomAmountsHolder);
                    } else {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                    }

                    String key = TextUtils.isEmpty(accountName) ? "_GLOBAL_" : accountName;
                    BcStore.addScheme(key, scheme);
                    BcStore.save(getContext());

                    Toast.makeText(getContext(), "BC added", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // Create monthly schedule dates for a BC scheme
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

    // List all BC schemes and open details
    private void showBcListDialog() {
        HashMap<String, ArrayList<BcScheme>> bcMap = BcStore.getBcMap();

        if (bcMap.isEmpty()) {
            Toast.makeText(getContext(), "No BC schemes added", Toast.LENGTH_SHORT).show();
            return;
        }

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

    // Show one BC scheme with its schedule dates and installment amounts
    private void showBcDetailsDialog(BcScheme scheme) {
        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        scrollView.addView(container);

        for (int i = 0; i < scheme.scheduleDates.size(); i++) {
            String date = scheme.scheduleDates.get(i);
            String amountText = "";

            if ("FIXED".equals(scheme.installmentType)) {
                amountText = "  ₹" + scheme.fixedAmount;
            } else if ("RANDOM".equals(scheme.installmentType)
                    && i < scheme.monthlyAmounts.size()) {
                amountText = "  ₹" + scheme.monthlyAmounts.get(i);
            }

            TextView tv = new TextView(getContext());
            tv.setText("□ " + date + amountText);
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

        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;
        ArrayList<String> names = new ArrayList<>(givenMap.keySet());
        Collections.sort(names);

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);

            int totalGave = 0;
            int totalReceived = 0;

            for (GivenFragment.Entry e : givenMap.get(name)) {
                if (TextUtils.isEmpty(e.category)) {
                    totalGave += e.getAmount();
                }
            }

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

            TextView nameTv = new TextView(getContext());
            nameTv.setText(name);
            nameTv.setTextSize(16);
            nameTv.setTextColor(0xFF000000);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(nameTv);

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

            View spacer = new View(getContext());
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1f);
            spacer.setLayoutParams(spacerParams);
            row.addView(spacer);

            TextView balanceText = new TextView(getContext());
            balanceText.setText("Balance : ");
            balanceText.setTextSize(14);
            balanceText.setTypeface(null, android.graphics.Typeface.ITALIC);
            balanceText.setTextColor(0xFFB0B0B0);
            row.addView(balanceText);

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

    // Per-person menu (Delete + Notes)
    private void showPersonMenu(View anchor, String name) {
        PopupMenu menu = new PopupMenu(getContext(), anchor);
        menu.getMenu().add("Delete Entry");
        menu.getMenu().add("Notes (Interest)");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Delete Entry".equals(title)) {
                showDeleteEntriesDialog(name);
            } else if ("Notes (Interest)".equals(title)) {
                showInterestNotesDialog(name);
            }
            return true;
        });
        menu.show();
    }

    private void showDeleteEntriesDialog(String name) {
        ArrayList<Entry> list = givenMap.get(name);
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
                        givenMap.remove(name);
                    }
                    saveMap(getContext());
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

                    Toast.makeText(getContext(), "Deleted selected entries for " + name,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showInterestNotesDialog(String name) {
        ArrayList<Entry> list = givenMap.get(name);
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
        return Math.round((float) dp * density);
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
