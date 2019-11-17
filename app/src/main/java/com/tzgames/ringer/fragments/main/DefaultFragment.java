package com.tzgames.ringer.fragments.main;


import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tzgames.ringer.data.ContactsManager;
import com.tzgames.ringer.activities.MainActivity;
import com.tzgames.ringer.R;

/**
 * Fragment that holds the Default ringtone / vibration patterns used. User can update them by
 * clicking on the corresponding tiles.
 */
public class DefaultFragment extends Fragment {

    /** Default Notification TextView */
    private TextView defNotifTxt;

    /** Default Vibration TextView */
    private TextView defVibTxt;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_default, container, false);

        // Set onClick Handlers for setting vib / ringtones
        CardView setDefNotifBtn = view.findViewById(R.id.set_default_tone_btn);
        setDefNotifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null)
                    activity.pickRingtone(ContactsManager.getDefaultContactName());
            }
        });

        CardView setDefVibBtn = view.findViewById(R.id.set_default_vibration_btn);
        setDefVibBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null)
                    activity.pickVibrationPattern(ContactsManager.getDefaultContactName());
            }
        });

        // Set text of text of default ringtone / vib pattern to appropriate ones that are saved
        // in NotifyWho
        defNotifTxt = view.findViewById(R.id.set_default_tone_txt);
        defVibTxt = view.findViewById(R.id.set_default_vib_txt);
        refreshDefaults(view.getContext());

        return view;
    }

    /**
     * Method that updates the current default textviews that show which ringtones / vibs are the
     * default. Should be called implicitely by ViewPager when this view is dirty. This view should
     * be dirty when the default ringtones/ vibs changes.
     */
    public void refreshDefaults(Context ctx) {
        Ringtone tone = RingtoneManager.getRingtone(ctx, Uri.parse(ContactsManager.getDefaultToneString(ctx)));
        defNotifTxt.setText(tone.getTitle(ctx));
        defVibTxt.setText(ContactsManager.getDefaultVibString(ctx));
    }
}
