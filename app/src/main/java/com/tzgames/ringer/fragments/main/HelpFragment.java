package com.tzgames.ringer.fragments.main;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import com.tzgames.ringer.activities.MainActivity;
import com.tzgames.ringer.R;

import static com.tzgames.ringer.activities.PermissionsActivity.isPackageInstalled;
import static com.tzgames.ringer.fragments.main.ContactsFragment.dpToPx;

/**
 * The Help Screen Fragment used in MainActivity. This shows rating bar, advice and has
 * contact dev. options. Also prompts user to buy premium if he's not premium.
 */
public class HelpFragment extends Fragment {
    /* Reference to premium card view (to animate) */
    private CardView premium = null;

    /* Currently showing the premium banner */
    private boolean premiumShown = false;

    /** User is premium boolean */
    private boolean isPremium = false;

    /** Reference to main activity */
    private MainActivity main;

    /**
     * Build the Help Screen
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        main = (MainActivity) getActivity();
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        initRatingBar(view);
        initContactDev(view);
        initPremiumCV(view);
        isPremium = main.getBillingManager().isPremium();
        return view;
    }

    /**
     * Adds onClickListener to premium tile. On Click: show buyPremium prompt from MainActivity
     */
    private void initPremiumCV(View view) {
        premium = view.findViewById(R.id.help_premium_tile);
        premium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main.getBillingManager().buyPremium();
            }
        });
    }

    /**
     * Adds listener to the 5 star rating bar. On Click, open GooglePlay NotifyWho
     */
    private void initRatingBar(View view) {
        RatingBar rb = view.findViewById(R.id.rating_bar);
        rb.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                String str = "https://play.google.com/store/apps/details?id=com.tzgames.ringer";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
            }
        });
    }

    /**
     * Adds onClickListener to Contact Developer tile. On Click: Open Gmail (or equivalent) with
     * a pre-defined email and my email so that users can ask for help.
     */
    // TODO: Put E-mail dev help in strings.xml
    private void initContactDev(View view) {
        CardView cv = view.findViewById(R.id.cardview_contact_dev);
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:"));
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"travisjayday@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "NotifyWho: Developer Contact: Subject");
                i.putExtra(Intent.EXTRA_TEXT   , "Hey Travis,\nI have the following issues/suggestions/feedback:\n-\n-\n-");

                if (isPackageInstalled(main, "com.google.android.gm")) {
                    i.setPackage("com.google.android.gm");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    main.startActivity(i);
                } else {
                    try {
                        main.startActivity(Intent.createChooser(i, "Send email..."));
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(main, "No email app is installed on your device...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * This method gets called when the fragment becomes visible to user (if visible is set true)
     * Thus, check if premium, if not, then show the Go premium! banner by animating it into screen.
     * @param visible if true, show premium if user is not premium
     */
    @Override
    public void setMenuVisibility(final boolean visible) {
        super.setMenuVisibility(visible);
        if (!isPremium && visible && !premiumShown && premium != null) {
            premiumShown = true;
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) premium.getLayoutParams();
            marginLayoutParams.topMargin = dpToPx(main,20);
            premium.requestLayout();
            ValueAnimator anim = ValueAnimator.ofInt(0, 500);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = premium.getLayoutParams();
                    layoutParams.height = val;
                    premium.setLayoutParams(layoutParams);
                }
            });
            anim.setDuration(500);
            anim.start();
        }
    }
}
