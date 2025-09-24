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

    private LinearLayout layoutGave, layoutReceived;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_summary, container, false);
        layoutGave = v.findViewById(R.id.layoutGave);
        layoutReceived = v.findViewById(R.id.layoutReceived);
        refreshView();
        return v;
    }

    public void refreshView() {
        if (layoutGave == null || layoutReceived == null) return;

        layoutGave.removeAllViews();
        layoutReceived.removeAllViews();

        HashMap<String, Integer> givenMap = GivenFragment.givenMap;
        HashMap<String, Integer> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(givenMap.keySet());
        allNames.addAll(receivedMap.keySet());

        // "I Gave" section title
        TextView gaveTitle = new TextView(getContext());
        gaveTitle.setText("I Gave");
        gaveTitle.setTypeface(null, Typeface.BOLD);
        gaveTitle.setTextSize(18);
        layoutGave.addView(gaveTitle);

        // "I Received" section title
        TextView receivedTitle = new TextView(getContext());
        receivedTitle.setText("I Received");
        receivedTitle.setTypeface(null, Typeface.BOLD);
        receivedTitle.setTextSize(18);
        layoutReceived.addView(receivedTitle);

        for (String name : allNames) {
            int given = givenMap.getOrDefault(name, 0);
            int received = receivedMap.getOrDefault(name, 0);
            int balance = given - received;

            if (balance > 0) {
                layoutGave.addView(createAccountBox(name, balance)); // Positive: I Gave
            } else if (balance < 0) {
                layoutReceived.addView(createAccountBox(name, -balance)); // Negative: I Received (show positive)
            }
            // If balance == 0, do not display
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
