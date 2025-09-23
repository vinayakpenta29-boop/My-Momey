package com.example.moneytracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.HashMap;

public class GivenFragment extends Fragment {

    private EditText nameInput, amountInput;
    private Button addButton;

    public static HashMap<String, Integer> givenMap = new HashMap<>();
    private GivenSummaryFragment summaryFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_given, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        addButton = v.findViewById(R.id.buttonAdd);

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    // Accumulate amount if name exists
                    if (givenMap.containsKey(name)) {
                        amount += givenMap.get(name);
                    }
                    givenMap.put(name, amount);
                    Toast.makeText(getContext(), "Added " + name + ": ₹" + amount, Toast.LENGTH_SHORT).show();
                    nameInput.setText("");
                    amountInput.setText("");

                    // Force refresh summary UI if fragment alive
                    refreshSummary();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please enter both name and amount", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    private void refreshSummary() {
        // Try to find fragment and call its update
        if (getActivity() != null) {
            GivenSummaryFragment fragment = (GivenSummaryFragment) getActivity()
                    .getSupportFragmentManager()
                    .findFragmentByTag("f2");  // "f2" usually second tab index tag for ViewPager2
            if (fragment != null) {
                fragment.refreshView();
            }
        }
    }
}
