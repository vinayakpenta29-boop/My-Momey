package com.example.moneytracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabAdapter extends FragmentStateAdapter {

    public TabAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new GivenFragment();
            case 1: return new ReceivedFragment();
            case 2: return new SummaryFragment();
            case 3: return new TransactionsFragment(); // <-- Added this line
            default: return new GivenFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // <-- Updated to 4 fragments
    }
}
