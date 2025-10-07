package com.example.moneytracker;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TransactionsFragment extends Fragment {

    private LinearLayout layoutTransactions;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_transactions, container, false);
        layoutTransactions = v.findViewById(R.id.layoutTransactions);
        showAllTransactions();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        showAllTransactions();
    }

    private void showAllTransactions() {
        layoutTransactions.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        boolean isGivenEmpty = (gaveMap == null || gaveMap.isEmpty());
        boolean isReceivedEmpty = (receivedMap == null || receivedMap.isEmpty());

        if (isGivenEmpty && isReceivedEmpty) {
            TextView noData = new TextView(getContext());
            noData.setText("No Data in I Gave or I Received!");
            noData.setTextSize(18);
            noData.setTypeface(null, Typeface.BOLD);
            noData.setTextColor(0xFFFF3A44);
            noData.setPadding(32, 48, 32, 0);
            noData.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            layoutTransactions.addView(noData);
            return;
        }

        Map<String, ArrayList<EntryBase>> allEntriesMap = new TreeMap<>();

        // Add Given entries
        if (gaveMap != null) {
            for (String name : gaveMap.keySet()) {
                ArrayList<GivenFragment.Entry> entries = gaveMap.get(name);
                for (GivenFragment.Entry entry : entries) {
                    if (!allEntriesMap.containsKey(name)) allEntriesMap.put(name, new ArrayList<>());
                    allEntriesMap.get(name).add(entry);
                }
            }
        }
        // Add Received entries
        if (receivedMap != null) {
            for (String name : receivedMap.keySet()) {
                ArrayList<ReceivedFragment.Entry> entries = receivedMap.get(name);
                for (ReceivedFragment.Entry entry : entries) {
                    if (!allEntriesMap.containsKey(name)) allEntriesMap.put(name, new ArrayList<>());
                    allEntriesMap.get(name).add(entry);
                }
            }
        }

        // For each account, show all its entries
        for (String name : allEntriesMap.keySet()) {
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

                // Arrow
                ImageView arrowView = new ImageView(getContext());

                if (entry instanceof GivenFragment.Entry) {
                    arrowView.setImageResource(R.drawable.ic_arrow_up_red); // export
                } else {
                    arrowView.setImageResource(R.drawable.ic_arrow_down_green); // import
                }
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
                iconParams.setMargins(0, 0, 16, 0);
                arrowView.setLayoutParams(iconParams);

                entryRow.addView(arrowView);

                // Amount + note (white color)
                TextView entryDetails = new TextView(getContext());
                String details = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryDetails.setText(details.trim());
                entryDetails.setTextColor(0xFFFFFFFF); // white
                entryDetails.setTypeface(null, Typeface.BOLD);
                entryDetails.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                entryRow.addView(entryDetails);

                TextView entryDate = new TextView(getContext());
                entryDate.setText(formatDate(entry.getDate()));
                entryDate.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                entryDate.setTextColor(0xFFA0A0A0);
                entryDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

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
