package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.collection.ArrayMap;

import com.amazmod.service.R;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.ConversationActivity;
import com.amazmod.service.ui.view.NotificationCardView;
import com.amazmod.service.ui.view.ReplyPillView;
import com.amazmod.service.util.DeviceUtil;

import amazmod.com.transport.data.NotificationData;

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
        List<String> allStoreKeys = NotificationStore.getStoreKeys();
        for (String storeKey : allStoreKeys) {
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
        return new NotificationCardView(mContext, info, cardListener);
    }

    private final NotificationCardView.Listener cardListener = new NotificationCardView.Listener() {
        @Override
        public void onCardClick(NotificationInfo info) {
            openConversation(info);
        }

        @Override
        public void onCardLongClick(NotificationInfo info) {
            deleteNotification(info, true);
        }

        @Override
        public void onCardSwiped(NotificationInfo info) {
            deleteNotification(info, false);
        }
    };

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
        ReplyPillView item = new ReplyPillView(ctx);
        item.setText(text);
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
