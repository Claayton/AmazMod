package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.collection.ArrayMap;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.amazmod.service.R;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.ConversationActivity;
import com.amazmod.service.util.DeviceUtil;

import amazmod.com.transport.data.NotificationData;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Notifications screen built as a single continuous scroll surface:
 * the title and the conversation cards flow together (no fixed header overlay).
 */
public class WearNotificationsFragment extends Fragment {

    static WearNotificationsFragment instance = null;

    private static final int SWIPE_THRESHOLD = 60;
    private static final int DELETE_REVEAL = 90;

    private Context mContext;
    private ScrollView scroll;
    private LinearLayout content;

    private List<NotificationInfo> notificationInfoList = new ArrayList<>();
    private boolean animate = false;

    public static WearNotificationsFragment newInstance(boolean animate) {
        WearNotificationsFragment fragment = new WearNotificationsFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("animate", animate);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static WearNotificationsFragment getInstance() {
        return instance;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        instance = this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            animate = getArguments().getBoolean("animate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        scroll = new ScrollView(mContext);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getResources().getColor(R.color.amz_bg));

        content = new LinearLayout(mContext);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        content.setPadding(pad, dp(40), pad, dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return scroll;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void loadNotifications() {
        if (getActivity() == null || content == null)
            return;

        notificationInfoList = buildConversations();
        render();
    }

    private List<NotificationInfo> buildConversations() {
        NotificationStore.purgeExpired();

        List<NotificationInfo> list = new ArrayList<>();

        ArrayMap<String, List<String>> conversations = new ArrayMap<>();
        if (NotificationStore.getKeySet() != null) {
            for (String storeKey : NotificationStore.getKeySet()) {
                String convKey = NotificationStore.getKey(storeKey);
                if (convKey == null)
                    convKey = storeKey;
                List<String> keys = conversations.get(convKey);
                if (keys == null) {
                    keys = new ArrayList<>();
                    conversations.put(convKey, keys);
                }
                keys.add(storeKey);
            }
        }

        for (int i = 0; i < conversations.size(); i++) {
            String convKey = conversations.keyAt(i);
            List<String> storeKeys = conversations.valueAt(i);

            String latestStoreKey = null;
            long latest = Long.MIN_VALUE;
            for (String storeKey : storeKeys) {
                long ts;
                try {
                    ts = Long.parseLong(storeKey.substring(storeKey.lastIndexOf("|") + 1));
                } catch (NumberFormatException e) {
                    ts = 0L;
                }
                if (ts > latest) {
                    latest = ts;
                    latestStoreKey = storeKey;
                }
            }

            NotificationData data = NotificationStore.getCustomNotification(latestStoreKey);
            if (data != null)
                list.add(new NotificationInfo(data, latestStoreKey, convKey, storeKeys.size()));
        }

        Collections.sort(list, new Comparator<NotificationInfo>() {
            @Override
            public int compare(NotificationInfo a, NotificationInfo b) {
                return b.getId().compareTo(a.getId());
            }
        });

        return list;
    }

    private void render() {
        content.removeAllViews();

        addHeaderRow();

        if (notificationInfoList.isEmpty()) {
            TextView empty = new TextView(mContext);
            empty.setText(getResources().getString(R.string.empty));
            empty.setTextColor(getResources().getColor(R.color.amz_text_secondary));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            empty.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(30);
            content.addView(empty, params);
            return;
        }

        for (NotificationInfo info : notificationInfoList)
            content.addView(buildCard(info));
    }

    private void addHeaderRow() {
        LinearLayout header = new LinearLayout(mContext);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp(10);

        TextView title = new TextView(mContext);
        title.setText(getResources().getString(R.string.notifications));
        title.setTextColor(getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        ImageView overflow = new ImageView(mContext);
        overflow.setImageResource(R.drawable.ic_more_vert);
        overflow.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams overflowParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        overflow.setLayoutParams(overflowParams);
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        header.addView(overflow);

        content.addView(header, headerParams);
    }

    private View buildCard(final NotificationInfo info) {
        LinearLayout card = new LinearLayout(mContext);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_bubble);
        int pad = dp(10);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(4);
        cardParams.bottomMargin = dp(4);
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(mContext);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        icon.setLayoutParams(iconParams);
        setCardIcon(icon, info);
        card.addView(icon);

        LinearLayout texts = new LinearLayout(mContext);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textsParams.leftMargin = dp(10);
        textsParams.rightMargin = dp(8);
        card.addView(texts, textsParams);

        TextView title = new TextView(mContext);
        title.setText(info.getNotificationTitle());
        title.setTextColor(getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(title);

        TextView preview = new TextView(mContext);
        preview.setText(info.getNotificationText());
        preview.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        preview.setSingleLine(true);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewParams.topMargin = dp(2);
        texts.addView(preview, previewParams);

        LinearLayout meta = new LinearLayout(mContext);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setGravity(Gravity.CENTER);
        card.addView(meta);

        TextView time = new TextView(mContext);
        time.setText(info.getNotificationTime());
        time.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        time.setGravity(Gravity.CENTER);
        meta.addView(time);

        if (info.getMessageCount() > 1) {
            TextView badge = new TextView(mContext);
            badge.setText(String.valueOf(info.getMessageCount()));
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundResource(R.drawable.bg_badge);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            badgeParams.topMargin = dp(4);
            badgeParams.gravity = Gravity.CENTER;
            meta.addView(badge, badgeParams);
        }

        attachGestures(card, info);
        return card;
    }

    private void setCardIcon(ImageView iconView, final NotificationInfo info) {
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

    private void attachGestures(final LinearLayout card, final NotificationInfo info) {
        card.setOnTouchListener(new View.OnTouchListener() {
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
                                deleteNotification(info, false);
                        } else {
                            float moved = Math.abs(event.getX() - downX) + Math.abs(event.getY() - downY);
                            if (moved < 20)
                                openConversation(info);
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
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                deleteNotification(info, true);
                return true;
            }
        });
    }

    private void openConversation(NotificationInfo info) {
        if (info.getConversationKey() == null)
            return;
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(ConversationActivity.KEY, info.getConversationKey());
        intent.putExtra(ConversationActivity.TITLE, info.getNotificationTitle());
        startActivity(intent);
    }

    private void deleteNotification(final NotificationInfo info, boolean confirm) {
        if (!confirm) {
            removeNotification(info);
            return;
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.delete))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeNotification(info);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void removeNotification(NotificationInfo info) {
        if (info.getConversationKey() != null)
            NotificationStore.removeByNotificationKey(info.getConversationKey());
        NotificationStore.setNotificationCount(mContext);
        loadNotifications();
    }

    private void showMenu() {
        final Context ctx = getActivity();
        if (ctx == null)
            return;
        final Dialog dialog = new Dialog(ctx);

        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_overlay_panel);
        int pad = dp(16);
        layout.setPadding(pad, dp(18), pad, dp(12));

        TextView title = new TextView(ctx);
        title.setText(getResources().getString(R.string.notifications));
        title.setTextColor(getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(10);
        layout.addView(title, titleParams);

        layout.addView(menuButton(ctx, getString(R.string.amz_refresh), new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                loadNotifications();
            }
        }));

        layout.addView(menuButton(ctx, getString(R.string.amz_clear_all), new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                clearAll();
            }
        }));

        TextView cancel = new TextView(ctx);
        cancel.setText(getString(R.string.amz_cancel));
        cancel.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(dp(12), dp(12), dp(12), dp(4));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        layout.addView(cancel);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private View menuButton(Context ctx, String text, View.OnClickListener listener) {
        TextView item = new TextView(ctx);
        item.setText(text);
        item.setTextColor(getResources().getColor(R.color.amz_text));
        item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        item.setBackgroundResource(R.drawable.bg_reply_pill);
        item.setGravity(Gravity.CENTER);
        item.setMinHeight(dp(44));
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        item.setLayoutParams(params);
        item.setOnClickListener(listener);
        return item;
    }

    private void clearAll() {
        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.clear_notifications))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        NotificationStore.clear();
                        DeviceUtil.notificationCounterSet(mContext, 0);
                        loadNotifications();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private int dp(int value) {
        return (int) (value * mContext.getResources().getDisplayMetrics().density);
    }
}
