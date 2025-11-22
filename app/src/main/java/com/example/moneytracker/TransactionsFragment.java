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
        // Always load latest persisted data
        GivenFragment.loadMap(requireContext());
        ReceivedFragment.loadMap(requireContext());

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
        // Always reload for latest
        GivenFragment.loadMap(requireContext());
        ReceivedFragment.loadMap(requireContext());

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

        // For each account, show all its entries inside a curved card/box
        for (String name : allEntriesMap.keySet()) {
            LinearLayout cardBox = new LinearLayout(getContext());
            cardBox.setOrientation(LinearLayout.VERTICAL);
            cardBox.setBackgroundResource(R.drawable.account_card_bg_for_transaction_tab);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 24, 0, 24);
            cardBox.setLayoutParams(cardParams);

            // Account name header
            TextView accountHeader = new TextView(getContext());
            accountHeader.setText(name);
            accountHeader.setTypeface(null, Typeface.BOLD);
            accountHeader.setTextSize(18);
            accountHeader.setTextColor(0xFFff2b60);
            accountHeader.setPadding(48, 24, 48, 10);
            cardBox.addView(accountHeader);

            ArrayList<EntryBase> entries = allEntriesMap.get(name);

            for (EntryBase entry : entries) {
                LinearLayout entryRow = new LinearLayout(getContext());
                entryRow.setOrientation(LinearLayout.HORIZONTAL);
                entryRow.setPadding(28, 8, 32, 8);

                // Arrow (just use your single vector directly, no need for circle wrapper)
                ImageView arrowView = new ImageView(getContext());
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
                iconParams.setMargins(0, 0, 16, 0);
                arrowView.setLayoutParams(iconParams);

                if (entry instanceof GivenFragment.Entry) {
                    arrowView.setImageResource(R.drawable.ic_arrow_up_red); // single vector: up, white circ., red border
                } else {
                    arrowView.setImageResource(R.drawable.ic_arrow_down_green); // single vector: down, white circ., green border
                }

                entryRow.addView(arrowView);

                // Amount + note (Dark Gray color)
                TextView entryDetails = new TextView(getContext());
                String details = "₹" + entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryDetails.setText(details.trim());
                entryDetails.setTextColor(0xFFFFFFFF); // Dark Gray
                entryDetails.setTypeface(null, Typeface.BOLD);
                entryDetails.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRow.addView(entryDetails);

                // Date
                TextView entryDate = new TextView(getContext());
                entryDate.setText(formatDate(entry.getDate()));
                entryDate.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                entryDate.setTextColor(0xFFFFFFFF);
                entryDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRow.addView(entryDate);

                cardBox.addView(entryRow);

                // Divider
                addDivider(cardBox, 1);
            }

            layoutTransactions.addView(cardBox);
        }
    }

    private void addDivider(LinearLayout layout, int thicknessDp) {
        View line = new View(getContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, thicknessDp * 2);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(0xFFFFFFFF);
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
