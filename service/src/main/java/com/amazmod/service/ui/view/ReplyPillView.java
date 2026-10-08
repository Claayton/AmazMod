package com.amazmod.service.ui.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.amazmod.service.R;
import com.amazmod.service.util.SafeArea;

/**
 * A rounded, tappable pill used for quick replies and menu options.
 * Centralizes the pill style (background, height, padding, text).
 */
public class ReplyPillView extends TextView {

    public ReplyPillView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setTextColor(getResources().getColor(R.color.amz_text));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        setBackgroundResource(R.drawable.bg_reply_pill);
        setGravity(Gravity.CENTER);
        setMinHeight(SafeArea.dp(getContext(), 44));
        int h = SafeArea.dp(getContext(), 12);
        int v = SafeArea.dp(getContext(), 10);
        setPadding(h, v, h, v);
    }
}
