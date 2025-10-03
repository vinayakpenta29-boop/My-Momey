package com.example.moneytracker;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class TransactionsFragment extends Fragment {

    private LinearLayout layoutTransactions;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_transactions, container, false);
        layoutTransactions = v.findViewById(R.id.layoutTransactions);
        showAllTransactions();
        return v;
    }

    private void showAllTransactions() {
        layoutTransactions.removeAllViews();

        // Combine all accounts from both maps
        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        Map<String, ArrayList<EntryBase>> allEntriesMap = new TreeMap<>();

        // Add Given entries
        for (String name : gaveMap.keySet()) {
            ArrayList<GivenFragment.Entry> entries = gaveMap.get(name);
            for (GivenFragment.Entry entry : entries) {
                if (!allEntriesMap.containsKey(name)) allEntriesMap.put(name, new ArrayList<>());
                allEntriesMap.get(name).add(entry);
            }
        }
        // Add Received entries
        for (String name : receivedMap.keySet()) {
            ArrayList<ReceivedFragment.Entry> entries = receivedMap.get(name);
            for (ReceivedFragment.Entry entry : entries) {
                if (!allEntriesMap.containsKey(name)) allEntriesMap.put(name, new ArrayList<>());
                allEntriesMap.get(name).add(entry);
            }
        }

        // For each account, show all its entries
        for (String name : allEntriesMap.keySet()) {
            // Header
            TextView accountHeader = new TextView(getContext());
            accountHeader.setText(name);
            accountHeader.setTypeface(null, Typeface.BOLD);
            accountHeader.setTextSize(18);
            accountHeader.setTextColor(0xFF27AE60);
            accountHeader.setPadding(16, 24, 16, 10);
            layoutTransactions.addView(accountHeader);

            ArrayList<EntryBase> entries = allEntriesMap.get(name);

            for (EntryBase entry : entries) {
                LinearLayout entryRow = new LinearLayout(getContext());
                entryRow.setOrientation(LinearLayout.HORIZONTAL);
                entryRow.setPadding(16, 8, 16, 8);

                // Amount + note
                TextView entryDetails = new TextView(getContext());
                String details = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryDetails.setText(details.trim());
                entryDetails.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                // date
                TextView entryDate = new TextView(getContext());
                entryDate.setText(formatDate(entry.getDate()));
                entryDate.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                entryDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                entryRow.addView(entryDetails);
                entryRow.addView(entryDate);

                layoutTransactions.addView(entryRow);

                // Divider
                addDivider(layoutTransactions, 1);
            }
        }
    }

    private void addDivider(LinearLayout layout, int thicknessDp) {
        View line = new View(getContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, thicknessDp * 2);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(0xFFD1D1D1);
        layout.addView(line);
    }

    private String formatDate(String inputDate) {
        try {
            Date date = null;
            if (inputDate.contains("/")) {
                date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(inputDate);
            } else if (inputDate.contains("-") && inputDate.length() >= 10) {
                date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(inputDate.substring(0, 10));
            }
            if (date != null) {
                return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date);
            }
        } catch (ParseException e) {}
        return inputDate;
    }
}
