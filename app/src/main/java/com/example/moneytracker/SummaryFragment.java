package com.example.moneytracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class SummaryFragment extends Fragment {

    private LinearLayout layoutMoneyShouldCome, layoutIHaveToPay;
    private ImageButton btnDeleteAccounts;
    private TextView textBalanceMoneyShouldCome, textBalanceIHaveToPay;
    private Typeface alumniSansMedium;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Always load latest persisted data
        GivenFragment.loadMap(requireContext());
        ReceivedFragment.loadMap(requireContext());
        View v = inflater.inflate(R.layout.fragment_summary, container, false);
        layoutMoneyShouldCome = v.findViewById(R.id.layoutMoneyShouldCome);
        layoutIHaveToPay = v.findViewById(R.id.layoutIHaveToPay);
        btnDeleteAccounts = v.findViewById(R.id.btnDeleteAccounts);
        textBalanceMoneyShouldCome = v.findViewById(R.id.textBalanceMoneyShouldCome);
        textBalanceIHaveToPay = v.findViewById(R.id.textBalanceIHaveToPay);

        // Load custom font from res/font
        alumniSansMedium = ResourcesCompat.getFont(requireContext(), R.font.alumnisans_medium);

        btnDeleteAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAccountDeleteDialog();
            }
        });

        refreshView();
        return v;
    }

    public void refreshView() {
        // Always reload for latest
        GivenFragment.loadMap(requireContext());
        ReceivedFragment.loadMap(requireContext());

        if (layoutMoneyShouldCome == null || layoutIHaveToPay == null) return;

        layoutMoneyShouldCome.removeAllViews();
        layoutIHaveToPay.removeAllViews();

        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());

        int totalMoneyShouldCome = 0;
        int totalIHaveToPay = 0;

        for (String name : allNames) {
            ArrayList<GivenFragment.Entry> givenList = gaveMap.getOrDefault(name, new ArrayList<>());
            ArrayList<ReceivedFragment.Entry> receivedList = receivedMap.getOrDefault(name, new ArrayList<>());

            int totalGiven = 0, totalPaid = 0;
            for (EntryBase e : givenList) totalGiven += e.getAmount();
            for (EntryBase e : receivedList) totalPaid += e.getAmount();
            int balance = totalGiven - totalPaid;
            boolean hasEntries = !givenList.isEmpty() || !receivedList.isEmpty();

            if (hasEntries) {
                if (balance > 0) {
                    layoutMoneyShouldCome.addView(
                            createAccountBox(name, totalGiven, totalPaid, balance, givenList, receivedList, false, true));
                    totalMoneyShouldCome += balance;
                } else if (balance < 0) {
                    layoutIHaveToPay.addView(
                            createAccountBox(name, totalPaid, totalGiven, -balance, receivedList, givenList, true, false));
                    totalIHaveToPay += -balance;
                } else if (balance == 0 && totalPaid > totalGiven) {
                    layoutIHaveToPay.addView(
                            createAccountBox(name, totalPaid, totalGiven, 0, receivedList, givenList, true, false));
                } else if (balance == 0) {
                    layoutMoneyShouldCome.addView(
                            createAccountBox(name, totalGiven, totalPaid, 0, givenList, receivedList, false, true));
                }
            }
        }

        // Set total balances in curved boxes
        if (textBalanceMoneyShouldCome != null)
            textBalanceMoneyShouldCome.setText("₹" + totalMoneyShouldCome);
        if (textBalanceIHaveToPay != null)
            textBalanceIHaveToPay.setText("₹" + totalIHaveToPay);
    }

    private void showAccountDeleteDialog() {
        HashMap<String, ArrayList<GivenFragment.Entry>> gaveMap = GivenFragment.givenMap;
        HashMap<String, ArrayList<ReceivedFragment.Entry>> receivedMap = ReceivedFragment.receivedMap;
        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(gaveMap.keySet());
        allNames.addAll(receivedMap.keySet());
        final List<String> accountList = new ArrayList<>(allNames);

        if (accountList.isEmpty()) {
            Toast.makeText(getContext(), "No accounts to delete!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] checkedArr = new boolean[accountList.size()];
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select accounts to delete");
        builder.setMultiChoiceItems(accountList.toArray(new String[0]), checkedArr, new DialogInterface.OnMultiChoiceClickListener() {
            @Override public void onClick(DialogInterface dialog, int which, boolean isChecked) { }
        });

        builder.setNegativeButton("Cancel", null);

        builder.setPositiveButton("Delete", (dialog, which) -> {
            List<String> selectedAccounts = new ArrayList<>();
            for (int i = 0; i < accountList.size(); i++) {
                if (((AlertDialog)dialog).getListView().isItemChecked(i)) {
                    selectedAccounts.add(accountList.get(i));
                }
            }
            if (selectedAccounts.isEmpty()) {
                Toast.makeText(getContext(), "No accounts selected", Toast.LENGTH_SHORT).show();
                return;
            }
            showDeleteConfirmation(selectedAccounts);
        });

        builder.show();
    }

    private void showDeleteConfirmation(List<String> selectedAccounts) {
        StringBuilder msg = new StringBuilder();
        for (String name : selectedAccounts) {
            msg.append(name).append(", ");
        }
        if (msg.length() > 2) msg.setLength(msg.length() - 2);
        String message = "Are you sure you want to delete these accounts?" + msg.toString();

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage(message)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    deleteSelectedAccounts(selectedAccounts);
                })
                .show();
    }

    private void deleteSelectedAccounts(List<String> selectedAccounts) {
        for (String name : selectedAccounts) {
            GivenFragment.deleteAccount(getContext(), name);
            ReceivedFragment.deleteAccount(getContext(), name);
        }
        Toast.makeText(getContext(), "Deleted: " + TextUtils.join(", ", selectedAccounts), Toast.LENGTH_SHORT).show();
        refreshView();
    }

    private LinearLayout createAccountBox(
            String name, int totalPrimary, int totalSecondary, int balance,
            ArrayList<? extends EntryBase> primaryList,
            ArrayList<? extends EntryBase> secondaryList,
            boolean iHaveToPaySection,
            boolean showGivenHeader) {

        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{32,32,32,32,12,12,12,12});
        drawable.setColor(0xFFF8FFF3);
        drawable.setStroke(3, 0xFFBFBFBF);
        box.setBackground(drawable);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 24, 0, 24);
        box.setLayoutParams(params);

        // Heading text with custom font and blue span for Account Name + Amount
        TextView heading = new TextView(getContext());
        String baseText;
        int blueColor = 0xFF2574FF;

        if (showGivenHeader) {
            baseText = "You Gived Money to " + name + " ₹" + totalPrimary;
            heading.setBackgroundColor(0xFFA0FFA0);
        } else if (iHaveToPaySection) {
            baseText = "You Taken from " + name + " ₹" + totalPrimary;
            heading.setBackgroundColor(0xFFA0D0FF);
        } else {
            baseText = name + " ₹" + totalPrimary;
            heading.setBackgroundColor(0xFFA0FFA0);
        }

        int nameStart = -1;
        int nameEnd = -1;
        if (showGivenHeader || iHaveToPaySection) {
            nameStart = baseText.indexOf(name);
            if (nameStart != -1) {
                nameEnd = baseText.length();
            }
        } else {
            nameStart = 0;
            nameEnd = baseText.length();
        }

        SpannableString spannable = new SpannableString(baseText);
        if (nameStart >= 0 && nameEnd > nameStart) {
            spannable.setSpan(new ForegroundColorSpan(blueColor),
                nameStart, nameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        heading.setText(spannable);
        heading.setTypeface(alumniSansMedium); // custom font
        heading.setTextColor(0xFFEA4444);
        heading.setTextSize(18);
        heading.setPadding(20, 16, 20, 16);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);

        // Entries Primary (Given or Received)
        if (!primaryList.isEmpty()) {
            for (int i = 0; i < primaryList.size(); i++) {
                EntryBase entry = primaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(14);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(14);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != primaryList.size() - 1 || !secondaryList.isEmpty()) {
                    addDivider(box, 1);
                }
            }
        }

        // Entries Secondary (Paid, Given, Received)
        if (!secondaryList.isEmpty()) {
            TextView paidLabel = new TextView(getContext());
            paidLabel.setText(iHaveToPaySection ? "You Gave Entries:" : "Paid Entries:");
            paidLabel.setTypeface(null, Typeface.ITALIC);
            paidLabel.setTextColor(0xFFA0A0A0);
            paidLabel.setPadding(24, 0, 0, 0);
            box.addView(paidLabel);

            for (int i = 0; i < secondaryList.size(); i++) {
                EntryBase entry = secondaryList.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView entryLeft = new TextView(getContext());
                String leftText = entry.getAmount() + " " + (TextUtils.isEmpty(entry.getNote()) ? "" : entry.getNote());
                entryLeft.setText(leftText.trim());
                entryLeft.setTextSize(14);
                entryLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(entryLeft);

                TextView entryRight = new TextView(getContext());
                entryRight.setText(formatDate(entry.getDate()));
                entryRight.setTextSize(14);
                entryRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                entryRight.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                row.addView(entryRight);

                row.setPadding(20, 10, 20, 10);
                box.addView(row);

                if (i != secondaryList.size() - 1) {
                    addDivider(box, 1);
                }
            }
        }

        // Totals & Balance
        TextView paidRow = new TextView(getContext());
        String paidRowText;
        if (iHaveToPaySection) {
            paidRowText = "You Paid to " + name + " Paid ₹" + totalSecondary;
        } else {
            paidRowText = name + " Paid ₹" + totalSecondary;
        }
        paidRow.setText(paidRowText);
        paidRow.setTypeface(alumniSansMedium); // custom font here
        paidRow.setTextSize(14);
        paidRow.setPadding(20, 10, 20, 10);
        paidRow.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(paidRow);

        // Balance or Settled
        if (balance == 0) {
            TextView settledView = new TextView(getContext());
            settledView.setText("Settled");
            settledView.setTypeface(null, Typeface.BOLD_ITALIC);
            settledView.setTextColor(0xFF27AE60);
            settledView.setTextSize(18);
            settledView.setPadding(20, 10, 20, 10);
            settledView.setGravity(Gravity.CENTER);
            box.addView(settledView);
        } else {
            TextView balanceView = new TextView(getContext());
            balanceView.setText("Balance ₹" + balance);
            balanceView.setTypeface(null, Typeface.BOLD);
            balanceView.setTextColor(0xFFEA4444);
            balanceView.setTextSize(18);
            balanceView.setPadding(20, 10, 20, 10);
            balanceView.setGravity(Gravity.CENTER_VERTICAL);
            box.addView(balanceView);
        }

        return box;
    }

    private void addDivider(LinearLayout layout, int thicknessDp) {
        View line = new View(getContext());
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, thicknessDp * 2);
        line.setLayoutParams(lineParams);
        line.setBackgroundColor(0xFFD1D1D1);
        layout.addView(line);
    }

    private String formatDate(String inputDate) {
        try {
            Date date = null;
            if (inputDate.contains("/")) {
                date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(inputDate);
            } else if (inputDate.contains("-") && inputDate.length() >= 10) {
                date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(inputDate.substring(0, 10));
            }
            if (date != null) {
                return new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(date);
            }
        } catch (ParseException e) { }
        return inputDate;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshView();
    }
}
