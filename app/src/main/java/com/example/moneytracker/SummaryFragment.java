package com.example.moneytracker;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
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

        // Use the Entry type for notes and amounts
        HashMap<String, GivenFragment.Entry> gaveMap = GivenFragment.givenMap;
        HashMap<String, ReceivedFragment.Entry> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());

        // Section title: Money should Come
        TextView moneyShouldComeTitle = new TextView(getContext());
        moneyShouldComeTitle.setText("Money should Come");
        moneyShouldComeTitle.setTypeface(null, Typeface.BOLD);
        moneyShouldComeTitle.setTextSize(18);
        layoutMoneyShouldCome.addView(moneyShouldComeTitle);

        // Section title: I have to Pay
        TextView iHaveToPayTitle = new TextView(getContext());
        iHaveToPayTitle.setText("I have to Pay");
        iHaveToPayTitle.setTypeface(null, Typeface.BOLD);
        iHaveToPayTitle.setTextSize(18);
        layoutIHaveToPay.addView(iHaveToPayTitle);

        for (String name : allNames) {
            int gave = gaveMap.containsKey(name) ? gaveMap.get(name).amount : 0;
            int received = receivedMap.containsKey(name) ? receivedMap.get(name).amount : 0;
            String noteGave = gaveMap.containsKey(name) ? gaveMap.get(name).note : "";
            String noteReceived = receivedMap.containsKey(name) ? receivedMap.get(name).note : "";
            int balance = gave - received;

            if (balance > 0) {
                layoutMoneyShouldCome.addView(createAccountBox(name, balance, noteGave));
            } else if (balance < 0) {
                layoutIHaveToPay.addView(createAccountBox(name, -balance, noteReceived));
            }
            // balance==0: do not show
        }
    }

    private TextView createAccountBox(String name, int amount, String note) {
        TextView box = new TextView(getContext());
        String text = name + ": ₹" + amount;
        if (note != null && !note.isEmpty()) {
            text += " (" + note + ")";
        }
        box.setText(text);
        box.setPadding(20, 20, 20, 20);
        box.setTextSize(16);
        box.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);
        box.setLayoutParams(params);
        return box;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }
}
