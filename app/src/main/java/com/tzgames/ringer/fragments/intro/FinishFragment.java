package com.tzgames.ringer.fragments.intro;


import android.os.Handler;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.fragment.app.Fragment;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.tzgames.ringer.activities.PermissionsActivity;
import com.tzgames.ringer.R;

/**
 * Fragment that lets the user know that the setup is finished. It also displays a cute
 * animatable drawable that turns into a heart.
 */
public class FinishFragment extends Fragment {
    /** Image view that displays vector animatable */
    private ImageView imgV;

    /** The vector drawable */
    private AnimatedVectorDrawableCompat animatedVector;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finish, container, false);
    }

    /**
     *  Set Animatable Drawable resource and add Button Callback handler to PermissionsActivity
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imgV = view.findViewById(R.id.id_fin_img);
        animatedVector = AnimatedVectorDrawableCompat.create(view.getContext(), R.drawable.ic_animated_finish_bell);
        imgV.setImageDrawable(animatedVector);

        imgV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animatedVector.stop();
                imgV.setImageDrawable(getResources().getDrawable(R.drawable.ic_notifications_black_24dp));
                playAnim(600);
            }
        });

        Button btn = view.findViewById(R.id.id_fin_next_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionsActivity activity = (PermissionsActivity) view.getContext();
                activity.finishFragmentClick();
            }
        });
    }

    /**
     * Function that plays the animation after some time
     * @param delay The amount of time after which the animation will be played
     */
    private void playAnim(int delay) {
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                imgV.setImageDrawable(animatedVector);
                animatedVector.start();
            }
        }, delay);
    }

    /**
     * Play the animation whence the fragment is loaded
     */
    @Override
    public void onStart() {
        super.onStart();
        playAnim(1000);
    }
}
