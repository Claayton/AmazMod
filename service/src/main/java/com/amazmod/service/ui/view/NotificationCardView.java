package com.amazmod.service.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.amazmod.service.R;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.util.SafeArea;

import org.tinylog.Logger;

/**
 * A single conversation card in the notifications list: icon, title, preview,
 * time and unread badge, plus the tap / long-press / swipe gestures.
 */
public class NotificationCardView extends LinearLayout {

    private static final int SWIPE_THRESHOLD = 60;
    private static final int DELETE_REVEAL = 90;

    public interface Listener {
        void onCardClick(NotificationInfo info);

        void onCardLongClick(NotificationInfo info);

        void onCardSwiped(NotificationInfo info);
    }

    private final NotificationInfo info;

    public NotificationCardView(Context context, NotificationInfo info, final Listener listener) {
        super(context);
        this.info = info;

        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.bg_bubble);
        int pad = SafeArea.dp(context, 10);
        setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = SafeArea.dp(context, 4);
        cardParams.bottomMargin = SafeArea.dp(context, 4);
        setLayoutParams(cardParams);

        ImageView icon = new ImageView(context);
        icon.setLayoutParams(new LinearLayout.LayoutParams(
                SafeArea.dp(context, 40), SafeArea.dp(context, 40)));
        setCardIcon(icon);
        addView(icon);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsParams.leftMargin = SafeArea.dp(context, 10);
        textsParams.rightMargin = SafeArea.dp(context, 8);
        addView(texts, textsParams);

        TextView title = new TextView(context);
        title.setText(info.getNotificationTitle());
        title.setTextColor(context.getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);

        TextView preview = new TextView(context);
        preview.setText(info.getNotificationText());
        preview.setTextColor(context.getResources().getColor(R.color.amz_text_secondary));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewParams.topMargin = SafeArea.dp(context, 2);
        texts.addView(preview, previewParams);

        LinearLayout meta = new LinearLayout(context);
        meta.setOrientation(VERTICAL);
        meta.setGravity(Gravity.CENTER);
        addView(meta);

        TextView time = new TextView(context);
        time.setText(info.getNotificationTime());
        time.setTextColor(context.getResources().getColor(R.color.amz_text_secondary));
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        time.setGravity(Gravity.CENTER);
        meta.addView(time);

        if (info.getMessageCount() > 1) {
            TextView badge = new TextView(context);
            badge.setText(String.valueOf(info.getMessageCount()));
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.bg_badge);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    SafeArea.dp(context, 22), SafeArea.dp(context, 22));
            badgeParams.topMargin = SafeArea.dp(context, 4);
            badgeParams.gravity = Gravity.CENTER;
            meta.addView(badge, badgeParams);
        }

        attachGestures(listener);
    }

    private void setCardIcon(ImageView iconView) {
        try {
            byte[] largeIconData = info.getLargeIconData();
            if (largeIconData != null && largeIconData.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(largeIconData, 0, largeIconData.length);
                if (bitmap != null) {
                    RoundedBitmapDrawable rounded = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
                    rounded.setCircular(true);
                    rounded.setAntiAlias(true);
                    iconView.setImageDrawable(rounded);
                    return;
                }
            }
            Drawable drawable = info.getIcon();
            if (drawable != null)
                iconView.setImageDrawable(drawable);
            else
                iconView.setImageResource(R.drawable.amazmod);
        } catch (Exception e) {
            Logger.error(e, "setCardIcon: {}", e.getMessage());
        }
    }

    private void attachGestures(final Listener listener) {
        setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private boolean swiping;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        swiping = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - downX;
                        float dy = event.getY() - downY;
                        if (!swiping && dx < -20 && Math.abs(dx) > Math.abs(dy))
                            swiping = true;
                        if (swiping)
                            v.setTranslationX(Math.max(dx, -DELETE_REVEAL));
                        return true;
                    case MotionEvent.ACTION_UP:
                        v.animate().translationX(0).setDuration(120).start();
                        if (swiping) {
                            float total = event.getX() - downX;
                            if (total < -SWIPE_THRESHOLD)
                                listener.onCardSwiped(info);
                        } else {
                            float moved = Math.abs(event.getX() - downX) + Math.abs(event.getY() - downY);
                            if (moved < 20)
                                listener.onCardClick(info);
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().translationX(0).setDuration(120).start();
                        return true;
                    default:
                        return false;
                }
            }
        });
        setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onCardLongClick(info);
                return true;
            }
        });
    }
}
