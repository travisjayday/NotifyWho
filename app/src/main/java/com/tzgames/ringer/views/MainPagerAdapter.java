package com.tzgames.ringer.views;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.tzgames.ringer.R;
import com.tzgames.ringer.fragments.main.ContactsFragment;
import com.tzgames.ringer.fragments.main.DefaultFragment;
import com.tzgames.ringer.fragments.main.HelpFragment;

/**
 * FragmentPagerAdapter that is in charge of juggling the three main fragments of the app:
 *  CONTACTS - index 0 - the fragment that shows the list of contacts that have custom ringtones
 *  DEFAULT - index 1 - the fragment that has tiles to update default ringtones / vibrations
 *  HELP - index 2 - the fragment that has help message, rating bar, and contact developer
 * By calling setDirty(index), the pager updates the corresponding views.
 */
public class MainPagerAdapter extends FragmentPagerAdapter {

    /** Fragment index that shows list view of all current contacts that have ringtones assigned */
    public static final int POSITION_CONTACTS_FRAG = 0;

    /** Fragment index that has options to set default ringtones / vibs */
    public static final int POSITION_DEFAULT_FRAG = 1;

    ///** Fragment index that has rating / help / contact developer options */
    // public static final int POSITION_HELP_FRAG = 2;

    /** Number of fragments this page adapter juggles */
    private static final int NUM_TABS = 3;

    /** Array that holds the strings of the names of the three fragments */
    private final String[] tabTitles;

    /** Position that is currently dirty and will be updated soon. -1 means nothing is dirty */
    private int dirtyPosition = -1;

    /** Context used throughout this class */
    private final Context context;

    /** Sets tab names and context */
    public MainPagerAdapter(FragmentManager fragmentManager, Context ctx) {
        super(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        tabTitles = new String[] {
                ctx.getString(R.string.toolbar_contacts),
                ctx.getString(R.string.toolbar_default),
                ctx.getString(R.string.toolbar_help)};
        context = ctx;
    }

    /** Called from outside this class and sets certain views dirty. Then notifies itself to
     * update the view that is marked dirty.
     * @param position The position of the view that should be updated.
     */
    public void setDirty(int position) {
        dirtyPosition = position;
        this.notifyDataSetChanged();
    }

    /** Called when notifyDataSetChanged() gets called. Loops through all vies and checks if it is
     * dirty. Then updates the view if it is dirty. Returning POSITION_UNCHANGED stops from force
     * redrawing the fragment, since we just want to update some of it.
     * @param item The current fragment under consideration
     */
    @Override
    public int getItemPosition(@NonNull Object item) {
        if (dirtyPosition == 0 && item instanceof ContactsFragment) {
            ((ContactsFragment) item).populateScrollView();
            dirtyPosition = -1;
        }
        else if (dirtyPosition == 1 && item instanceof DefaultFragment) {
            ((DefaultFragment) item).refreshDefaults(context);
            dirtyPosition = -1;
        }
        return POSITION_UNCHANGED;
    }

    /** Returns total number of pages. */
    @Override
    public int getCount() {
        return NUM_TABS;
    }

    /** Returns the fragment to display for a particular page. */
    @Override
    @NonNull public Fragment getItem(int position) {
        switch (position) {
            case 1:
                return new DefaultFragment();
            case 2:
                return new HelpFragment();
            default:
                // if index is zero or otherwise, return ContactsFragment
                return new ContactsFragment();
        }
    }

    /** Returns the page title for the top indicator */
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
