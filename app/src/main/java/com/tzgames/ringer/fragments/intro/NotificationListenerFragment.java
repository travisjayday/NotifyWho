package com.tzgames.ringer.fragments.intro;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.tzgames.ringer.activities.PermissionsActivity;
import com.tzgames.ringer.R;


/**
 * Fragment that displays the prompt to enable notification listener service
 */
public class NotificationListenerFragment extends Fragment {
    public NotificationListenerFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notification_sys, container, false);
    }

    /**
     * Set Button Callback handler to PermissionsActivity
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button btn = view.findViewById(R.id.id_notif_next_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionsActivity activity = (PermissionsActivity) view.getContext();
                activity.notificationFragmentClick();
            }
        });
    }

}