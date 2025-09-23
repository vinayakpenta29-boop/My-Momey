package com.example.moneytracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class GivenSummaryFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_given_summary, container, false);
        LinearLayout layout = v.findViewById(R.id.summaryLayout);
        layout.removeAllViews();

        for (String name : GivenFragment.givenMap.keySet()) {
            int amount = GivenFragment.givenMap.get(name);
            TextView tv = new TextView(getContext());
            tv.setText(name + ": ₹" + amount);
            layout.addView(tv);
        }
        return v;
    }
}
