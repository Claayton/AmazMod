package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.support.NotificationStore;

import amazmod.com.transport.data.NotificationData;

/**
 * Development-only screen: seeds fake notifications and opens the notifications list,
 * so the UI can be iterated on the device without waiting for real notifications.
 *
 * Start with: adb shell am start -n com.amazmod.service/.ui.UIPreviewActivity
 */
public class UIPreviewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("clear", false)) {
            NotificationStore.clear();
            finish();
            return;
        }
        seed();
        Intent intent = new Intent(this, LauncherWearGridActivity.class);
        intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.NOTIFICATIONS);
        startActivity(intent);
        finish();
    }

    private void seed() {
        long now = System.currentTimeMillis();
        String chatAna = "0|com.whatsapp|1|preview-chat-ana|1000";
        add(chatAna, "Ana", "Oi! Vamos marcar aquele café amanhã à tarde?", "12:01", now - 1000L * 60 * 5);
        add(chatAna, "Ana", "Pode ser às 15h naquele lugar de sempre?", "12:02", now - 1000L * 60 * 4);
        add(chatAna, "Ana",
                "Trouxe uma mensagem bem longa pra testar a quebra de linha e a rolagem dentro do balão no relógio redondo.",
                "12:03", now - 1000L * 60 * 3);
        add("0|com.whatsapp|1|preview-chat-bruno|1000", "Bruno", "Segue o comprovante", "11:40",
                now - 1000L * 60 * 20);
        add("0|com.google.android.gm|1|preview-gm|1000", "Gmail",
                "Nova promoção na sua caixa de entrada", "10:15", now - 1000L * 60 * 60);
    }

    private void add(String conversationKey, String title, String text, String time, long timestamp) {
        NotificationData data = new NotificationData();
        data.setKey(conversationKey);
        data.setId(1);
        data.setTitle(title);
        data.setText(text);
        data.setTime(time);
        data.setVibration(0);
        int[] icon = new int[]{0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        data.setIcon(icon);
        data.setIconWidth(2);
        data.setIconHeight(2);
        NotificationStore.addCustomNotification(conversationKey + "|" + timestamp, data);
    }
}
