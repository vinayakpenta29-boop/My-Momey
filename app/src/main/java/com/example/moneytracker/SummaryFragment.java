package com.example.moneytracker;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class SummaryFragment extends Fragment {

    private LinearLayout layoutMoneyShouldCome, layoutIHaveToPay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_summary, container, false);
        layoutMoneyShouldCome = v.findViewById(R.id.layoutMoneyShouldCome);
        layoutIHaveToPay = v.findViewById(R.id.layoutIHaveToPay);
        refreshView();
        return v;
    }

    public void refreshView() {
        if (layoutMoneyShouldCome == null || layoutIHaveToPay == null) return;

        layoutMoneyShouldCome.removeAllViews();
        layoutIHaveToPay.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());

        TextView moneyShouldComeTitle = new TextView(getContext());
        moneyShouldComeTitle.setText("Money should Come");
        moneyShouldComeTitle.setTypeface(null, Typeface.BOLD);
        moneyShouldComeTitle.setTextSize(18);
        layoutMoneyShouldCome.addView(moneyShouldComeTitle);

        TextView iHaveToPayTitle = new TextView(getContext());
        iHaveToPayTitle.setText("I have to Pay");
        iHaveToPayTitle.setTypeface(null, Typeface.BOLD);
        iHaveToPayTitle.setTextSize(18);
        layoutIHaveToPay.addView(iHaveToPayTitle);

        for (String name : allNames) {
            ArrayList<GivenFragment.Entry> givenList = gaveMap.getOrDefault(name, new ArrayList<>());
            ArrayList<ReceivedFragment.Entry> receivedList = receivedMap.getOrDefault(name, new ArrayList<>());

            int totalGiven = 0, totalPaid = 0;
            for (EntryBase e : givenList) totalGiven += e.getAmount();
            for (EntryBase e : receivedList) totalPaid += e.getAmount();
            int balance = totalGiven - totalPaid;

            boolean hasEntries = !givenList.isEmpty() || !receivedList.isEmpty();
            if (hasEntries) {
                if (balance >= 0) {
                    layoutMoneyShouldCome.addView(
                        createAccountBox(
                            name, totalGiven, totalPaid, balance, givenList, receivedList, false, true
                        )
                    );
                } else {
                    layoutIHaveToPay.addView(
                        createAccountBox(
                            name, totalPaid, totalGiven, -balance, receivedList, givenList, true, false
                        )
                    );
                }
            }
        }
    }

    private LinearLayout createAccountBox(
            String name, int totalPrimary, int totalSecondary, int balance,
            ArrayList<? extends EntryBase> primaryList,
            ArrayList<? extends EntryBase> secondaryList,
            boolean iHaveToPaySection,
            boolean showGivenHeader) {

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{32,32,32,32,12,12,12,12});
        drawable.setColor(0xFFF8FFF3);
        drawable.setStroke(3, 0xFFBFBFBF);
        box.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 24);
        box.setLayoutParams(params);

        // Heading
        TextView heading = new TextView(getContext());
        String headingText;
        if (showGivenHeader) {
            headingText = "You Gived Money to " + name + " ₹" + totalPrimary;
        } else if (iHaveToPaySection) {
            headingText = "You Taken from " + name + " ₹" + totalPrimary;
        } else {
            headingText = name + " ₹" + totalPrimary;
        }
        heading.setText(headingText);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(0xFFEA4444);
        heading.setTextSize(20);
        heading.setBackgroundColor(0xFFA0FFA0);
        heading.setPadding(20, 16, 20, 16);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        // Section 1: All given or received entries (primary direction)
        if (!primaryList.isEmpty()) {
            for (int i = 0; i < primaryList.size(); i++) {
                EntryBase entry = primaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(16);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(16);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != primaryList.size() - 1 || !secondaryList.isEmpty()) {
                    addDivider(box, 1);
                }
            }
        }

        // Section 2: All secondary entries (the other direction)
        if (!secondaryList.isEmpty()) {
            // Add a label for secondary entries if both lists are not empty (helps clarity)
            TextView paidLabel = new TextView(getContext());
            paidLabel.setText(iHaveToPaySection ? "You Gave Entries:" : "Paid Entries:");
            paidLabel.setTypeface(null, Typeface.ITALIC);
            paidLabel.setTextColor(0xFFA0A0A0);
            paidLabel.setPadding(24, 0, 0, 0);
            box.addView(paidLabel);

            for (int i = 0; i < secondaryList.size(); i++) {
                EntryBase entry = secondaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(16);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(16);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != secondaryList.size() - 1) {
                    addDivider(box, 1);
                }
            }
        }

        // Totals & Balance
        TextView paidRow = new TextView(getContext());
        String paidRowText;
        if (iHaveToPaySection) {
            paidRowText = "You Paid to " + name + " Paid ₹" + totalSecondary;
        } else {
            paidRowText = name + " Paid ₹" + totalSecondary;
        }
        paidRow.setText(paidRowText);
        paidRow.setTypeface(null, Typeface.BOLD);
        paidRow.setTextSize(16);
        paidRow.setPadding(20, 10, 20, 10);
        paidRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(paidRow);

        TextView balanceView = new TextView(getContext());
        balanceView.setText("Balance ₹" + balance);
        balanceView.setTypeface(null, Typeface.BOLD);
        balanceView.setTextColor(0xFFEA4444);
        balanceView.setTextSize(18);
        balanceView.setPadding(20, 10, 20, 10);
        balanceView.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(balanceView);

        return box;
    }
