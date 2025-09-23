package com.example.moneytracker;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;

import java.util.HashMap;

public class GivenFragment extends Fragment {

    private EditText nameInput, amountInput;
    private Button addButton;

    // static so other fragments can read it. In real apps, use ViewModel or DB.
    public static HashMap<String, Integer> givenMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_given, container, false);
        nameInput = v.findViewById(R.id.editTextName);
        amountInput = v.findViewById(R.id.editTextAmount);
        addButton = v.findViewById(R.id.buttonAdd);

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString();
            String amountStr = amountInput.getText().toString();
            if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(amountStr)) {
                int amount = Integer.parseInt(amountStr);
                givenMap.put(name, amount);
                nameInput.setText("");
                amountInput.setText("");
            }
        });

        return v;
    }
}
