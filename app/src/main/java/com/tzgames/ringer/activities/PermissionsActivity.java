package com.tzgames.ringer.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.tzgames.ringer.R;
import com.tzgames.ringer.views.IntroViewPager;

import static com.tzgames.ringer.activities.MainActivity.PERMISSIONS_REQUEST_READ_CONTACTS;
import static com.tzgames.ringer.activities.MainActivity.RESULT_ENABLED_SERVICE;
import static com.tzgames.ringer.activities.MainActivity.RESULT_WHATSAPP_DISABLED;
import static com.tzgames.ringer.activities.MainActivity.SERVICE_ENABLED_FINISHED;
import static com.tzgames.ringer.activities.MainActivity.WHATSAPP_PACKAGE;
import static com.tzgames.ringer.activities.MainActivity.WHATSAPP_SETTINGS;

/**
 * PermissionsActivity is launched on the first app launch to make the user grant permissions /
 * enable NotificationListener service permissions. It also gives user information about NotifyWho.
 * The flow is
 *      PermissionsFragment -> NotificationListenerFragment -> WhatsappDisableFragment -> FinishFragment
 * Each of those fragments has a button and the callback of that button lives in this class, so
 * that this class can keep track of on which screen the user is and where he has to go.
 */
public class PermissionsActivity extends AppCompatActivity {

    /** PermissionsActivity ViewPager */
    private IntroViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        mPager = findViewById(R.id.permissions_ViewPager);
        mPager.attachAdapter(getSupportFragmentManager());
    }

    /*
     * This overrides the original intent due to onResume and check if user enabled service
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Updates intent. When user returns from Android Settings, a key with name SERVICE_ENABLED_FINISHED
     * gets passed in order to check if the user is coming from the settings and has enabled the service.
     * If SERVICE_ENABLED_FINISHED exists, then the user has enabled service, so move to next screen.
     */
    @Override
    public void onResume() {
        super.onResume();
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(SERVICE_ENABLED_FINISHED)) {
            onActivityResult(extras.getInt(SERVICE_ENABLED_FINISHED), RESULT_OK, null);
            getIntent().removeExtra(SERVICE_ENABLED_FINISHED);
        }
    }

    /**
     * Callback for buttonClick of PermissionsFragment. Requests Android Read Contact Permissions
     */
    public void permissionsFragmentClick() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    /**
     * Callback for buttonClick of NotificationListenerFragment. Launches Android Settings to enable service.
     */
    public void notificationFragmentClick() {
        MainActivity.promptNotificationSettings(PermissionsActivity.this, PermissionsActivity.class);
    }

    /**
     * Callback for buttonClick of WhatsappDisableFragment. Launches Whatsapp and prompts to disable.
     */
    // TODO: Add whatsapp not installed, android settings uri, whatsapp package to strings.xml
    public void whatsappFragmentClick() {
        if (!isPackageInstalled(this, WHATSAPP_PACKAGE)) {
            Toast.makeText(this, "WhatsApp is not installed... Please install Whatsapp!", Toast.LENGTH_LONG).show();
            onActivityResult(RESULT_WHATSAPP_DISABLED, 0, null);
            return;
        }
        Toast.makeText(PermissionsActivity.this, getString(R.string.disable_whatsapp_toast_inst), Toast.LENGTH_LONG).show();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.NOTIFICATION_PREFERENCES");
        intent.setComponent(new ComponentName(WHATSAPP_PACKAGE, WHATSAPP_SETTINGS));
        startActivityForResult(intent, RESULT_WHATSAPP_DISABLED);
    }

    /**
     * Callback for buttonClick of FinishFragment. Saves previously_started bool and finishes.
     */
    public void finishFragmentClick() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(getString(R.string.pref_previously_started), Boolean.TRUE);
        edit.apply();
        finish();
    }

    /**
     * Checks whether a given package is installed or not.
     * @param context A Context
     * @param packageName The package name to check
     */
    public static boolean isPackageInstalled(@NonNull Context context, @NonNull String packageName) {
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            try {
                pm.getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * Method callback when user gives (or not gives) permissions that were requested. After
     * permissions are acquired, launch next screen: showNotificationManager
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
        mPager.arrowScroll(View.FOCUS_RIGHT);           // Scroll to NotificationListenerFragment page
    }

    /**
     * Method Called when some activity returns to this one. Namely, when user enabled
     * NotifyWho service or disabled WhatsApp sounds
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_ENABLED_SERVICE) {
            if (MainActivity.isNotificationServiceEnabled(this))
                mPager.arrowScroll(View.FOCUS_RIGHT);   // Scroll to WhatsappDisableFragment page
            else
                Toast.makeText(this, getString(R.string.notif_not_enabled), Toast.LENGTH_LONG).show();
        }
        else if (requestCode == RESULT_WHATSAPP_DISABLED) {
            mPager.arrowScroll(View.FOCUS_RIGHT);       // Scroll to FinishFragment page
        }
    }
}
