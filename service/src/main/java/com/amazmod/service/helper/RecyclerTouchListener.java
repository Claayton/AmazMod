package com.amazmod.service.helper;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

    private static final int SWIPE_THRESHOLD = 60;

    private GestureDetector gestureDetector;
    private ClickListener clickListener;
    private SwipeListener swipeListener;

    private float downX, downY;
    private View downChild;

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    public interface SwipeListener {
        void onSwipeLeft(View view, int position);
    }

    public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {
        recyclerView.setSaveEnabled(false);
        this.clickListener = clickListener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null && clickListener != null) {
                    clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                }
            }
        });
    }

    public void setSwipeListener(SwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        rv.setSaveEnabled(false);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                downChild = rv.findChildViewUnder(e.getX(), e.getY());
                break;
            case MotionEvent.ACTION_UP:
                if (downChild != null && swipeListener != null) {
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    if (dx < -SWIPE_THRESHOLD && Math.abs(dx) > Math.abs(dy)) {
                        swipeListener.onSwipeLeft(downChild, rv.getChildAdapterPosition(downChild));
                    }
                }
                downChild = null;
                break;
            default:
                break;
        }

        View child = rv.findChildViewUnder(e.getX(), e.getY());
        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
            clickListener.onClick(child, rv.getChildAdapterPosition(child));
        }

        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        rv.setSaveEnabled(false);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
