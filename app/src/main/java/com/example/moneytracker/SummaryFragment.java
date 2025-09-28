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
            ArrayList<EntryBase> givenBase = new ArrayList<>(givenList);
            ArrayList<EntryBase> receivedBase = new ArrayList<>(receivedList);

            int totalGiven = 0, totalPaid = 0;
            for (EntryBase e : givenBase) totalGiven += e.getAmount();
            for (EntryBase e : receivedBase) totalPaid += e.getAmount();
            int balance = totalGiven - totalPaid;

            boolean hasEntries = !givenBase.isEmpty() || !receivedBase.isEmpty();
            if (hasEntries) {
                if (balance > 0) {
                    layoutMoneyShouldCome.addView(createAccountBox(
                            name, totalGiven, totalPaid, balance, givenBase, receivedBase, false, true));
                } else if (balance < 0) {
                    layoutIHaveToPay.addView(createAccountBox(
                            name, totalPaid, totalGiven, -balance, receivedBase, givenBase, true, false));
                } else {
                    // Show box for zero balance too
                    layoutMoneyShouldCome.addView(createAccountBox(
                            name, totalGiven, totalPaid, balance, givenBase, receivedBase, false, true));
                }
            }
        }
    }

    // showGivenHeader is true only for "Money Should Come" section
    private LinearLayout createAccountBox(
            String name, int totalTaken, int totalPaid, int balance,
            ArrayList<EntryBase> takenList,
            ArrayList<EntryBase> paidList,
            boolean iHaveToPaySection,
            boolean showGivenHeader) {

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{
                32,32,32,32,12,12,12,12 // Top more curved
        });
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
            headingText = "You Gived Money to " + name + " ₹" + totalTaken;
        } else if (iHaveToPaySection) {
            headingText = "You Taken from " + name + " ₹" + totalTaken;
        } else {
            headingText = name + " ₹" + totalTaken;
        }
        heading.setText(headingText);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(0xFFEA4444);
        heading.setTextSize(20);
        heading.setBackgroundColor(0xFFA0FFA0);
        heading.setPadding(20, 16, 20, 16);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        // Entries Table
        for (int i = 0; i < takenList.size(); i++) {
            EntryBase entry = takenList.get(i);
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

            if (i != takenList.size() - 1) {
                addDivider(box, 1);
            }
        }

        // Paid row
        TextView paidRow = new TextView(getContext());
        String paidRowText;
        if (iHaveToPaySection) {
            paidRowText = "You Paid to " + name + " Paid ₹" + totalPaid;
        } else {
            paidRowText = name + " Paid ₹" + totalPaid;
        }
        paidRow.setText(paidRowText);
        paidRow.setTypeface(null, Typeface.BOLD);
        paidRow.setTextSize(16);
        paidRow.setPadding(20, 10, 20, 10);
        paidRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(paidRow);

        // Balance row
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
        // Parse yyyy-MM-dd or dd/MM/yyyy to dd-MM-yyyy
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
