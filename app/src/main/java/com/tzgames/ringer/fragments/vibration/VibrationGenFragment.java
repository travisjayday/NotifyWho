package com.tzgames.ringer.fragments.vibration;


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tzgames.ringer.R;
import com.tzgames.ringer.data.VibrationsManager;
import com.tzgames.ringer.activities.VibrationPickerActivity;
import com.tzgames.ringer.views.GeneratorView;

import static com.tzgames.ringer.fragments.main.ContactsFragment.dpToPx;

/**
 * Fragment that overlays VibrationPickerActivity and provides a view for the user to generate
 * vibration patterns based on how long he clicks that view. Also allows editing vibration
 * patterns by inputting raw strings. And in charge of naming / previewing the pattern.
 */
public class VibrationGenFragment extends Fragment {

    /** The input field where user writes the name for his custom vibration */
    private TextInputEditText editName;

    /** The input field where user / fragment write the sequence of vibration longs */
    private TextInputEditText editText;

    /** Reference to parent activity */
    private VibrationPickerActivity pickerAct;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        pickerAct = (VibrationPickerActivity) getActivity();

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_vibration_gen, container, false);

        // Get display width in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        pickerAct.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;

        // Set width of canvas holder to 75% and height to 80% of width
        View cv = view.findViewById(R.id.vib_gen_canvas_holder);
        ViewGroup.LayoutParams layoutParams = cv.getLayoutParams();
        layoutParams.width = (int) (displayWidth * 0.8);
        layoutParams.height = (int) (layoutParams.width * 0.75);
        cv.setLayoutParams(layoutParams);

        // Initially hide Play button FAB. Show once user generated a vibration.
        final FloatingActionButton playBtn = view.findViewById(R.id.vib_gen_play_btn);
        ConstraintLayout.LayoutParams p =(ConstraintLayout.LayoutParams) playBtn.getLayoutParams();
        p.setMargins(0, (int) Math.round(layoutParams.height
                - dpToPx(pickerAct, 56) / 2.0) , 0, 0);
        playBtn.setLayoutParams(p);
        playBtn.hide();

        // Set on change listeners
        editName = view.findViewById(R.id.vib_gen_edit_name);
        editText = view.findViewById(R.id.vib_gen_timestamps_txt);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                pickerAct.enableSave(!editable.toString().equals(""));
                if (editable.toString().length() > 0 && playBtn.getVisibility() != View.VISIBLE)
                    playBtn.show();
                else if (editable.toString().length() == 0 && playBtn.getVisibility() == View.VISIBLE)
                    playBtn.hide();
            }
        });

        // get the generator view and assign callbacks that are used to calculate vibration patterns
        GeneratorView genView = view.findViewById(R.id.vib_gen_view);
        final View pressToStart = view.findViewById(R.id.vib_gen_press_to_start_text);
        genView.setOnGeneratorTouchListener(new GeneratorView.GeneratorTouchListener() {
            @Override
            public void onTouchEnded(long touchEndTime) {
                // Time the user pressed on the screen
                String dt = String.valueOf(touchEndTime - getRecentStartTime());
                Editable txt = editText.getText();
                if (txt != null) {
                    String newTxt = txt.toString();
                    newTxt += (newTxt.equals("") ? dt : ", " + dt);
                    editText.setText(newTxt);
                    VibrationsManager.cancelVibrate(pickerAct);
                }
            }
            @Override
            public void onTouchStarted(long touchStartTime) {
                // Time the user DID NOT press on the screen
                long dt = touchStartTime - getRecentEndTime();

                if (dt > 10 * 1000) {
                    // first touch
                    dt = 0;
                    pressToStart.setVisibility(View.GONE);
                }

                Editable txt = editText.getText();
                if (txt != null) {
                    String newTxt = txt.toString();
                    newTxt += (newTxt.equals("")? dt : ", " + dt);
                    editText.setText(newTxt);
                    VibrationsManager.vibrateForever(pickerAct);
                }
            }
        });

        // try to parse generated / given pattern and play it
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (pickerAct.isBatterySaverOn())
                pickerAct.showSnackbar(R.string.vib_picker_batterysaver_enabled, Snackbar.LENGTH_LONG);
            // play vibration list
            long[] times = getPattern();
            if (times != null)
                VibrationsManager.vibrate(pickerAct, times);
            else
                pickerAct.showSnackbar(R.string.vib_gen_pattern_parsing_error, Snackbar.LENGTH_LONG);
            }
        });

        // if user clicks on shaded background, exit
        View background = view.findViewById(R.id.vib_gen_background_shadow);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickerAct.onBackPressed();
            }
        });
        return view;
    }

    /**
     * Parses the generated pattern and turns it into array of longs, ready to be played.
     * @return The long[] that the user generated. If the user generated sequence is invalid, NULL.
     */
    public long[] getPattern() {
        Editable text = editText.getText();
        if (text == null) return null;
        String[] items = text.toString()
                .replaceAll("\\s", "")
                .split(",");

        // try to parse each item as a long
        long[] times = new long[items.length];
        try {        // get individual times by splitting input string
            for (int i = 0; i < items.length; i++) times[i] = Long.decode(items[i]);
        } catch (Exception e) {
            return null;
        }
        return times;
    }

    /**
     * Returns the custom name that the user gave his vibration
     */
    public String getPatternName() {
        String ret = editName.getHint().toString();
        Editable name = editName.getText();
        if (name != null && !name.toString().equals("")) ret = name.toString();
        return ret;
    }
}
