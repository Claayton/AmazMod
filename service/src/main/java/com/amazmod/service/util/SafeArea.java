package com.amazmod.service.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Utilities to keep content inside the usable area of a round watch screen.
 *
 * For a circular screen the horizontal room shrinks as an element moves away from the
 * vertical center, so the horizontal inset must be computed from the element's vertical
 * position using the circle equation.
 */
public final class SafeArea {

    private SafeArea() {
    }

    /**
     * Horizontal inset (px) needed so that a horizontal band centered at {@code centerY}
     * stays inside a circle of diameter {@code screenSize}, keeping {@code safetyDp} of margin.
     */
    public static int insetFor(Context context, int screenSize, int centerY, int safetyDp) {
        int radius = screenSize / 2;
        int margin = dp(context, safetyDp);
        int maxDy = radius - margin;
        if (maxDy < 1)
            maxDy = 1;
        int dy = Math.abs(centerY - radius);
        if (dy > maxDy)
            dy = maxDy;
        int halfChord = (int) Math.sqrt((double) (maxDy * maxDy - dy * dy));
        return Math.max(0, radius - halfChord);
    }

    /**
     * Applies a computed horizontal margin to {@code child}, based on its vertical position
     * inside {@code root}. Call after the view has been laid out (e.g. from root.post(...)).
     */
    public static void applyHorizontalInset(Context context, View root, View child, int safetyDp) {
        if (root == null || child == null)
            return;
        if (root.getWidth() <= 0 || child.getHeight() <= 0)
            return;
        if (!(child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams))
            return;

        int[] rootLocation = new int[2];
        int[] childLocation = new int[2];
        root.getLocationInWindow(rootLocation);
        child.getLocationInWindow(childLocation);

        int centerY = (childLocation[1] - rootLocation[1]) + child.getHeight() / 2;
        int inset = insetFor(context, root.getWidth(), centerY, safetyDp);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
        params.leftMargin = inset;
        params.rightMargin = inset;
        child.setLayoutParams(params);
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
