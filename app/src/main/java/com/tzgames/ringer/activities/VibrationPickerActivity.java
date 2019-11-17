package com.tzgames.ringer.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;


import com.tzgames.ringer.R;
import com.tzgames.ringer.fragments.vibration.VibrationGenFragment;
import com.tzgames.ringer.views.VibListItemAdapter;
import com.tzgames.ringer.data.VibrationsManager;
import com.tzgames.ringer.views.VibrationListItem;

import java.util.ArrayList;

/**
 * Activity that handles the act of picking from a list of pre-defined and user created vibrations.
 * It's main components is a listview of checkable vibrations that the user can pick from, and then
 * save. Also handles creation of new vibrations through VibrationGenFragment
 */
// TODO: put string literals in strings.xml
public class VibrationPickerActivity extends AppCompatActivity {

    /** The pattern that was intially assigned to the contact the user is editing */
    private String currentPattern;

    /** The new pattern that was selected by the user */
    private String newPattern;

    /** reference to the SAVE button on the ActionBar */
    private MenuItem saveBtn;

    /** The VibrationGenFragment that is used for creating custom vibrations */
    private VibrationGenFragment vibrationGenFragment = null;

    /** FAB used to add custom vibrations */
    private FloatingActionButton createVibFab;

    /** Flag for whether or not the user is able to create custom vibrations. Passed by intent. */
    private boolean canSaveCustomVib = false;

