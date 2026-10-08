package com.amazmod.service.support;

import android.content.Context;
import android.support.wearable.view.GridViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class HorizontalGridViewPager extends GridViewPager {

    private final GestureDetector mGestureDetector;
    private boolean interceptHorizontal = true;

    public HorizontalGridViewPager(Context context, AttributeSet attrs ) {
        super( context, attrs );
        mGestureDetector = new GestureDetector( context, new HScrollDetector() );
    }

    // When disabled the pager will not intercept horizontal scrolls, so row views
    // (e.g. notification rows with swipe-to-delete) can handle them.
    public void setInterceptHorizontal(boolean intercept) {
        this.interceptHorizontal = intercept;
    }

    @Override
    public boolean onInterceptTouchEvent( MotionEvent ev ) {
        if (!interceptHorizontal)
            return false;
        // If we have more horizontal than vertical scrolling, intercept the event,
        // otherwise let the child handle it
        return super.onInterceptTouchEvent( ev ) && mGestureDetector.onTouchEvent( ev );
    }

    class HScrollDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
            // Returns true if scrolling horizontally
            return ( Math.abs( distanceX ) > Math.abs( distanceY ) );
        }
    }
}