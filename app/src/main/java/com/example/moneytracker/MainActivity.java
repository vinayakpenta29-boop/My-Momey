package com.example.moneytracker;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private String[] titles = {"I Gave", "I Received", "Summary", "Transactions"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabAdapter adapter = new TabAdapter(this);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(titles[position])).attach();

        // Example: Saving a value (call this when you add/update data)
        saveSummaryData("sample_key", "sample_value");

        // Example: Reading a value (call this when you load data)
        String savedValue = getSummaryData("sample_key");
        // Use "savedValue" where needed in your app
    }

    // Save data using SharedPreferences
    private void saveSummaryData(String key, String value) {
        SharedPreferences sharedPref = getSharedPreferences("MoneyTrackerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Retrieve data using SharedPreferences
    private String getSummaryData(String key) {
        SharedPreferences sharedPref = getSharedPreferences("MoneyTrackerPrefs", MODE_PRIVATE);
        return sharedPref.getString(key, "");
    }
}
