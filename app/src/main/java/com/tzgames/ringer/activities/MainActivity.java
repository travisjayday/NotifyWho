package com.tzgames.ringer.activities;

import com.google.android.material.snackbar.Snackbar;
import com.tzgames.ringer.data.BillingManager;
import com.tzgames.ringer.data.ContactsManager.CustomRingerPerson;
import com.tzgames.ringer.data.ContactsManager;
import com.tzgames.ringer.R;
import com.tzgames.ringer.views.MainPagerAdapter;
import com.tzgames.ringer.data.VibrationsManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.core.content.ContextCompat;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import static com.tzgames.ringer.activities.PermissionsActivity.isPackageInstalled;
import static com.tzgames.ringer.views.MainPagerAdapter.POSITION_CONTACTS_FRAG;
import static com.tzgames.ringer.views.MainPagerAdapter.POSITION_DEFAULT_FRAG;

/**
 * MainActivity that is in charge of the majority of app logic.
 *
 * It consists of 3 fragments:
 *
 * ContactsFragment - Displays all the current contacts that have custom ringtones assigned
 * DefaultFragment - Has options to set default ring/vibration patterns
 * Help - Help screen for advice / contact developer
 *
 * These fragments are managed by MainPagerAdapter and are updated by MainActivity when new data
 * becomes available.
 *
 * MainActivity also checks if correct permissions are set and prompts user if not set.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAINACT";

    /** Result for when user picks contact */
    private final static int RESULT_PICK_CONTACT = 1;

    /** Result for when user picks ringtone */
    private final static int RESULT_PICK_TONE = 2;

    /** Result for when user picks vibration pattern using VibrationPickerActivity */
    private final static int RESULT_PICK_VIB = 3;

    /** Result for when user enables the Notifcation Listener service in Android Settings */
    final static int RESULT_ENABLED_SERVICE = 4;

    /** Result for when user returns from WhatsApp and just disabled sounds there */
    final static int RESULT_WHATSAPP_DISABLED = 5;

    /** Result for when user finished going through PermissionsActivity / introduction fragments */
    private final static int RESULT_SETUP_FIN = 6;

    /** Result for when permission to read contacts has been granted */
    final static int PERMISSIONS_REQUEST_READ_CONTACTS = 7;

    /** Extra data put into an intent when the Android Settings are launched to enable the service.
     * This is used to tell whether a user returns to the app coming from the Settings */
    final static String SERVICE_ENABLED_FINISHED = "finished";

    /** Result for when User does not have premium and is redirected to buy it */
    public final static int RESULT_BUY_PREMIUM = 9;

    /** Whatsapp Package name */
    public static final String WHATSAPP_PACKAGE = "com.whatsapp";

    /** Whatsapp Vibration / Notification tone settings activity */
    public static final String WHATSAPP_SETTINGS = "com.whatsapp.SettingsNotifications";

    /** Contact that is being edited currently */
    private String choosingContact = "null";

    /** Bool whether user is in Android settings, trying to enable service */
    private boolean promptingNotificationListener = false;

    /** MainActivity ViewPager Adapter */
    private MainPagerAdapter mPagerAdapter;

    /** Billing Manager used to purchase premium or check if premium is available */
    private BillingManager billingManager;

    /**
     * Initializes MainActivity.
     * Creates BillingManager to check if premium was bought, sets Toolbar with TabLayout,
     * creates pageAdapter to display the three fragments, and sets onClickListener to FAB
     * to add new contacts.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VibrationsManager.genNotificationChannels(this);

        billingManager = new BillingManager();
        billingManager.connectToGoogle(this);

        Toolbar bar = findViewById(R.id.my_toolbar);
        bar.setTitle(getString(R.string.app_name));
        setSupportActionBar(bar);

        // setup fragments and tab layout
        ViewPager mPager = findViewById(R.id.mainViewPager);
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), this);
        mPager.setOffscreenPageLimit(3);
        mPager.setAdapter(mPagerAdapter);

        TabLayout tab = findViewById(R.id.tabs);
        tab.setupWithViewPager(mPager);

        FloatingActionButton addBtn = findViewById(R.id.fab_add_contact);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // discriminate against users who didn't buy premium. They are only allowed 2 contacts.
                if (ContactsManager.getAllContactNames(view.getContext()).size() < 2
                        || billingManager.isPremium()) {
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
                }
                else {
                    buildInformationDialog(getString(R.string.not_premium_dialog_title),
                            R.string.not_premium_dialog_txt, getString(R.string.about_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    billingManager.buyPremium();
                                }
                            }
                    ).show();
                }
            }
        });
    }

    /**
     * Called when user re-enters the activity. Checks whether all permissions are properly set.
     * Also launches PermissionsActivity introduction if it's first time startup.
     */
    @Override
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started), false);
        if(!previouslyStarted) {
            Log.d(TAG, "starting first time act");
            Intent intent  = new Intent(this, PermissionsActivity.class);
            startActivityForResult(intent, RESULT_SETUP_FIN);
        }
        else {
            checkPermissions();
        }
        promptingNotificationListener = false;
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    /**
     * In Charge of handling user clicking on various menu-items.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_buy_premium:
                // show Dialog to prompt user to buy premium
                informPremium();
                return true;
            case R.id.action_about:
                // show Dialog that tells a little about this app
                buildInformationDialog(getString(R.string.action_about_str),
                        R.string.about_msg,
                        getString(R.string.about_ok),
                        null).show();
                return true;
            case R.id.action_help:
                // show Dialog that should help the user
                buildInformationDialog(getString(R.string.action_help_str),
                        R.string.help_msg,
                        getString(R.string.about_ok),
                        null).show();
                return true;
            case R.id.action_set_default:
                // show Dialog that will allow user to set default ringtone
                buildInformationDialog(getString(R.string.action_set_default_tone),
                        R.string.set_default_tone_msg,
                        getString(R.string.set_default_tone_btn),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pickRingtone(ContactsManager.getDefaultContactName());
                            }
                        }
                ).show();
                return true;
            case R.id.disable_whatsapp:
                // show Dialog that will lead user to WhatsApp settings to disable ringtones there
                buildWhatsAppDisableDialog().show();
                return true;
            case R.id.action_enable_notification_service:
                // show Dialog that will lead user to settings where he can enable listener service
                buildNotificationServiceAlertDialog();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public BillingManager getBillingManager() { return billingManager; }

    /**
     * Checks for contacts permissions and notification service enabled. If not, prompts user
     * to enable them
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.permissions_not_enabled),
                    Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        else if (!isNotificationServiceEnabled(this)) {
                buildNotificationServiceAlertDialog();
        }
    }

    /**
     * Method callback when user gives (or not gives) permissions that were requested
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            // If request is cancelled, the result arrays are empty.
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this,
                        getString(R.string.permissions_not_enabled),
                        Toast.LENGTH_LONG).show();
            }
        }
        if (!isNotificationServiceEnabled(this))
            buildNotificationServiceAlertDialog();
    }

    /**
     * Handles actions that finished outside of this activity. Handles: 
     * - RESULT_PICK_CONTACT    (user picked contact, Android)
     * - RESULT_PICK_TONE       (user picked ringtone, Android)
     * - RESULT_PICK_VIB        (user picked vibration, VibrationPickerActivity)
     * - RESULT_ENABLED_SERVICE (user enabled NotificationListener service from Android Settings
     *                              and was redirect to MainActivity automatically)
     * - RESULT_SETUP_FIN       (user finished the PermissionsActivity Introduction fragments)
     * - RESULT_BUY_PREMIUM     (user tried to add custom ringtone but is not premium)
     * @param requestCode The action that was requested and has no finished. E.g. Pick Contact, 
     *                    Pick Ringtone, Pick Vibration, etc.
     * @param resultCode The code that determines whether the action was successful
     * @param data Data passed along. For example, which contact was selected, which ringtone, etc.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check whether the result is ok
        if (resultCode == RESULT_OK && requestCode == RESULT_PICK_CONTACT) {
            Uri uri = data.getData();
            Cursor cursor;
            try {
                //Query the content uri
                if (uri == null) throw new Exception("Returned URI is null");

                cursor = getContentResolver().query(
                        uri, null, null, null, null);

                if (cursor == null) throw new Exception("Cursor is null");
                if (cursor.getCount() <= 0) throw new Exception("Cursor's length is not > 0");

                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String photoURI = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));

                CustomRingerPerson person = new CustomRingerPerson();
                person.name = name;
                person.photoURI = photoURI;
                person.ringtoneURI = ContactsManager.getDefaultToneString(this);
                person.messageTimestamps = new ArrayList<>();
                person.vibrateURI = VibrationsManager.DEFAULT_VIBRATION;

                ContactsManager.putContact(this, person);

                mPagerAdapter.setDirty(POSITION_CONTACTS_FRAG);
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_OK && requestCode == RESULT_PICK_TONE) {
            Uri ringtoneURI = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            CustomRingerPerson person = ContactsManager.getContact(this, choosingContact);
            person.ringtoneURI = ringtoneURI != null? ringtoneURI.toString() : ContactsManager.NONE_RINGTONE_ID;
            ContactsManager.putContact(this, person);
            mPagerAdapter.setDirty(POSITION_CONTACTS_FRAG);
            if (choosingContact.equals(ContactsManager.getDefaultContactName()))
                mPagerAdapter.setDirty(POSITION_DEFAULT_FRAG);
        }
        else if (resultCode == RESULT_OK && requestCode == RESULT_PICK_VIB) {
            Uri d = data.getData();
            if (d != null) {
                String newTone = d.toString();
                CustomRingerPerson person = ContactsManager.getContact(this, choosingContact);
                person.vibrateURI = newTone;
                ContactsManager.putContact(this, person);
                mPagerAdapter.setDirty(POSITION_CONTACTS_FRAG);
                if (choosingContact.equals(ContactsManager.getDefaultContactName()))
                    mPagerAdapter.setDirty(POSITION_DEFAULT_FRAG);

                Log.d(TAG, "Successfully aquired and set new uri [" + newTone + "] for vibration for " + choosingContact);
            }
            else {
                Log.d(TAG, "Returned picked virbation pattern uri was null... aborting.");
            }
        }
        else if (requestCode == RESULT_ENABLED_SERVICE) {
            if (!isNotificationServiceEnabled(this)) {
                Toast.makeText(this, getString(R.string.warning_service_not_enabled), Toast.LENGTH_LONG).show();
            } else {
                AlertDialog disableWhatsApp = buildWhatsAppDisableDialog();
                disableWhatsApp.show();
            }
        }
        else if (requestCode == RESULT_SETUP_FIN) {
            Log.i(TAG, "Setup complete. First time load: default ringtone / vib contact does not exist. Adding system default.");
        }
        else {
            Log.e("ContactFragment", "Failed to pick contact");
        }

        if (resultCode == RESULT_BUY_PREMIUM) {
            informPremium();
            showSnackbar(R.string.not_premium_no_action, Snackbar.LENGTH_LONG);
        }
    }

    /**
     * Start the activity flow that enables user to pick a ringtone. Once this ringtone has been 
     * picked, it will be assigned to nameOfContact. 
     * @param nameOfContact The contact for whom the user is choosing a new ringtone
     */
    public void pickRingtone(String nameOfContact) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                Uri.parse(ContactsManager.getContact(getApplicationContext(), nameOfContact).ringtoneURI));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        choosingContact = nameOfContact;
        startActivityForResult(intent, RESULT_PICK_TONE);
    }

    /**
     * Start the activity flow that enables user to pick a vibration pattern. Once this vib has been 
     * picked, it will be assigned to nameOfContact. 
     * @param nameOfContact The contact for whom the user is choosing a new vibration pattern
     */
    public void pickVibrationPattern(String nameOfContact) {
        Intent intent = new Intent(getApplicationContext(), VibrationPickerActivity.class);
        intent.putExtra("CurrentPattern",
                ContactsManager.getContact(getApplicationContext(), nameOfContact).vibrateURI);
        intent.putExtra("EnableCustom", billingManager.isPremium());
        choosingContact = nameOfContact;
        startActivityForResult(intent, RESULT_PICK_VIB);
    }


    /**
     * Is Notification Service Enabled. 
     * Verifies if the notification listener service is enabled.
     */
    public static boolean isNotificationServiceEnabled(Context ctx){
        String pkgName = ctx.getPackageName();
        final String flat = Settings.Secure.getString(ctx.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Build an Alert Dialog that redirects users to WhatsApp settings menu. Used in
     * PermissionsActivity and when user clicks on the setting in option menu
     */
    private AlertDialog buildWhatsAppDisableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.disable_whatsapp_ringtones_tit));
        builder.setMessage(getString(R.string.disable_whatsapp_ringtones_msg));
        builder.setPositiveButton(getString(R.string.disable_whatsapp_ringtones_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!isPackageInstalled(getApplicationContext(), WHATSAPP_PACKAGE)) {
                    Toast.makeText(getApplicationContext(), "WhatsApp is not installed... Please install Whatsapp!", Toast.LENGTH_LONG).show();
                    onActivityResult(RESULT_WHATSAPP_DISABLED, 0, null);
                    return;
                }
                Toast.makeText(MainActivity.this, getString(R.string.disable_whatsapp_toast_inst), Toast.LENGTH_LONG).show();
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.NOTIFICATION_PREFERENCES");
                intent.setComponent(new ComponentName(WHATSAPP_PACKAGE, WHATSAPP_SETTINGS));
                startActivity(intent);
            }
        });
        return builder.create();
    }

    /**
     * Build an Alert Dialog that redirects users to Android Service settings menu so that he can 
     * enable the NotificationListener. Used in PermissionsActivity or when permissions are not enabled.
     */
    private void buildNotificationServiceAlertDialog(){
        if (promptingNotificationListener)
            return;
        promptingNotificationListener = true;
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogCustom));
        alertDialogBuilder.setTitle(getString(R.string.enable_service_tit));
        alertDialogBuilder.setMessage(getString(R.string.enable_service_msg));
        alertDialogBuilder.setPositiveButton(getString(R.string.enable_service_btn),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                promptNotificationSettings(MainActivity.this, MainActivity.class);
            }
        });
        alertDialogBuilder.create().show();
    }

    /**
     * Opens Android notification listener settings so that user can enable NotifyWho service there.
     * Automatically redirects back to app once enabled.
     * TODO: User reported that automatic redirect from Settings doesn't work on Redmi Phone
     */
    public static void promptNotificationSettings(final Context packageContext, final Class classs) {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                | Intent.FLAG_ACTIVITY_NO_HISTORY 
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // check every 500ms if settings are enabled yet
        final Handler handler = new Handler();
        final Runnable checkSettings = new Runnable() {
            @Override
            public void run() {
                if (isNotificationServiceEnabled(packageContext)) {
                    Intent mainIntent = new Intent(packageContext, classs);
                    mainIntent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainIntent.putExtra(SERVICE_ENABLED_FINISHED, RESULT_ENABLED_SERVICE);
                    packageContext.startActivity(mainIntent);
                    return;
                }
                handler.postDelayed(this, 500);
            }
        };
        handler.postDelayed(checkSettings, 1000);

        packageContext.startActivity(intent);
    }

    /**
     * Build a general Alert Dialog that displays some information. 
     * @param messageId the string resource id 
     * @param positiveBtn the string of what the button should say
     * @param positiveAction OnClickListener that triggers when user presses positiveBtn
     * @param title The dialog title
     */
    public AlertDialog buildInformationDialog(String title, int messageId, String positiveBtn,
                                               DialogInterface.OnClickListener positiveAction){
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogCustom));
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(messageId);
        alertDialogBuilder.setPositiveButton(positiveBtn, positiveAction);
        return (alertDialogBuilder.create());
    }

    /**
     * Informs the user about buying premium. Wrapper around buildInformationDialog specifically
     * for premium coersion.
     */
    private void informPremium() {
        buildInformationDialog(getString(R.string.action_buy_premium_txt),
                R.string.advertise_premium_help_msg, getString(R.string.about_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        billingManager.buyPremium();
                    }
                }
        ).show();
    }

    /**
     * Shows a snackbar in activity
     * @param str Literal string to display
     * @param length Time to show snackbar. E.g. Snackbar.LENGTH_LONG
     */
    public void showSnackbar(String str, int length) {
        Snackbar.make(findViewById(R.id.mainActivity_coordinatorLayout),
                str,
                length).show();
    }

    /**
     * Shows a snackbar in activity
     * @param id ID of strings.xml resource
     * @param length Time to show snackbar. E.g. Snackbar.LENGTH_LONG
     */
    public void showSnackbar(int id, int length) {
        Snackbar.make(findViewById(R.id.mainActivity_coordinatorLayout),
                getString(id),
                length).show();
    }
}
