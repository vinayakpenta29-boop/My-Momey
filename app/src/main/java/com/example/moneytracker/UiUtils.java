package com.example.moneytracker;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

public class UiUtils {

    public static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static void addDividerWithMargin(Context ctx, LinearLayout layout, int thicknessDp) {
        View line = new View(ctx);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(ctx, thicknessDp));
        int pxMargin = dpToPx(ctx, 4);
        params.setMargins(pxMargin, 0, pxMargin, 0);
        line.setLayoutParams(params);
        line.setBackgroundColor(0xFFD1D1D1);
        layout.addView(line);
    }
}
