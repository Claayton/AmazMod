package com.amazmod.service.support;

import android.content.Context;

import androidx.collection.ArrayMap;

import com.amazmod.service.util.DeviceUtil;
import com.huami.watch.notification.data.NotificationKeyData;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amazmod.com.transport.data.NotificationData;

/**
 * In-memory store of custom notifications, keyed by {@code phoneKey|timestamp}.
 *
 * <p>It is written from the transport thread ({@code NotificationService}) and read from the
 * UI thread, so all access is synchronized and snapshots are returned to callers that iterate.
 */
public class NotificationStore {

    // Notifications older than this are removed automatically (1 hour)
    public static final long MAX_AGE = 60L * 60L * 1000L;

    private static final ArrayMap<String, NotificationData> customNotifications = new ArrayMap<>();
    private static final ArrayMap<String, String> keyMap = new ArrayMap<>();
    private static final ArrayMap<String, Long> timestamps = new ArrayMap<>();

    public static synchronized NotificationData getCustomNotification(String key) {
        return customNotifications.get(key);
    }

    public static synchronized int getCustomNotificationCount() {
        return customNotifications.size();
    }

    public static synchronized void addCustomNotification(String key, NotificationData notificationData) {
        customNotifications.put(key, notificationData);
        keyMap.put(key, notificationData.getKey());
        timestamps.put(key, System.currentTimeMillis());
        purgeExpiredLocked();
    }

    /** Removes notifications older than {@link #MAX_AGE} (1 hour). */
    public static synchronized void purgeExpired() {
        purgeExpiredLocked();
    }

    private static void purgeExpiredLocked() {
        if (timestamps.isEmpty())
            return;
        final long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            Long t = timestamps.valueAt(i);
            if (t == null || (now - t) > MAX_AGE)
                expired.add(timestamps.keyAt(i));
        }
        for (String key : expired) {
            customNotifications.remove(key);
            keyMap.remove(key);
            timestamps.remove(key);
            Logger.debug("NotificationStore purgeExpired removed {}", key);
        }
    }

    /** Removes the stored notification(s) matching an original phone notification key. */
    public static synchronized void removeByNotificationKey(String originalKey) {
        if (originalKey == null)
            return;
        List<String> toRemove = new ArrayList<>();
        for (int i = 0; i < keyMap.size(); i++) {
            if (originalKey.equals(keyMap.valueAt(i)))
                toRemove.add(keyMap.keyAt(i));
        }
        for (String key : toRemove) {
            customNotifications.remove(key);
            keyMap.remove(key);
            timestamps.remove(key);
        }
    }

    /** Store keys of a conversation (same phone key), sorted oldest -> newest. */
    public static synchronized List<String> getConversationStoreKeys(String originalKey) {
        List<String> keys = new ArrayList<>();
        if (originalKey == null)
            return keys;
        for (int i = 0; i < keyMap.size(); i++) {
            if (originalKey.equals(keyMap.valueAt(i)))
                keys.add(keyMap.keyAt(i));
        }
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Long.compare(parseTimestamp(a), parseTimestamp(b));
            }
        });
        return keys;
    }

    /** Snapshot of all store keys (safe to iterate outside the store lock). */
    public static synchronized List<String> getStoreKeys() {
        return new ArrayList<>(customNotifications.keySet());
    }

    public static synchronized String getKey(String key) {
        NotificationData notificationData = customNotifications.get(key);
        return notificationData == null ? null : notificationData.getKey();
    }

    public static synchronized Boolean getHideReplies(String key) {
        NotificationData notificationData = customNotifications.get(key);
        return notificationData == null ? Boolean.TRUE : notificationData.getHideReplies();
    }

    public static synchronized Boolean getForceCustom(String key) {
        NotificationData notificationData = customNotifications.get(key);
        return notificationData == null ? Boolean.TRUE : notificationData.getForceCustom();
    }

    public static synchronized int getTimeoutRelock(String key) {
        NotificationData notificationData = customNotifications.get(key);
        return notificationData == null ? 0 : notificationData.getTimeoutRelock();
    }

    public static synchronized void removeCustomNotification(String key, Context context) {
        NotificationData notificationData = customNotifications.get(key);
        // Updates the notification counter only if del action is not sent (NotificationData is null)
        if (notificationData == null) {
            DeviceUtil.notificationCounter(context, -1,
                    "NotificationWearActivity notification is null (del action will not be send)");
        } else {
            sendRequestDeleteNotification(key, notificationData);
            removeLocked(key);
        }
    }

    public static synchronized void clear() {
        if (!customNotifications.isEmpty()) {
            for (String key : customNotifications.keySet())
                sendRequestDeleteNotification(key);
            customNotifications.clear();
            keyMap.clear();
            timestamps.clear();
        }
    }

    private static void removeLocked(String key) {
        customNotifications.remove(key);
        keyMap.remove(key);
        timestamps.remove(key);
    }

    public static void setNotificationCount(Context context) {
        setNotificationCount(context, getCustomNotificationCount());
    }

    public static void setNotificationCount(Context context, int count) {
        DeviceUtil.notificationCounterSet(context, count);
    }

    private static long parseTimestamp(String storeKey) {
        try {
            return Long.parseLong(storeKey.substring(storeKey.lastIndexOf("|") + 1));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void sendRequestDeleteNotification(String key) {
        sendRequestDeleteNotification(key, customNotifications.get(key));
    }

    private static void sendRequestDeleteNotification(String key, NotificationData notificationData) {
        Logger.debug("NotificationStore sendRequestDeleteNotification key: {} ", key);
        if (notificationData == null)
            return;
        String pkg = key.split("\\|")[1];
        NotificationKeyData notificationKeyData = NotificationKeyData.from(pkg, notificationData.getId(),
                null, notificationData.getKey(), null);
        EventBus.getDefault().post(notificationKeyData);
    }
}