package com.example.moneytracker;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

public class SummaryFragment extends Fragment {

    private LinearLayout layoutMoneyShouldCome, layoutIHaveToPay;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_summary, container, false);
        layoutMoneyShouldCome = v.findViewById(R.id.layoutMoneyShouldCome);
        layoutIHaveToPay = v.findViewById(R.id.layoutIHaveToPay);
        refreshView();
        return v;
    }

    public void refreshView() {
        if (layoutMoneyShouldCome == null || layoutIHaveToPay == null) return;

        layoutMoneyShouldCome.removeAllViews();
        layoutIHaveToPay.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());

        TextView moneyShouldComeTitle = new TextView(getContext());
        moneyShouldComeTitle.setText("Money should Come");
        moneyShouldComeTitle.setTypeface(null, Typeface.BOLD);
        moneyShouldComeTitle.setTextSize(18);
        layoutMoneyShouldCome.addView(moneyShouldComeTitle);

        TextView iHaveToPayTitle = new TextView(getContext());
        iHaveToPayTitle.setText("I have to Pay");
        iHaveToPayTitle.setTypeface(null, Typeface.BOLD);
        iHaveToPayTitle.setTextSize(18);
        layoutIHaveToPay.addView(iHaveToPayTitle);

        for (String name : allNames) {
            ArrayList<GivenFragment.Entry> givenList = gaveMap.getOrDefault(name, new ArrayList<>());
            ArrayList<ReceivedFragment.Entry> receivedList = receivedMap.getOrDefault(name, new ArrayList<>());

            int totalGiven = 0, totalPaid = 0;
            for (EntryBase e : givenList) totalGiven += e.getAmount();
            for (EntryBase e : receivedList) totalPaid += e.getAmount();
            int balance = totalGiven - totalPaid;

            boolean hasEntries = !givenList.isEmpty() || !receivedList.isEmpty();
            if (hasEntries) {
                if (balance >= 0) {
                    layoutMoneyShouldCome.addView(
                        createAccountBox(
                            name, totalGiven, totalPaid, balance, givenList, receivedList, false, true
                        )
                    );
                } else {
                    layoutIHaveToPay.addView(
                        createAccountBox(
                            name, totalPaid, totalGiven, -balance, receivedList, givenList, true, false
                        )
                    );
                }
            }
        }
    }

    private LinearLayout createAccountBox(
            String name, int totalPrimary, int totalSecondary, int balance,
            ArrayList<? extends EntryBase> primaryList,
            ArrayList<? extends EntryBase> secondaryList,
            boolean iHaveToPaySection,
            boolean showGivenHeader) {

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{32,32,32,32,12,12,12,12});
        drawable.setColor(0xFFF8FFF3); // light curved box as per screenshot
        drawable.setStroke(3, 0xFFBFBFBF);
        box.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 24);
        box.setLayoutParams(params);

        // Heading
        TextView heading = new TextView(getContext());
        String headingText;
        if (showGivenHeader) {
            headingText = "You Gived Money to " + name + " ₹" + totalPrimary;
        } else if (iHaveToPaySection) {
            headingText = "You Taken from " + name + " ₹" + totalPrimary;
        } else {
            headingText = name + " ₹" + totalPrimary;
        }
        heading.setText(headingText);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(0xFFEA4444);
        heading.setTextSize(20);
        heading.setBackgroundColor(0xFFA0FFA0);
        heading.setPadding(20, 16, 20, 16);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        // Entries
        if (!primaryList.isEmpty()) {
            for (int i = 0; i < primaryList.size(); i++) {
                EntryBase entry = primaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(16);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(16);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != primaryList.size() - 1 || !secondaryList.isEmpty()) {
                    addDivider(box, 1);
                }
            }
        }

        if (!secondaryList.isEmpty()) {
            TextView paidLabel = new TextView(getContext());
            paidLabel.setText(iHaveToPaySection ? "You Gave Entries:" : "Paid Entries:");
            paidLabel.setTypeface(null, Typeface.ITALIC);
            paidLabel.setTextColor(0xFFA0A0A0);
            paidLabel.setPadding(24, 0, 0, 0);
            box.addView(paidLabel);

            for (int i = 0; i < secondaryList.size(); i++) {
                EntryBase entry = secondaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(16);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(16);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != secondaryList.size() - 1) {
                    addDivider(box, 1);
                }
            }
        }

        // Totals & Balance
        TextView paidRow = new TextView(getContext());
        String paidRowText;
        if (iHaveToPaySection) {
            paidRowText = "You Paid to " + name + " Paid ₹" + totalSecondary;
        } else {
            paidRowText = name + " Paid ₹" + totalSecondary;
        }
        paidRow.setText(paidRowText);
        paidRow.setTypeface(null, Typeface.BOLD);
        paidRow.setTextSize(16);
        paidRow.setPadding(20, 10, 20, 10);
        paidRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(paidRow);

        TextView balanceView = new TextView(getContext());
        balanceView.setText("Balance ₹" + balance);
        balanceView.setTypeface(null, Typeface.BOLD);
        balanceView.setTextColor(0xFFEA4444);
        balanceView.setTextSize(18);
        balanceView.setPadding(20, 10, 20, 10);
        balanceView.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(balanceView);

        return box;
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
        } catch (ParseException e) { }
        return inputDate;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }
}
