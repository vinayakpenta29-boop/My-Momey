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

        HashMap<String, Integer> givenMap = GivenFragment.givenMap;
        HashMap<String, Integer> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(givenMap.keySet());
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
            int given = givenMap.getOrDefault(name, 0);
            int received = receivedMap.getOrDefault(name, 0);
            int balance = given - received;

            if (balance < 0) {
                layoutMoneyShouldCome.addView(createAccountBox(name, balance)); // Money should Come
            } else if (balance > 0) {
                layoutIHaveToPay.addView(createAccountBox(name, -balance)); // I have to Pay
            }
        }
    }

    private TextView createAccountBox(String name, int amount) {
        TextView box = new TextView(getContext());
        box.setText(name + ": ₹" + amount);
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
