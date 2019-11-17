package com.tzgames.ringer.data;

import android.content.Context;
import android.media.RingtoneManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class that keeps track of which ringtones / vibrations have been assigned to which contacts.
 * Saves and retrieves data to disk on app load to permanently keep track of contacts / tones.
 *
 * A custom contact is abstracted with CustomRingerPerson class.
 *
 * In addition to maintaining user selected contacts, this class maintains a DEFAULT_CONTACT_ID
 * who is assigned default ringtones and is hidden from the user's custom contacts.
 */
public class ContactsManager {
    private static final String TAG = "ContactsManager";
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static LinkedHashMap<String, CustomRingerPerson> contactList = null;
    private static final String DATAFILE = "/data/data7.bin";
    private static final String DEFAULT_CONTACT_ID = "__DEFAULT_RINGTONE__83242088AA";
    public static final String NONE_RINGTONE_ID = "No Tone (Silent)";

    /**
     * Class that represents a CustomRingerPerson that has several attributes and can be serialized
     */
    public static class CustomRingerPerson implements Serializable {
        /** Name of contact person */
        public String name;

        /** The profile pic (stored in Contacts) from person */
        public String photoURI;

        /** The custom ringtone URI that plays when user gets message from this person */
        public String ringtoneURI;

        /** The custom vibrate URI that plays when user gets message from this person */
        public String vibrateURI;

        /** The times at which user received messages from this person. */
        public ArrayList<Long> messageTimestamps;
    }

    /**
     * Internal method used to save the current set of CustomRingerPerson 's
     */
    private static void writeDataFile(Context context) {
        lock.writeLock().lock();
        try {
            File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);
            FileOutputStream outputStream = new FileOutputStream(dataFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(contactList);
            objectOutputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Return the name of the default contact (with default ringtone / vib pattern)
     */
    public static String getDefaultContactName() {
        return DEFAULT_CONTACT_ID;
    }

    /**
     * Check whether the user added contacts already. If not known, read datafile to cache contacts.
     * @return True if contacts list is empty. False, otherwise.
     */
    public static boolean isEmptyContacts(Context ctx) {
        if (contactList == null) readDataFile(ctx);
        return contactList.size() <= 1;     // one is empty since default ringer takes up 1 space
    }

    /**
     * Add a new CustomRingerPerson to the current list of contacts. Then save to disk.
     * @param person CustomRingerPerson to add to current contacts list
     */
    public static void putContact(Context ctx, CustomRingerPerson person) {
        if (contactList == null) readDataFile(ctx);
        contactList.put(person.name, person);
        writeDataFile(ctx);
    }

    /**
     * Remove a CustomRingerPerson by name from the current list of contacts. Then save to disk.
     * @param name The name of the contact to remove
     */
    public static void removeContact(Context ctx, String name) {
        if (contactList == null) readDataFile(ctx);
        contactList.remove(name);
        writeDataFile(ctx);
    }

    /**
     * Call method if default contact does not exist. This method creates a default contact if
     * not already exist, then assigned him the default tone & vib pattern
     */
    private static void addDefaultContact(Context ctx) {
        CustomRingerPerson def = getContact(ctx, DEFAULT_CONTACT_ID);
        if (def == null) {
            // Default person doesn't exist yet, so add him.
            def = new CustomRingerPerson();
            def.name = DEFAULT_CONTACT_ID;
        }
        def.ringtoneURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString();
        def.vibrateURI = VibrationsManager.DEFAULT_VIBRATION;
        putContact(ctx, def);
    }

    /**
     * Replaces a given vibration with the default vibration for all contacts. Called when a
     * vibration needs to be removed permanently.
     * @param vib The vibration name to remove
     */
    static void removeVibrationFromAllContacts(Context ctx, String vib) {
        if (contactList == null) readDataFile(ctx);
        for (Map.Entry<String, CustomRingerPerson> entry : contactList.entrySet()) {
            if (entry.getValue().vibrateURI.equals(vib))
                entry.getValue().vibrateURI = VibrationsManager.DEFAULT_VIBRATION;
        }
        writeDataFile(ctx);
    }

    /**
     * Returns the number of custom contacts that have a vibration assigned to them (other than
     * default vibration or default contact person).
     */
    public static int countContactsWithVibrations(Context ctx) {
        if (contactList == null) readDataFile(ctx);
        int i = 0;
        for (Map.Entry<String, CustomRingerPerson> entry : contactList.entrySet()) {
            if (entry.getKey().equals(DEFAULT_CONTACT_ID)) continue;
            if (!entry.getValue().vibrateURI.equals(VibrationsManager.DEFAULT_VIBRATION))
                i++;
        }
        return i;
    }

    /**
     * Returns the current default ringtone uri (from DEFAULT_CONTACT_ID person)
     */
    public static String getDefaultToneString(Context ctx) {
        CustomRingerPerson def = getContact(ctx, DEFAULT_CONTACT_ID);
        if (def == null || def.ringtoneURI.equals("") || def.vibrateURI.equals("")) {
            addDefaultContact(ctx);
            def = getContact(ctx, DEFAULT_CONTACT_ID);
        }
        return def.ringtoneURI;
    }

    /**
     *  Returns the current default vibrate uri (from DEFAULT_CONTACT_ID person)
     */
    public static String getDefaultVibString(Context ctx) {
        CustomRingerPerson def = getContact(ctx, DEFAULT_CONTACT_ID);
        if (def == null || def.ringtoneURI.equals("") || def.vibrateURI.equals("")) {
            addDefaultContact(ctx);
            def = getContact(ctx, DEFAULT_CONTACT_ID);
        }
        return def.vibrateURI;
    }

    /**
     *  Returns a list of all names that have been given a custom ringtone
     *  (excluding DEFAULT_CONTACT_ID person)
     */
    public static List<String> getAllContactNames(Context ctx) {
        if (contactList == null) readDataFile(ctx);
        List<String> c = new ArrayList<>(contactList.keySet());
        c.remove(DEFAULT_CONTACT_ID);
        return c;
    }

    /**
     * Gets a custom a CustomRingerPerson by name.
     * @param name The name of the CustomRingerPerson to get
     * @return CustomRingerPerson. null if the person has no custom ringtone assigned
     */
    public static CustomRingerPerson getContact(Context ctx, String name) {
        if (contactList == null) readDataFile(ctx);
        return contactList.get(name);
    }

    /**
     * Loads the data file (with contacts) from disk; Creates it if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    private static void readDataFile(Context context) {
        lock.readLock().lock();
        try {
            File dataDir = new File (context.getExternalFilesDir(null) + "/data");
            if (!dataDir.exists()) {
                // directory does not exist, so create it
                if (!dataDir.mkdirs()) {
                    Log.e(TAG, "Failed to create data dir: " + dataDir.getAbsolutePath());
                }
            }

            File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);

            if (dataFile.exists()) {
                // Read contents of datafile into contacts list
                FileInputStream inputStream = new FileInputStream(dataFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                contactList = (LinkedHashMap<String, CustomRingerPerson>) (objectInputStream.readObject());
                objectInputStream.close();
                inputStream.close();
            } else {
                // data file does not exist, so create an empty list from scratch
                contactList = new LinkedHashMap<>();
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            Log.e(TAG, "Contacts data file is corrupt. Trying to delete it...");
            File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);
            if (dataFile.delete()) {
                Log.i(TAG, "Data file deleted succesfully!");
            }
            contactList = new LinkedHashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }
}
