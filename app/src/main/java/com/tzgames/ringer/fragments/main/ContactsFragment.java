package com.tzgames.ringer.fragments.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.content.res.AppCompatResources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tzgames.ringer.activities.MainActivity;
import com.tzgames.ringer.R;
import com.tzgames.ringer.data.ContactsManager;
import com.tzgames.ringer.data.ContactsManager.CustomRingerPerson;
import com.tzgames.ringer.data.VibrationsManager;
import com.tzgames.ringer.views.RoundedImageView;


/**
 * Fragment that consists of a ScrollableView that holds the contacts that have a custom ringtone
 * assigned. In other words, it is the visual representation of all CustomRingerPerson 's. Each
 * list item has some logic attatched to it like update ringtone / vib or delete person.
 */
public class ContactsFragment extends Fragment {

    /** Context used throughout fragment */
    private Context context;

    /** Root scrollView (which is actually the LinearLayout content of a scrollView) */
    private LinearLayout scrollView;

    /** Reference to MainActivity to handle update function calls */
    private MainActivity mainActivity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    /** Initial population of scroll view occurrs here */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        context = view.getContext();
        mainActivity = (MainActivity) context;
        scrollView = view.findViewById(R.id.scrollViewContent);
        populateScrollView();
    }

    /**
     * Converts dp to px. Used in classes outside of MainActivity too.
     */
    public static int dpToPx(Context ctx, float dp) {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        ctx.getResources().getDisplayMetrics()
                )
        );
    }

    /**
     * Method that re-draws the main ScrollView items and puts them into the listview.
     * It draws the CustomRingerPersons that have custom ringtones assigned to them.
     */
    public void populateScrollView() {
        // remove outdated list items and re-create from scratch
        scrollView.removeAllViews();

        // if no contacts are created, show default screen that prompts user to add contacts
        if (ContactsManager.isEmptyContacts(context)) {
            TextView t = new TextView(context);
            int pxPad = dpToPx(context,20);
            t.setPadding(pxPad, pxPad * 2, pxPad,0);
            t.setTextColor(Color.GRAY);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            t.setLayoutParams(params);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(16);
            t.setLineSpacing(dpToPx(context,3), (float) 1.0);
            t.setText(getString(R.string.no_contacts));
            scrollView.addView(t);
        }
        for (String c : ContactsManager.getAllContactNames(context)) {
            RelativeLayout contact = makeContactView(ContactsManager.getContact(context, c));
            contact.setOnClickListener(new ContactOnClickListener(c));
            contact.setOnLongClickListener(new ContactOnLongClickListener(c));
            scrollView.addView(contact);
        }
    }

    /**
     * Given a CustomRinger person, return a relativeLayout that will be a listitem in the main
     * contacts scrollView. See R.layout.list_item_contact to see what a listitem looks like.
     */
    @SuppressWarnings("deprecation")
    private RelativeLayout makeContactView(CustomRingerPerson person) {

        RelativeLayout view = (RelativeLayout) View.inflate(context, R.layout.list_item_contact, null);

        TextView name = view.findViewById(R.id.contact_name_txt);
        name.setText(person.name);

        Bitmap bitmap;
        RoundedImageView imgView = view.findViewById(R.id.contact_pic);

        // try to set image to image stored in contact. If not, set it to default no-face image
        try {
            if (Build.VERSION.SDK_INT < 28) {
                 bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                        Uri.parse(person.photoURI));
            } else {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(),
                        Uri.parse(person.photoURI));
                bitmap = ImageDecoder.decodeBitmap(source);
            }
            imgView.setImageBitmap(bitmap);
        } catch (Exception e) {
            imgView.setImageDrawable(
                    AppCompatResources.getDrawable(context, R.drawable.ic_default_user));
        }

        // Add notification tone under bolded name
        TextView txtTone = view.findViewById(R.id.contact_notif_txt);
        String tone = RingtoneManager.getRingtone(context, Uri.parse(person.ringtoneURI)).getTitle(context);
        if (tone.toLowerCase().contains("default")) {
            tone = tone.replace("default", "");
            tone = tone.replace("Default", "");
            tone = tone.replace("(", "");
            tone = tone.replace(")", "");
            tone = tone.replace("ringtone", "");
            tone = tone.replace("Ringtone", "");
            tone = tone.trim();
        }

        // If person is the default ringtone, do not add him to the list by making him invisible
        if (!person.ringtoneURI.equals(ContactsManager.getDefaultToneString(context))) {
            view.findViewById(R.id.contact_default_txt).setVisibility(View.GONE);
        }

        // If person has a default vibration pattern, do not write the name of the pattern
        if (!person.vibrateURI.equals(VibrationsManager.DEFAULT_VIBRATION))
            tone = tone + " | " + person.vibrateURI;

        txtTone.setText(tone);

        // If person has ringtones or vibrations turned off, draw appropriate on/off icons
        TextView icons = view.findViewById(R.id.contact_icons);
        Drawable left = person.ringtoneURI.equals(ContactsManager.NONE_RINGTONE_ID)
                ? AppCompatResources.getDrawable(context, R.drawable.ic_notifications_off_black_24dp)
                : AppCompatResources.getDrawable(context, R.drawable.ic_notifications_active_black_24dp);
        Drawable right = !person.vibrateURI.equals(VibrationsManager.NONE_VIBRATION)
                ? AppCompatResources.getDrawable(context, R.drawable.ic_vibration_black_24dp)
                : null;
        icons.setCompoundDrawablesWithIntrinsicBounds(left, null, right, null);
        icons.setText("");
        if (right != null) {
            icons.setText(" | ");
            icons.setTextSize(17);
            icons.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return view;
    }

    /**
     * OnLongClickListener for a list item in main scroll view. If a contact is long pressed,
     * present the option to delete that contact.
     */
    private class ContactOnLongClickListener implements View.OnLongClickListener {
        final String contactName;
        ContactOnLongClickListener(String name) {
            contactName = name;
        }
        @Override
        public boolean onLongClick(View view) {
            String[] options = {getString(R.string.main_dialog_delete_contact) + " " + contactName};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        ContactsManager.removeContact(context, contactName);
                        populateScrollView();
                    }
                }
            });
            AlertDialog built = builder.create();
            built.show();

            return true;
        }
    }

    /**
     * OnClickListener for a list item in the main scroll view. If a contact is clicked, then
     * give the user options to pick the contact's ringtone and vibration pattern. Also play the
     * current ringtone and vibration pattern as feedback for user.
     */
    private class ContactOnClickListener implements View.OnClickListener {
        final String contactName;
        ContactOnClickListener(String name) {
            contactName = name;
        }
        @Override
        public void onClick(View view) {
            String[] options = {getString(R.string.main_dialog_set_notif_tone),
                    getString(R.string.main_dialog_set_vib_pattern)};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(getString(R.string.main_dialog_title) + " " + contactName);
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        mainActivity.pickRingtone(contactName);
                    }
                    else if (which == 1) {
                        if (!mainActivity.getBillingManager().isPremium()) {
                            int vibs = ContactsManager.countContactsWithVibrations(context);
                            boolean hasVib = !ContactsManager.getContact(context, contactName).vibrateURI
                                    .equals(VibrationsManager.DEFAULT_VIBRATION);
                            if (vibs <= 0 || (vibs == 1 && hasVib)) {
                                mainActivity.pickVibrationPattern(contactName);
                            }
                            else {
                                mainActivity.buildInformationDialog(getString(R.string.not_premium_dialog_title),
                                        R.string.not_premium_dialog_txt_vib, getString(R.string.about_ok),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                mainActivity.getBillingManager().buyPremium();
                                            }
                                        }
                                ).show();
                            }
                        }
                        else {
                            mainActivity.pickVibrationPattern(contactName);
                        }
                    }
                }
            });
            AlertDialog built = builder.create();
            built.show();

            CustomRingerPerson person = ContactsManager.getContact(context, contactName);


            // play vibration
            VibrationsManager.vibrateByName(context, person.vibrateURI);

            // play notification tone
            RingtoneManager.getRingtone(context, Uri.parse(person.ringtoneURI)).play();
        }
    }
}
