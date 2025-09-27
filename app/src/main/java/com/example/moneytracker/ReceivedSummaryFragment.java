package com.example.moneytracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class ReceivedSummaryFragment extends Fragment {
    private LinearLayout layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_received_summary, container, false);
        layout = v.findViewById(R.id.summaryLayout);
        refreshView();
        return v;
    }

    public void refreshView() {
        if (layout == null) return;
        layout.removeAllViews();
        for (String name : ReceivedFragment.receivedMap.keySet()) {
            ArrayList<ReceivedFragment.Entry> entries = ReceivedFragment.receivedMap.get(name);
            int amount = 0;
            if (entries != null) {
                for (ReceivedFragment.Entry e : entries) {
                    amount += e.getAmount();
                }
            }
            TextView tv = new TextView(getContext());
            tv.setText(name + ": ₹" + amount);
            layout.addView(tv);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }
}
