package com.amazmod.service.support;

import com.amazmod.service.events.ReplyNotificationEvent;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

/**
 * Single entry point for delivering a quick reply to the phone.
 * Both the arrival screen ({@code NotificationFragment}) and the conversation
 * screen ({@code ConversationActivity}) send replies through here, while
 * {@code MainService} consumes the event and clears the local notification.
 */
public final class ReplyHelper {

    private ReplyHelper() {
    }

    public static void sendReply(String conversationKey, String value) {
        if (conversationKey == null) {
            Logger.error("cannot reply null key");
            return;
        }
        EventBus.getDefault().post(new ReplyNotificationEvent(conversationKey, value));
    }
}
