package fav.drtinao.skama;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Custom ViewPager, which disables switching between tabs by swipe. The mentioned behaviour is unwanted in charts because
 * user can switch between tabs by accident when searching for a value in chart.
 */
public class ActualValueViewPager extends ViewPager {
    public ActualValueViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
