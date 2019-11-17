package com.tzgames.ringer.services;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.tzgames.ringer.data.ContactsManager;
import com.tzgames.ringer.data.ContactsManager.CustomRingerPerson;
import com.tzgames.ringer.data.VibrationsManager;

import static android.app.Notification.EXTRA_TITLE;
import static com.tzgames.ringer.activities.MainActivity.WHATSAPP_PACKAGE;

/**
 * Sticky bckground service that scans user notification and checks if they are from Whatsapp.
 * NotificationListener listens if notification was received from a custom contact, and if so
 * plays appropriate sounds / vibrations.
 */
public class Intercepter extends NotificationListenerService {
    /** Log tag */
    private static final String TAG = "Intercepter";

    /** Time since last default notification was played. Used to prevent notification spam */
    private long lastUnknownNotificationTime = 0;

    /**
     * Called when service starts. By returning START_STICKY, ensure that this service always runs,
     * and restarts itself if stopped.
     * @return START_STICKY
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    /**
     * Method that handles incoming notifications. If received from Whatsapp, check the sender's
     * name against all custom contacts, and play appropriate notification / vibration patterns
     * @param sbn The StatusBarNotification that was received
     */
    @Override
    public void onNotificationPosted(StatusBarNotification sbn){

        // Notification arrive from WhatsApp
        if (sbn.getPackageName().equals(WHATSAPP_PACKAGE)) {
            Long msgLongDate = sbn.getNotification().when;
            String msgFrom = sbn.getNotification().extras.getString(EXTRA_TITLE);

            // No message from, exiting
            if (msgFrom == null) return;

            // Message is from whatsapp itself, not from user. exiting
            else if (msgFrom.toLowerCase().contains("whatsapp")) return;

            try {
                // Time since last played notification sound. We don't want to spam!
                long dt = System.currentTimeMillis() - lastUnknownNotificationTime;

                // Try to get the CustomRingerPerson
                CustomRingerPerson person = ContactsManager.getContact(this, msgFrom);

                // Person does not have a custom ringtone assigned, so play default sound.
                if (person == null) {
                    if (dt < 800) return;
                    lastUnknownNotificationTime = System.currentTimeMillis();
                    playDefaultTone();
                    return;
                }

                // Check if ringtone was already played for that person
                for (Long time : person.messageTimestamps) {
                    if (time.equals(msgLongDate)) {
                        Log.d(TAG, "Message already exists. Disregarding.");
                        return;
                    }
                }

                // Check if last message from this person was longer than 500ms ago
                if (person.messageTimestamps.size() > 0 && System.currentTimeMillis() -
                        person.messageTimestamps.get(person.messageTimestamps.size() - 1) < 500)
                    return;

                // add timestamp to person and update him
                person.messageTimestamps.add(msgLongDate);
                ContactsManager.putContact(this, person);

                // Play vibration and notification
                playNotificationTone(person.ringtoneURI);
                VibrationsManager.vibrateByName(this, person.vibrateURI);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /**
     * Plays the default notification and vibration pattern
     */
    private void playDefaultTone() {
        playNotificationTone(ContactsManager.getDefaultToneString(this));
        VibrationsManager.vibrateByName(this, "Default");
    }

    /**
     * Plays a given notification tone
     * @param tone The name of the tone to play
     */
    private void playNotificationTone(String tone) {
        Ringtone player = RingtoneManager.getRingtone(this, Uri.parse(tone));
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        player.setAudioAttributes(attributes);
        player.play();
    }
}
