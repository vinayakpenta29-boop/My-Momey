package com.example.moneytracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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
import java.util.Set;
import java.util.TreeSet;

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
        if (layoutTransactions == null) return;

        layoutTransactions.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());

        for (String name : allNames) {
            ArrayList<GivenFragment.Entry> givenList = gaveMap.getOrDefault(name, new ArrayList<>());
            ArrayList<ReceivedFragment.Entry> receivedList = receivedMap.getOrDefault(name, new ArrayList<>());

            if (givenList.isEmpty() && receivedList.isEmpty()) continue;

            // Account Name Header
            TextView accountHeader = new TextView(getContext());
            accountHeader.setText(name);
            accountHeader.setTextSize(20);
            accountHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            accountHeader.setPadding(16, 32, 16, 16);
            layoutTransactions.addView(accountHeader);

            // Given Entries
            for (GivenFragment.Entry entry : givenList) {
                layoutTransactions.addView(createTransactionRow("Gave", entry.getAmount(), entry.getNote(), entry.getDate()));
            }
            // Received Entries
            for (ReceivedFragment.Entry entry : receivedList) {
                layoutTransactions.addView(createTransactionRow("Received", entry.getAmount(), entry.getNote(), entry.getDate()));
            }
        }
    }

    private View createTransactionRow(String type, int amount, String note, String dateStr) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView typeView = new TextView(getContext());
        typeView.setText(type);
        typeView.setTextSize(16);
        typeView.setPadding(8, 8, 16, 8);

        TextView amountView = new TextView(getContext());
        amountView.setText("₹" + amount);
        amountView.setTextSize(16);
        amountView.setPadding(8, 8, 16, 8);

        TextView noteView = new TextView(getContext());
        noteView.setText(TextUtils.isEmpty(note) ? "" : note);
        noteView.setTextSize(16);
        noteView.setPadding(8, 8, 16, 8);

        TextView dateView = new TextView(getContext());
        dateView.setText(formatDate(dateStr));
        dateView.setTextSize(16);
        dateView.setPadding(8, 8, 8, 8);
        dateView.setGravity(Gravity.END);

        row.addView(typeView);
        row.addView(amountView);
        row.addView(noteView);
        row.addView(dateView);

        return row;
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
        } catch (ParseException e) { }
        return inputDate;
    }
}
