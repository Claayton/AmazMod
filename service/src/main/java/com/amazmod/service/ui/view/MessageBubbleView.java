package com.amazmod.service.ui.view;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import com.amazmod.service.R;
import com.amazmod.service.util.SafeArea;

/**
 * A message bubble used in the conversation screen.
 * Keeps the visual style centralized (background, padding, max width, text style).
 */
public class MessageBubbleView extends TextView {

    private static final int MAX_WIDTH_DP = 205;

    public MessageBubbleView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setTextColor(getResources().getColor(R.color.amz_text));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        setBackgroundResource(R.drawable.bg_bubble);
        int h = SafeArea.dp(getContext(), 14);
        int v = SafeArea.dp(getContext(), 10);
        setPadding(h, v, h, v);
        setMaxWidth(SafeArea.dp(getContext(), MAX_WIDTH_DP));
    }
}
