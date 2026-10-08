package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.collection.ArrayMap;

import com.amazmod.service.R;
import com.amazmod.service.adapters.NotificationListAdapter;
import com.amazmod.service.helper.RecyclerTouchListener;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.ConversationActivity;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SafeArea;

import amazmod.com.transport.data.NotificationData;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class WearNotificationsFragment extends Fragment {

    static WearNotificationsFragment instance = null;

    private BoxInsetLayout rootLayout;
    private RelativeLayout wearNotificationsFrameLayout;
	private WearableListView listView;
    private TextView mHeader;
    private ProgressBar progressBar;

    private Context mContext;

    private List<NotificationInfo> notificationInfoList;
    private NotificationListAdapter mAdapter;

    private static boolean animate = false;

    //private static final String REFRESH = "Refresh";
    //private static final String CLEAR = "Clear";
    public static final String ANIMATE = "animate";


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearNotificationsFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        animate = getArguments().getBoolean(ANIMATE);

        Logger.info("WearNotificationsFragment onCreate animate: {}", animate);
        instance = this;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearNotificationsFragment onCreateView");

        View view = inflater.inflate(R.layout.fragment_wear_notifications, container, false);

        if (animate)
            view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_from_right));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearNotificationsFragment onViewCreated");
        init();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onItemClick(int position) {

        Logger.info("WearNotificationsFragment onClick position: " + position);

        if (position >= 0 && position < notificationInfoList.size())
            showNotification(position);
    }

    public void onItemLongClick(int position) {

        Logger.info("WearNotificationsFragment onLongClick position: " + position);

        if (!getResources().getString(R.string.refresh).equals(notificationInfoList.get(position).getNotificationTitle()))
            deleteNotification(position, true);
    }

    private void init() {
        rootLayout = getActivity().findViewById(R.id.wear_notifications_main_layout);
        wearNotificationsFrameLayout = getActivity().findViewById(R.id.wear_notifications_frame_layout);
        listView = getActivity().findViewById(R.id.wear_notifications_list);
        mHeader = getActivity().findViewById(R.id.wear_notifications_header);
        progressBar = getActivity().findViewById(R.id.wear_notifications_loading_spinner);

        rootLayout.setBackgroundColor(mContext.getResources().getColor(R.color.black));

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);

        final RecyclerTouchListener touchListener = new RecyclerTouchListener(mContext, listView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Logger.debug("WearNotificationsFragment addOnItemTouchListener onClick");
                onItemClick(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Logger.debug("WearNotificationsFragment addOnItemTouchListener onLongClick");
                onItemLongClick(position);
            }
        });
        touchListener.setSwipeListener(new RecyclerTouchListener.SwipeListener() {
            @Override
            public void onSwipeLeft(View view, int position) {
                Logger.debug("WearNotificationsFragment onSwipeLeft position: " + position);
                deleteNotification(position, false);
            }
        });
        listView.addOnItemTouchListener(touchListener);

        mHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu();
            }
        });

        View overflow = getActivity().findViewById(R.id.wear_notifications_overflow);
        if (overflow != null) {
            overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMenu();
                }
            });
        }

        loadNotifications();
    }

    @SuppressLint("CheckResult")
    public void loadNotifications() {
        Logger.info("WearNotificationsFragment loadNotifications");

        //Return if there is no activity to avoid crashes
        if (getActivity() == null)
            return;

        wearNotificationsFrameLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        Flowable.fromCallable(new Callable<List<NotificationInfo>>() {
            @Override
            public List<NotificationInfo> call() {
                Logger.debug("WearNotificationsFragment loadNotifications call");

                NotificationStore.purgeExpired();

                List<NotificationInfo> notificationInfoList = new ArrayList<>();

                // Group notifications by conversation (original phone notification key)
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

                    // Representative = latest message in the conversation
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
                    if (data != null) {
                        Logger.debug("WearNotificationsFragment conversation {} with {} messages", convKey, storeKeys.size());
                        notificationInfoList.add(new NotificationInfo(data, latestStoreKey, convKey, storeKeys.size()));
                    }
                }

                sortNotifications(notificationInfoList);
                WearNotificationsFragment.this.notificationInfoList = notificationInfoList;
                return notificationInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<NotificationInfo>>() {
                    @Override
                    public void accept(final List<NotificationInfo> notificationInfoList) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Logger.debug("WearNotificationsFragment loadNotifications run");
                                mAdapter = new NotificationListAdapter(mContext, notificationInfoList);
                                if (notificationInfoList.isEmpty())
                                    mHeader.setText(mContext.getResources().getString(R.string.empty));
                                else
                                    mHeader.setText(mContext.getResources().getString(R.string.notifications));
                                listView.setAdapter(mAdapter);
                                listView.post(new Runnable() {
                                    public void run() {
                                        Logger.debug("WearNotificationsFragment loadNotifications scrollToTop");
                                        listView.smoothScrollToPosition(0);
                                    }
                                });
                                progressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }, throwable -> {
                    Logger.error("WearNotificationsFragment: Flowable: subscribeOn: " + throwable.getMessage());
                });
    }

    private void showNotification(final int itemChosen) {

        final NotificationInfo info = notificationInfoList.get(itemChosen);
        Logger.debug("WearNotificationsFragment showNotification conversationKey: " + info.getConversationKey());

        if (info.getConversationKey() != null) {
            // Open the conversation (all messages of the same chat)
            Intent intent = new Intent(mContext, ConversationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(ConversationActivity.KEY, info.getConversationKey());
            intent.putExtra(ConversationActivity.TITLE, info.getNotificationTitle());
            mContext.startActivity(intent);
            return;
        }

        final String key = info.getKey();

        Intent intent = new Intent(mContext, NotificationWearActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(NotificationWearActivity.KEY, key);
        intent.putExtra(NotificationWearActivity.MODE, NotificationWearActivity.MODE_VIEW);

        mContext.startActivity(intent);
    }

    private void deleteNotification(final int itemChosen, boolean confirm) {

        if (itemChosen < 0 || itemChosen >= notificationInfoList.size())
            return;

        final NotificationInfo info = notificationInfoList.get(itemChosen);
        final String title = info.getNotificationTitle();
        if (getResources().getString(R.string.refresh).equals(title) || getResources().getString(R.string.clear).equals(title))
            return;

        Logger.debug("WearNotificationsFragment deleteNotification title: " + title);

        if (!confirm) {
            removeNotification(info);
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(mContext.getResources().getString(R.string.delete))
                .setMessage(mContext.getResources().getString(R.string.confirmation))
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
        else
            NotificationStore.removeCustomNotification(info.getKey(), mContext);
        NotificationStore.setNotificationCount(mContext);
        loadNotifications();
    }

    // The following code ensures that the title scrolls as the user scrolls up
    // or down the list
    private WearableListView.OnScrollListener mOnScrollListener =
            new WearableListView.OnScrollListener() {
                @Override
                public void onAbsoluteScrollChange(int i) {
                    // Only scroll the title up from its original base position
                    // and not down.
                    if (i > 0) {
                        mHeader.setY(-i);
                    }
                }

                @Override
                public void onScroll(int i) {
                    // Placeholder
                }

                @Override
                public void onScrollStateChanged(int i) {
                    // Placeholder
                }

                @Override
                public void onCentralPositionChanged(int i) {
                    // Placeholder
                }
            };

    private void sortNotifications(List<NotificationInfo> notificationInfoList) {
        Collections.sort(notificationInfoList, new Comparator<NotificationInfo>() {
            @Override
            public int compare(NotificationInfo o1, NotificationInfo o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
    }

    private void resetNotificationsCounter() {
        DeviceUtil.notificationCounterSet(mContext, 0);
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
        title.setText(ctx.getString(R.string.notifications));
        title.setTextColor(ctx.getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dp(10);
        layout.addView(title, titleParams);

        layout.addView(menuButton(ctx, "Atualizar", new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                reloadList();
            }
        }));

        layout.addView(menuButton(ctx, "Limpar tudo", new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                clearAll();
            }
        }));

        TextView cancel = new TextView(ctx);
        cancel.setText("Cancelar");
        cancel.setTextColor(ctx.getResources().getColor(R.color.amz_text_secondary));
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
        item.setTextColor(ctx.getResources().getColor(R.color.amz_text));
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

    private void reloadList() {
        notificationInfoList.clear();
        if (mAdapter != null)
            mAdapter.clear();
        loadNotifications();
    }

    private void clearAll() {
        new AlertDialog.Builder(getActivity())
                .setTitle(mContext.getResources().getString(R.string.clear_notifications))
                .setMessage(mContext.getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        NotificationStore.clear();
                        resetNotificationsCounter();
                        loadNotifications();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private int dp(int value) {
        return (int) (value * mContext.getResources().getDisplayMetrics().density);
    }

    public static WearNotificationsFragment newInstance(boolean animate) {
        Logger.info("WearNotificationsFragment newInstance animate: {}", animate);

        WearNotificationsFragment myFragment = new WearNotificationsFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ANIMATE, animate);
        myFragment.setArguments(bundle);

        return myFragment;
    }

    public static WearNotificationsFragment getInstance() {
        return instance;
    }
}