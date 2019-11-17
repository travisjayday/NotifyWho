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
 * Fragment that displays the prompt to enable read contacts permissions
 */
public class PermissionsFragment extends Fragment {
    public PermissionsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permissions, container, false);
    }

    /**
     * Set Button Callback handler to PermissionsActivity
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button btn = view.findViewById(R.id.id_permissions_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PermissionsActivity activity = (PermissionsActivity) view.getContext();
                activity.permissionsFragmentClick();
            }
        });
    }
}