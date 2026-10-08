package com.amazmod.service.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.R;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.support.ReplyHelper;
import com.amazmod.service.ui.view.MessageBubbleView;
import com.amazmod.service.ui.view.ReplyPillView;
import com.amazmod.service.util.FragmentUtil;
import com.amazmod.service.util.SafeArea;

import org.tinylog.Logger;

import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;

/**
 * Conversation screen with a single continuous scroll surface:
 * header, messages and the reply button all move together.
 */
public class ConversationActivity extends Activity {

    public static final String KEY = "conversation_key";
    public static final String TITLE = "conversation_title";

    private String conversationKey;
    private FragmentUtil util;

    private FrameLayout root;
    private ScrollView contentScroll;
    private ScrollView replyOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversationKey = getIntent().getStringExtra(KEY);
        String title = getIntent().getStringExtra(TITLE);
        if (conversationKey == null) {
            finish();
            return;
        }

        util = new FragmentUtil(this);

        root = new FrameLayout(this);
        root.setBackgroundColor(getResources().getColor(R.color.amz_bg));

        contentScroll = buildContent(title);
        root.addView(contentScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Start at the most recent message
        contentScroll.post(new Runnable() {
            @Override
            public void run() {
                contentScroll.fullScroll(View.FOCUS_DOWN);
            }
        });

        Logger.debug("ConversationActivity onCreate key: {}", conversationKey);
    }

    private ScrollView buildContent(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = SafeArea.dp(this, 14);
        content.setPadding(pad, SafeArea.dp(this, 38), pad, SafeArea.dp(this, 24));
        scroll.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView header = new TextView(this);
        header.setText(title != null ? title : getString(R.string.amz_conversation));
        header.setTextColor(getResources().getColor(R.color.amz_text));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setSingleLine(true);
        header.setEllipsize(TextUtils.TruncateAt.END);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(header);

        List<String> keys = NotificationStore.getConversationStoreKeys(conversationKey);

        TextView subtitle = new TextView(this);
        subtitle.setText(getResources().getQuantityString(R.plurals.amz_message_count, keys.size(), keys.size()));
        subtitle.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = SafeArea.dp(this, 2);
        subParams.bottomMargin = SafeArea.dp(this, 10);
        content.addView(subtitle, subParams);

        int index = 0;
        int total = keys.size();
        for (String storeKey : keys) {
            NotificationData data = NotificationStore.getCustomNotification(storeKey);
            if (data == null)
                continue;
            addMessage(content, data, index++ == total - 1);
        }

        TextView replyButton = new TextView(this);
        replyButton.setText(getString(R.string.amz_reply));
        replyButton.setTextColor(0xFFFFFFFF);
        replyButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        replyButton.setTypeface(Typeface.DEFAULT_BOLD);
        replyButton.setGravity(Gravity.CENTER);
        replyButton.setBackgroundResource(R.drawable.bg_accent_button);
        replyButton.setMinWidth(SafeArea.dp(this, 150));
        replyButton.setMinHeight(SafeArea.dp(this, 44));
        replyButton.setPadding(SafeArea.dp(this, 20), SafeArea.dp(this, 12),
                SafeArea.dp(this, 20), SafeArea.dp(this, 12));
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReplies();
            }
        });
        LinearLayout.LayoutParams replyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        replyParams.gravity = Gravity.CENTER_HORIZONTAL;
        replyParams.topMargin = SafeArea.dp(this, 14);
        content.addView(replyButton, replyParams);

        return scroll;
    }

    private void addMessage(LinearLayout container, NotificationData data, boolean isLast) {
        MessageBubbleView bubble = new MessageBubbleView(this);
        bubble.setText(!TextUtils.isEmpty(data.getText()) ? data.getText() : data.getTitle());

        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.topMargin = SafeArea.dp(this, 8);
        container.addView(bubble, bubbleParams);

        TextView time = new TextView(this);
        time.setText(data.getTime() != null ? data.getTime() : "");
        time.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeParams.leftMargin = SafeArea.dp(this, 4);
        timeParams.bottomMargin = SafeArea.dp(this, isLast ? 0 : 4);
        container.addView(time, timeParams);
    }

    private void showReplies() {
        final List<Reply> replies = util.listReplies();
        if (replies == null || replies.isEmpty()) {
            Toast.makeText(this, getString(R.string.amz_no_replies), Toast.LENGTH_SHORT).show();
            return;
        }
        if (replyOverlay != null && replyOverlay.getParent() != null) {
            return;
        }

        replyOverlay = new ScrollView(this);
        replyOverlay.setFillViewport(true);
        replyOverlay.setBackgroundColor(0xE6000000);
        replyOverlay.setClickable(true);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_overlay_panel);
        int pad = SafeArea.dp(this, 16);
        panel.setPadding(pad, SafeArea.dp(this, 20), pad, SafeArea.dp(this, 16));
        replyOverlay.addView(panel, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText(getString(R.string.amz_reply));
        title.setTextColor(getResources().getColor(R.color.amz_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = SafeArea.dp(this, 12);
        panel.addView(title, titleParams);

        for (final Reply reply : replies) {
            ReplyPillView item = new ReplyPillView(this);
            item.setText(reply.getValue());
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemParams.bottomMargin = SafeArea.dp(this, 8);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendReply(reply.getValue());
                }
            });
            panel.addView(item, itemParams);
        }

        TextView cancel = new TextView(this);
        cancel.setText(getString(R.string.amz_cancel));
        cancel.setTextColor(getResources().getColor(R.color.amz_text_secondary));
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        cancel.setGravity(Gravity.CENTER);
        cancel.setMinHeight(SafeArea.dp(this, 40));
        cancel.setPadding(SafeArea.dp(this, 12), SafeArea.dp(this, 12),
                SafeArea.dp(this, 12), SafeArea.dp(this, 8));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideReplies();
            }
        });
        panel.addView(cancel);

        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(replyOverlay, overlayParams);
    }

    private void hideReplies() {
        if (replyOverlay != null && replyOverlay.getParent() != null) {
            root.removeView(replyOverlay);
            replyOverlay = null;
        }
    }

    private void sendReply(String value) {
        ReplyHelper.sendReply(conversationKey, value);
        NotificationStore.removeByNotificationKey(conversationKey);
        Logger.debug("ConversationActivity sendReply: {}", value);
        Toast.makeText(this, getString(R.string.amz_reply_sent), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (replyOverlay != null && replyOverlay.getParent() != null) {
            hideReplies();
            return;
        }
        super.onBackPressed();
    }
}
