package com.tzgames.ringer.views;

import android.content.Context;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;

import com.tzgames.ringer.R;

/**
 * A simple view that represents a vibration list item in VibrationPickerActivity. Used in
 * VibListItemAdapter to create a scrollable list view of all vibration patterns that the user
 * can select from.
 */
public class VibrationListItem extends LinearLayout implements Checkable {

    /** The main checkbox associated with whether the item is selected or not */
    private final AppCompatRadioButton checkBox;

    /** Inflate view from xml resource and find checkBox */
    public VibrationListItem(Context context) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.list_item_vibration, this, true);
        checkBox = v.findViewById(R.id.selection_checkbox);
    }

    /**
     * @return Name of the vibration associated with this list item
     */
    public String getVibrationName() {
        return (String)(checkBox.getText());
    }

    /*
     * @param The name this list item's vibration should have
     */
    public void setVibrationName(String name) {
        checkBox.setText(name);
    }

    /**
     * @return True if this list item is checked. Else, false.
     */
    @Override
    public boolean isChecked() {
        return checkBox.isChecked();
    }

    /**
     * @param checked Set the checked state of this list item (vibration pattern)
     */
    @Override
    public void setChecked(boolean checked) {
        checkBox.setChecked(checked);
    }

    /**
     * Toggle the checked state of this list item (vibration pattern)
     */
    @Override
    public void toggle() {
        checkBox.toggle();
    }
}
