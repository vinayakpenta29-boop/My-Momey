package com.example.moneytracker;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.moneytracker.EmiStore.EmiScheme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class EmiUiHelper {

    public interface OnEmiAddedListener {
        void onEmiAdded();
    }

    private static int dpToPx(Fragment fragment, int dp) {
        float density = fragment.requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Optional separate EMI menu (you already use BC menu that calls EmiUiHelper)
    public static void showEmiManagerMenu(Fragment fragment, View anchor) {
        android.widget.PopupMenu menu =
                new android.widget.PopupMenu(fragment.getContext(), anchor);
        menu.getMenu().add("Add EMI");
        menu.getMenu().add("View EMI List");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Add EMI".equals(title)) {
                showAddEmiDialog(fragment, null);
            } else if ("View EMI List".equals(title)) {
                showEmiListDialog(fragment);
            }
            return true;
        });
        menu.show();
    }

    // Add EMI (with installment options, same as BC)
    public static void showAddEmiDialog(Fragment fragment, OnEmiAddedListener listener) {
        Context ctx = fragment.requireContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(fragment, 16);
        root.setPadding(pad, pad, pad, pad);

        EditText etAccount = new EditText(ctx);
        etAccount.setHint("Account Name (optional)");
        root.addView(etAccount);

        EditText etEmiName = new EditText(ctx);
        etEmiName.setHint("EMI Name");
        root.addView(etEmiName);

        EditText etMonths = new EditText(ctx);
        etMonths.setHint("Months");
        etMonths.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(etMonths);

        EditText etStartDate = new EditText(ctx);
        etStartDate.setHint("Start / Due Date (dd/MM/yyyy)");
        etStartDate.setFocusable(false);
        etStartDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(
                    ctx,
                    (DatePicker view, int year, int month, int dayOfMonth) -> {
                        String d = String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etStartDate.setText(d);
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });
        root.addView(etStartDate);

        // Installment label
        TextView instLabel = new TextView(ctx);
        instLabel.setText("Installment Option");
        instLabel.setPadding(0, dpToPx(fragment, 8), 0, dpToPx(fragment, 4));
        root.addView(instLabel);

        // Two buttons
        LinearLayout instLayout = new LinearLayout(ctx);
        instLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnFixed = new Button(ctx);
        btnFixed.setText("Fixed Amount Installment");
        instLayout.addView(btnFixed);

        Button btnRandom = new Button(ctx);
        btnRandom.setText("Random Amount Installment");
        instLayout.addView(btnRandom);

        root.addView(instLayout);

        final String[] instType = new String[] { "NONE" };  // FIXED / RANDOM / NONE
        final int[] fixedAmountHolder = new int[] { 0 };
        final List<Integer> randomAmountsHolder = new ArrayList<>();

        // Fixed dialog
        btnFixed.setOnClickListener(v2 -> {
            final EditText input = new EditText(ctx);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Installment amount");

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("Fixed Amount Installment")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        String amtStr = input.getText().toString().trim();
                        try {
                            int val = Integer.parseInt(amtStr);
                            instType[0] = "FIXED";
                            fixedAmountHolder[0] = val;
                            randomAmountsHolder.clear();
                            Toast.makeText(ctx, "Fixed installment set: ₹" + val,
                                    Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(ctx, "Invalid amount", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        });

        // Random dialog
        btnRandom.setOnClickListener(v2 -> {
            String monthsStr = etMonths.getText().toString().trim();
            int m;
            try {
                m = Integer.parseInt(monthsStr);
            } catch (NumberFormatException e) {
                Toast.makeText(ctx, "Enter Months first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (m <= 0) {
                Toast.makeText(ctx, "Months must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            ScrollView scroll = new ScrollView(ctx);
            LinearLayout listLayout = new LinearLayout(ctx);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(listLayout);

            EditText[] amountInputs = new EditText[m];
            for (int i = 0; i < m; i++) {
                EditText et = new EditText(ctx);
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                et.setHint((i + 1) + " installment amount");
                listLayout.addView(et);
                amountInputs[i] = et;
            }

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("Random Amount Installments")
                    .setView(scroll)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (d, w) -> {
                        List<Integer> list = new ArrayList<>();
                        for (int i = 0; i < m; i++) {
                            String s = amountInputs[i].getText().toString().trim();
                            try {
                                list.add(Integer.parseInt(s));
                            } catch (NumberFormatException e) {
                                Toast.makeText(ctx,
                                        "Invalid amount at position " + (i + 1),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        instType[0] = "RANDOM";
                        fixedAmountHolder[0] = 0;
                        randomAmountsHolder.clear();
                        randomAmountsHolder.addAll(list);
                        Toast.makeText(ctx, "Random installments set",
                                Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("Add EMI Scheme")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String accountName = etAccount.getText().toString().trim();
                    String emiName = etEmiName.getText().toString().trim();
                    String monthsStr = etMonths.getText().toString().trim();
                    String startDate = etStartDate.getText().toString().trim();

                    if (TextUtils.isEmpty(emiName) ||
                            TextUtils.isEmpty(monthsStr) ||
                            TextUtils.isEmpty(startDate)) {
                        Toast.makeText(ctx,
                                "EMI Name, Months and Start Date are required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int months;
                    try {
                        months = Integer.parseInt(monthsStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "Invalid months", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    EmiScheme scheme = new EmiScheme();
                    scheme.name = emiName;
                    scheme.months = months;
                    scheme.startDate = startDate;
                    generateEmiSchedule(scheme);

                    scheme.installmentType = instType[0];
                    if ("FIXED".equals(instType[0])) {
                        scheme.fixedAmount = fixedAmountHolder[0];
                        scheme.monthlyAmounts.clear();
                    } else if ("RANDOM".equals(instType[0])) {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                        scheme.monthlyAmounts.addAll(randomAmountsHolder);
                    } else {
                        scheme.fixedAmount = 0;
                        scheme.monthlyAmounts.clear();
                    }

                    String key = TextUtils.isEmpty(accountName) ? "_GLOBAL_" : accountName;
                    EmiStore.addScheme(key, scheme);
                    EmiStore.save(ctx);

                    if (listener != null) listener.onEmiAdded();
                    Toast.makeText(ctx, "EMI added", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // Generate monthly schedule for EMI
    public static void generateEmiSchedule(EmiScheme scheme) {
        scheme.scheduleDates.clear();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(df.parse(scheme.startDate));
        } catch (ParseException e) {
            return;
        }
        for (int i = 0; i < scheme.months; i++) {
            scheme.scheduleDates.add(df.format(c.getTime()));
            c.add(Calendar.MONTH, 1);
        }
    }

    // List EMI schemes + open details
    public static void showEmiListDialog(Fragment fragment) {
        Context ctx = fragment.requireContext();
        HashMap<String, ArrayList<EmiScheme>> emiMap = EmiStore.getEmiMap();

        if (emiMap.isEmpty()) {
            Toast.makeText(ctx, "No EMI schemes added", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<EmiScheme> schemes = new ArrayList<>();
        for (String key : emiMap.keySet()) {
            ArrayList<EmiScheme> list = emiMap.get(key);
            if (list == null) continue;
            for (EmiScheme s : list) {
                String label = (key.equals("_GLOBAL_") ? "" : key + " - ") + s.name;
                labels.add(label);
                schemes.add(s);
            }
        }
        if (labels.isEmpty()) {
            Toast.makeText(ctx, "No EMI schemes added", Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("EMI Schemes")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    EmiScheme s = schemes.get(which);
                    showEmiDetailsDialog(fragment, s);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // Detail dialog with dates + amounts + auto-tick using paidCount
    public static void showEmiDetailsDialog(Fragment fragment, EmiScheme scheme) {
        Context ctx = fragment.requireContext();
        ScrollView scrollView = new ScrollView(ctx);
        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(fragment, 16), dpToPx(fragment, 8),
                dpToPx(fragment, 16), dpToPx(fragment, 8));
        scrollView.addView(container);

        for (int i = 0; i < scheme.scheduleDates.size(); i++) {
            String date = scheme.scheduleDates.get(i);
            String amountText = "";

            if ("FIXED".equals(scheme.installmentType)) {
                amountText = "  ₹" + scheme.fixedAmount;
            } else if ("RANDOM".equals(scheme.installmentType)
                    && i < scheme.monthlyAmounts.size()) {
                amountText = "  ₹" + scheme.monthlyAmounts.get(i);
            }

            boolean done = i < scheme.paidCount;          // first paidCount installments ticked
            String prefix = done ? "✅ " : "☐ ";

            TextView tv = new TextView(ctx);
            tv.setText(prefix + date + amountText);
            if (done) {
                tv.setTextColor(0xFF2E7D32);              // green text for paid
            }
            tv.setTextSize(14);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setPadding(0, dpToPx(fragment, 4), 0, dpToPx(fragment, 4));
            container.addView(tv);
        }

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("EMI: " + scheme.name)
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
    }
}
