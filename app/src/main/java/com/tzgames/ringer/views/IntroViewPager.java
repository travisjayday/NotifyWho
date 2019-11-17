package com.tzgames.ringer.views;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.tzgames.ringer.fragments.intro.FinishFragment;
import com.tzgames.ringer.fragments.intro.NotificationListenerFragment;
import com.tzgames.ringer.fragments.intro.PermissionsFragment;
import com.tzgames.ringer.fragments.intro.WhatsappDisableFragment;

/**
 * ViewPager that holds all the IntroFragments. This lets PermissionsActivity easily transition
 * from one page to another with IntroViewPager.arrowScroll.
 */
public class IntroViewPager extends ViewPager {
    private class IntroViewPagerAdapter extends FragmentPagerAdapter{
        /**
         * Number of fragments this page adapter juggles
         */
        private static final int NUM_PAGES=4;

        private IntroViewPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        /** Returns total number of pages. */
        @Override
        public int getCount(){
            return NUM_PAGES;
        }

        /** Returns the fragment to display for a particular page. */
        @Override
        @NonNull
        public Fragment getItem(int position){
            switch(position){
                case 1:
                    return new NotificationListenerFragment();
                case 2:
                    return new WhatsappDisableFragment();
                case 3:
                    return new FinishFragment();
                default:
                    // if index is 0 or otherwise, start with first page
                    return new PermissionsFragment();
            }
        }
    }

    /**
     * Initialize this IntroViewPager by creating the adapter
     * @param manager Support Fragment Manager
     */
    public void attachAdapter(FragmentManager manager) {
        this.setAdapter(new IntroViewPagerAdapter(manager));
    }

    public IntroViewPager (Context context, AttributeSet attr) {
        super(context, attr);
    }

    /**
     * Disable Manual scrolling. I.e. User cannot swipe left or right. This is instead controlled
     * by the buttons that he presses.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    /**
     * Disable Manual scrolling. I.e. User cannot swipe left or right. This is instead controlled
     * by the buttons that he presses.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

}
