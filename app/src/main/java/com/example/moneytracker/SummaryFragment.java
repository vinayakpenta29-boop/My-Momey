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
import java.util.ArrayList;
import java.util.HashMap;
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

        // Section titles
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

            if (balance > 0) {
                layoutMoneyShouldCome.addView(createAccountBox(name, totalGiven, totalPaid, balance, givenBase, receivedBase, false));
            } else if (balance < 0) {
                layoutIHaveToPay.addView(createAccountBox(name, totalPaid, totalGiven, -balance, receivedBase, givenBase, true));
            }
        }
    }

    private LinearLayout createAccountBox(String name, int totalTaken, int totalPaid, int balance,
                                          ArrayList<EntryBase> takenList,
                                          ArrayList<EntryBase> paidList,
                                          boolean iHaveToPaySection) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(32);
        drawable.setColor(0xFFF8FFF3);
        drawable.setStroke(3, 0xFFBFBFBF);
        box.setBackground(drawable);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 24);
        box.setLayoutParams(params);

        // Heading
        TextView heading = new TextView(getContext());
        heading.setText((iHaveToPaySection ? "You Taken from " : "") + name + " " + totalTaken);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(0xFFEA4444);
        heading.setTextSize(20);
        heading.setBackgroundColor(0xFFA0FFA0);
        heading.setPadding(20, 16, 20, 16);
        box.addView(heading);

        // Entries Table
        for (EntryBase entry : takenList) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView entryLeft = new TextView(getContext());
            String leftText = entry.getAmount() + (!TextUtils.isEmpty(entry.getNote()) ? " (" + entry.getNote() + ")" : "");
            entryLeft.setText(leftText);
            entryLeft.setTextSize(16);
            entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(entryLeft);

            TextView entryRight = new TextView(getContext());
            entryRight.setText(entry.getDate());
            entryRight.setTextSize(16);
            entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            row.addView(entryRight);

            row.setPadding(20, 10, 20, 10);
            box.addView(row);
        }

        if (!takenList.isEmpty()) {
            addDivider(box);
        }

        // Paid entries (show as total)
        TextView paidRow = new TextView(getContext());
        paidRow.setText((iHaveToPaySection ? "You Paid to " + name + " Paid" : name + " Paid") + "  " + totalPaid);
        paidRow.setTypeface(null, Typeface.BOLD);
        paidRow.setTextSize(16);
        paidRow.setPadding(20, 10, 20, 10);
        box.addView(paidRow);

        // Balance row
        TextView balanceView = new TextView(getContext());
        balanceView.setText("Balance   " + balance);
        balanceView.setTypeface(null, Typeface.BOLD);
        balanceView.setTextColor(0xFFEA4444);
        balanceView.setTextSize(18);
        balanceView.setPadding(20, 10, 20, 10);
        box.addView(balanceView);

        return box;
    }

    private void addDivider(LinearLayout layout) {
        View line = new View(getContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(0xFFD1D1D1);
        layout.addView(line);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }
}