    /**
     * Creates the activity views. Sets actionbar actions, creates list of vibrations the user
     * can choose from, sets current vibration pattern, handles vibration selections, notifies
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibration_picker);

        Toolbar tb = findViewById(R.id.toolbar_vibrations);
        setSupportActionBar(tb);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // get the current pattern set for the given contact (from intent) and display it
        currentPattern = "";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentPattern = extras.getString("CurrentPattern");
            if (currentPattern != null && !currentPattern.equals("")) {
                TextView txt = findViewById(R.id.current_vibration_pattern_txt);
                txt.setText(currentPattern);
            }
            else
                currentPattern = null;
            canSaveCustomVib = extras.getBoolean("EnableCustom");
        }

        // get all vibrations and put them in a list view so the user can select them
        ListView vibrationsList = findViewById(R.id.vibrations_list);
        ArrayList<String> vibNames = new ArrayList<>(VibrationsManager.getVibrations(this).keySet());
        final VibListItemAdapter adapter = new VibListItemAdapter(this, 0, vibNames,null);
        vibrationsList.setAdapter(adapter);

        // set currently selected item as checked
        final int id = vibNames.indexOf(currentPattern);
        vibrationsList.setSelection(id);
        vibrationsList.setItemChecked(id, true);

        // add on click listener to update selections
        vibrationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                VibrationListItem vv = (VibrationListItem) view;
                String vibName = vv.getVibrationName();

                // preview vibration
                VibrationsManager.vibrateByName(view.getContext(), vibName);
                newPattern = vibName;

                if (vibName.equals(currentPattern)) enableSave(false);
                else enableSave(true);

            }
        });

        // add on long click listener to delete vibrations
        vibrationsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int i, long l) {
                VibrationListItem vv = (VibrationListItem) view;
                final String vibName = vv.getVibrationName();


                String[] items = {view.getContext().getString(R.string.vib_picker_delete_custom_vib)
                        + " " +  vibName};

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // check if vibName is custom vibration
                            if (VibrationsManager.removeCustomVibration(view.getContext(), vibName)) {
                                adapter.remove(vibName);
                                showSnackbar("Removed '" + vibName + "'", Snackbar.LENGTH_SHORT);
                            } else {
                                showSnackbar("Cannot remove non-custom vibration!", Snackbar.LENGTH_LONG);
                            }
                        }
                    }
                });
                AlertDialog built = builder.create();
                built.show();
                return true;
            }
        });

        // Add the FAB that the user presses to create custom vibrations
        createVibFab = findViewById(R.id.fab_add_vibration);
        createVibFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableSave(false);
                vibrationGenFragment = new VibrationGenFragment();
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.vib_gen_container, vibrationGenFragment)
                        .commitAllowingStateLoss();
                createVibFab.hide();
            }
        });
        // Hide FAB initially and then animate it
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                createVibFab.show();
            }
        }, 500);

        // If user has battery save mode enabled, let him know that vibrations won't play
        if (isBatterySaverOn())
            Snackbar.make(findViewById(R.id.vibration_coordlayout),
                            getString(R.string.vib_picker_batterysaver_enabled),
                            Snackbar.LENGTH_LONG).show();
    }

    /**
     * Makes the save button clickable so that the user can exit & save newly selected vibration
     * or make it unclickable until the user selects a different vibration pattern.
     * @param enabled Whether the save button should be clickable or not.
     */
    public void enableSave(boolean enabled) {
        if (enabled) {
            saveBtn.setEnabled(true);
            SpannableString s = new SpannableString(saveBtn.getTitle());
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary)),
                    0, s.length(), 0);
            saveBtn.setTitle(s);
        } else {
            saveBtn.setEnabled(false);
            SpannableString s = new SpannableString(saveBtn.getTitle());
            s.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorPrimaryDisabled)),
                    0, s.length(), 0);
            saveBtn.setTitle(s);
        }
    }

    /**
     * Creates actionbar buttons for saving the currently selected vibration. Disable saving by
     * default because no vibration has been selectd yet.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_vibpicker_menu, menu);
        saveBtn = menu.findItem(R.id.vibration_picker_save_btn);
        enableSave(false);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Click Handler for when the save button is clicked. If not creating a new vibration, then
     * just set selected vibration in an intent and finish the activity so that the calling activity
     * can handle assigning the picked vibration. If currently creating a new vibration, check if
     * it is valid, then save it and exit to calling activity.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.vibration_picker_save_btn) {

            // user not creating new vibration currently, so exiting
            if (vibrationGenFragment == null) {
                Intent data = new Intent();
                data.setData(Uri.parse(newPattern));
                setResult(RESULT_OK, data);
                finish();
            }
            else {
                // userdefined name of new vibration
                String vibName = vibrationGenFragment.getPatternName();

                // userdefined pattern
                long[] times = vibrationGenFragment.getPattern();

                if (times == null) {
                    showSnackbar(R.string.vib_gen_pattern_parsing_error, Snackbar.LENGTH_LONG);
                }
                else {
                    if (!canSaveCustomVib) {
                        setResult(MainActivity.RESULT_BUY_PREMIUM, null);
                        finish();
                    }
                    else if (VibrationsManager.addCustomVibration(this, vibName, times)) {
                        Intent data = new Intent();
                        data.setData(Uri.parse(vibName));
                        setResult(RESULT_OK, data);
                        finish();
                    }
                    else {
                        showSnackbar(R.string.vib_gen_pattern_naming_error, Snackbar.LENGTH_LONG);
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If user pressed back button, and is editing a custom vibration, remove the VibratoinGenFrag.
     * If he is not editing custom vibration, return to MainActivity.
     */
    @Override
    public void onBackPressed() {
        if (vibrationGenFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(vibrationGenFragment).commit();
            vibrationGenFragment = null;
            createVibFab.show();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * If user presss back arrow in navigation bar, trigger back press to go to MainActivity
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * @return True if the user has battery saver enabled, false otherwise.
     */
    public boolean isBatterySaverOn() {
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        return (powerManager != null && powerManager.isPowerSaveMode());
    }

    /**
     * Shows a snackbar in activity
     * @param id ID of strings.xml resource
     * @param length Time to show snackbar. E.g. Snackbar.LENGTH_LONG
     */
    public void showSnackbar(int id, int length) {
        Snackbar.make(findViewById(R.id.vibration_coordlayout),
                getString(id),
                length).show();
    }

    /**
     * Shows a snackbar in activity
     * @param str Literal string to display
     * @param length Time to show snackbar. E.g. Snackbar.LENGTH_LONG
     */
    private void showSnackbar(String str, int length) {
        Snackbar.make(findViewById(R.id.vibration_coordlayout),
                str,
                length).show();
    }
}
