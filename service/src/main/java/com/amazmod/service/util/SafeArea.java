package com.amazmod.service.util;

import android.content.Context;

/**
 * Helpers to keep content inside the usable area of a round watch screen.
 *
 * On a circular screen the horizontal room shrinks as an element moves away from the
 * vertical center, so the horizontal inset must be computed from the element's vertical
 * position using the circle equation.
 */
public final class SafeArea {

    private SafeArea() {
    }

    /**
     * Horizontal inset (px) so a horizontal band centered at {@code centerY} stays inside a
     * circle of diameter {@code screenSize}, keeping {@code safetyDp} of margin.
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

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
