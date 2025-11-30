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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

import com.example.moneytracker.BcStore;
import com.example.moneytracker.BcStore.BcScheme;

public class ReceivedFragment extends Fragment {

    private EditText nameInput, amountInput, noteInput;
    private Button addButton;
    private RadioGroup categoryGroup;
    private LinearLayout layoutBalanceList;
    private ImageButton btnMoreTopReceived;

    // Belongs-to-BC UI
    private TextView tvBelongsToBc;
    private Spinner spinnerBelongsToBc;

    public static class Entry implements EntryBase {
        public int amount;
        public String note;
        public String date;
        public String category;
        public String bcKey;      // optional BC scheme id

        public Entry(int amount, String note, String date, String category, String bcKey) {
            this.amount = amount;
            this.note = note;
            this.date = date;
            this.category = category;
            this.bcKey = bcKey;
        }

        @Override public int getAmount() { return amount; }
        @Override public String getNote() { return note; }
        @Override public String getDate() { return date; }
        public String getCategory() { return category; }
        public String getBcKey() { return bcKey; }

        public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("amount", amount);
            obj.put("note", note);
            obj.put("date", date);
            obj.put("category", category);
            obj.put("bcKey", bcKey);
            return obj;
        }

        public static Entry fromJSON(JSONObject obj) throws JSONException {
            return new Entry(
                    obj.getInt("amount"),
                    obj.optString("note"),
                    obj.optString("date"),
                    obj.has("category") ? obj.getString("category") : "",
                    obj.optString("bcKey", "")
            );
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
        BcStore.load(requireContext());

        View v = inflater.inflate(R.layout.fragment_received, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        noteInput = v.findViewById(R.id.editTextNote);
        addButton = v.findViewById(R.id.buttonAdd);
        layoutBalanceList = v.findViewById(R.id.layoutBalanceList);
        categoryGroup = v.findViewById(R.id.radioGroupCategory);
        btnMoreTopReceived = v.findViewById(R.id.btnMoreTopReceived);

        tvBelongsToBc = v.findViewById(R.id.tvBelongsToBc);
        spinnerBelongsToBc = v.findViewById(R.id.spinnerBelongsToBc);

        addButton.setBackgroundResource(R.drawable.orange_rounded_button);

        if (btnMoreTopReceived != null) {
            btnMoreTopReceived.setOnClickListener(a -> {
                BcUiHelper.showBcManagerMenu(this, a);
                updateBcSpinner();
            });
        }

        updateBcSpinner();

        categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            private int lastCheckedId = -1;
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == -1) {
                    lastCheckedId = -1;
                    tvBelongsToBc.setVisibility(View.GONE);
                    spinnerBelongsToBc.setVisibility(View.GONE);
                    return;
                }
                if (checkedId == lastCheckedId) {
                    group.clearCheck();
                    lastCheckedId = -1;
                    tvBelongsToBc.setVisibility(View.GONE);
                    spinnerBelongsToBc.setVisibility(View.GONE);
                } else {
                    lastCheckedId = checkedId;
                    RadioButton rb = group.findViewById(checkedId);
                    boolean isBc = rb != null && "BC".equalsIgnoreCase(rb.getText().toString());
                    tvBelongsToBc.setVisibility(isBc ? View.VISIBLE : View.GONE);
                    spinnerBelongsToBc.setVisibility(isBc ? View.VISIBLE : View.GONE);
                    if (isBc) {
                        updateBcSpinner();
                    }
                }
            }
        });

        addButton.setOnClickListener(view -> addReceivedEntry(v));

        updateBalanceList();
        return v;
    }

    private void updateBcSpinner() {
        if (spinnerBelongsToBc == null) return;
        HashMap<String, ArrayList<BcScheme>> bcMap = BcStore.getBcMap();
        List<String> labels = new ArrayList<>();
        for (String key : bcMap.keySet()) {
            ArrayList<BcScheme> list = bcMap.get(key);
            if (list == null) continue;
            for (BcScheme s : list) {
                if (TextUtils.isEmpty(s.id)) {
                    s.id = key + "|" + s.name;
                }
                labels.add(s.id);
            }
        }
        if (labels.isEmpty()) {
            spinnerBelongsToBc.setAdapter(null);
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBelongsToBc.setAdapter(adapter);
    }

    private void addReceivedEntry(View rootView) {
        String name = nameInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String noteStr = noteInput.getText().toString().trim();

        int checkedId = categoryGroup.getCheckedRadioButtonId();
        String category = "";
        if (checkedId != -1) {
            RadioButton selected = rootView.findViewById(checkedId);
            category = selected.getText().toString();   // "Interest", "EMI", "BC"
        }

        String bcKey = "";
        if ("BC".equalsIgnoreCase(category) &&
                spinnerBelongsToBc.getVisibility() == View.VISIBLE &&
                spinnerBelongsToBc.getSelectedItem() != null) {
            bcKey = spinnerBelongsToBc.getSelectedItem().toString();
        }

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
            try {
                int amount = Integer.parseInt(amountStr);
                String dateStr = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
                Entry entry = new Entry(amount, noteStr, dateStr, category, bcKey);
                if (!receivedMap.containsKey(name)) {
                    receivedMap.put(name, new ArrayList<>());
                }
                receivedMap.get(name).add(entry);

                if (!TextUtils.isEmpty(bcKey)) {
                    BcStore.markBcInstallmentDone(bcKey, dateStr);
                    BcStore.save(getContext());
                }

                saveMap(getContext());
                Toast.makeText(getContext(), "Added " + name + ": ₹" + amount,
                        Toast.LENGTH_SHORT).show();
                nameInput.setText("");
                amountInput.setText("");
                noteInput.setText("");
                categoryGroup.clearCheck();
                tvBelongsToBc.setVisibility(View.GONE);
                spinnerBelongsToBc.setVisibility(View.GONE);
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
            Toast.makeText(getContext(), "Enter both name and amount",
                    Toast.LENGTH_SHORT).show();
        }
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

            for (ReceivedFragment.Entry e : receivedMap.get(name)) {
                if (TextUtils.isEmpty(e.category)) {
                    totalReceived += e.getAmount();
                }
            }

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

            TextView nameTv = new TextView(getContext());
            nameTv.setText(name);
            nameTv.setTextSize(16);
            nameTv.setTextColor(0xFF252525);
            nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(nameTv);

            TextView greenLabel = new TextView(getContext());
            greenLabel.setText("₹" + totalReceived);
            greenLabel.setTextSize(14);
            greenLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            greenLabel.setPadding(16, 2, 16, 2);
            greenLabel.setBackgroundResource(R.drawable.balance_label_green);
            greenLabel.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams greenParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
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
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            pinkParams.setMargins(8, 0, 0, 0);
            balanceLabel.setLayoutParams(pinkParams);
            row.addView(balanceLabel);

            ImageView morePerson = new ImageView(getContext());
            LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                    UiUtils.dpToPx(getContext(), 24), UiUtils.dpToPx(getContext(), 24));
            moreParams.setMargins(UiUtils.dpToPx(getContext(), 8), 0, 0, 0);
            morePerson.setLayoutParams(moreParams);
            morePerson.setImageResource(R.drawable.ic_more_vert);
            row.addView(morePerson);

            morePerson.setOnClickListener(v -> showPersonMenu(v, name));

            layoutBalanceList.addView(row);

            if (i != names.size() - 1) {
                UiUtils.addDividerWithMargin(getContext(), layoutBalanceList, 1);
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
                showInterestNotesDialog(name);
            }
            return true;
        });
        menu.show();
    }

    private void showDeleteEntriesDialog(String name) {
        ArrayList<Entry> list = receivedMap.get(name);
        if (list == null || list.isEmpty()) {
            Toast.makeText(getContext(), "No entries to delete for " + name,
                    Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "No entries selected",
                                Toast.LENGTH_SHORT).show();
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

    private void showInterestNotesDialog(String name) {
        ArrayList<Entry> list = receivedMap.get(name);
        if (list == null || list.isEmpty()) {
            Toast.makeText(getContext(), "No entries for " + name,
                    Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "No Interest entries for " + name,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scrollView = new ScrollView(getContext());
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(UiUtils.dpToPx(getContext(), 16),
                UiUtils.dpToPx(getContext(), 8),
                UiUtils.dpToPx(getContext(), 16),
                UiUtils.dpToPx(getContext(), 8));
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
            tv.setPadding(0, UiUtils.dpToPx(getContext(), 4), 0,
                    UiUtils.dpToPx(getContext(), 4));
            container.addView(tv);
        }

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Interest (Vyaj) – " + name)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
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
