package com.amazmod.service.support;

import android.content.Context;

import androidx.collection.ArrayMap;

import com.amazmod.service.util.DeviceUtil;
import com.huami.watch.notification.data.NotificationKeyData;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import amazmod.com.transport.data.NotificationData;

public class NotificationStore {

    private static ArrayMap<String, NotificationData> customNotifications = new ArrayMap<>();
    public static ArrayMap<String, String> keyMap = new ArrayMap<>();
    private static ArrayMap<String, Long> timestamps = new ArrayMap<>();

    // Notifications older than this are removed automatically (1 hour)
    public static final long MAX_AGE = 60L * 60L * 1000L;

    public NotificationStore() {
        customNotifications = new ArrayMap<>();
        keyMap = new ArrayMap<>();
        timestamps = new ArrayMap<>();
    }

    public static NotificationData getCustomNotification(String key) {
        return customNotifications.get(key);
    }

    public static int getCustomNotificationCount() {
        return customNotifications.size();
    }

    public static void addCustomNotification(String key, NotificationData notificationData) {
        customNotifications.put(key, notificationData);
        keyMap.put(key, notificationData.getKey());
        timestamps.put(key, System.currentTimeMillis());
        purgeExpired();
    }

    // Removes notifications older than MAX_AGE (1 hour)
    public static void purgeExpired() {
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

    // Removes the stored notification(s) matching an original phone notification key
    public static void removeByNotificationKey(String originalKey) {
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

    public static void removeCustomNotification(String key, Context context) {
        NotificationData notificationData = NotificationStore.getCustomNotification(key);
        // Updates the notification counter only if del action is not send (NotificationData is null)
        if (notificationData == null)
            DeviceUtil.notificationCounter(context, -1,"NotificationWearActivity notification is null (del action will not be send)");
        else{
            // Remove custom notification
            sendRequestDeleteNotification(key, notificationData);
            customNotifications.remove(key);
            keyMap.remove(key);
        }
    }

    /*// Not used
    public static void removeCustomNotification(String key) {
        sendRequestDeleteNotification(key);
        customNotifications.remove(key);
        keyMap.remove(key);
    }
    */

    public static String getKey(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getKey();
    }

    public static Boolean getHideReplies(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return true;
        else
            return notificationData.getHideReplies();
    }

    public static Boolean getForceCustom(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return true;
        else
            return notificationData.getForceCustom();
    }

    public static int getTimeoutRelock(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return 0;
        else
            return notificationData.getTimeoutRelock();
    }

    public static String getTitle(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getTitle();
    }

    public static String getTime(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getTime();
    }

    public static int[] getIcon(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getIcon();
    }

    public static Set<String> getKeySet() {
        if (customNotifications != null)
            return customNotifications.keySet();
        else
            return null;
    }

    public static void clear() {
        if (!customNotifications.isEmpty()) {
            for (String key : customNotifications.keySet())
                sendRequestDeleteNotification(key);
            customNotifications.clear();
            keyMap.clear();
            timestamps.clear();
        }
    }

    public static void setNotificationCount(Context context) {
        setNotificationCount(context, getCustomNotificationCount());
    }

    public static void setNotificationCount(Context context, int count) {
        DeviceUtil.notificationCounterSet(context, count);
    }

    private static boolean isEmpty() {
        return getCustomNotificationCount() == 0;
    }

    // Send notification delete
    private static void sendRequestDeleteNotification(String key) {
        sendRequestDeleteNotification(key, customNotifications.get(key));
    }
    private static void sendRequestDeleteNotification(String key, NotificationData notificationData) {
        Logger.debug("NotificationStore sendRequestDeleteNotification key: {} ", key);

        if (notificationData == null)
            return;

        String pkg = key.split("\\|")[1];
        // Logger.debug("NotificationStore sendRequestDeleteNotification pkg: {} ", pkg);

        // NotificationKeyData from(String pkg, int id, String tag, String key, String targetPkg)
        NotificationKeyData notificationKeyData = NotificationKeyData.from(pkg, notificationData.getId(),null, notificationData.getKey(), null);
        EventBus.getDefault().post(notificationKeyData);
    }

}
