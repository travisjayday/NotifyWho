package com.tzgames.ringer.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;

import com.tzgames.ringer.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class in charge of vibrations. Maintains two lists:
 *      - customVibrations : user defined / created vibrations that are saved to disk
 *      - vibrations : the collection of default vibrations AND customVibrations. Not saved to disk.
 * This class is used to add vibrations, remove vibrations, and to actually trigger vibrations.
 */
public class VibrationsManager {
    /** Debug Tag */
    private static final String TAG = "VibrationPatternC";

    /** Delay after which to start playing all vibrations */
    private static final int DELAY = 100;

    /** Name of None vibration, i.e. no vibration set */
    public static final String NONE_VIBRATION = "None";

    /** Name of default vibration */
    public static final String DEFAULT_VIBRATION = "Default";

    /** Directory where app data is stored */
    private static final String DATADIR = "/data";

    /** File where custom vibrations are stored */
    private static final String DATAFILE = DATADIR + "/vibrations.bin";

    /** Read / Write lock */
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    /** HashMap that stores custom user vibrations */
    private static LinkedHashMap<String, VibrationPattern> customVibrations;

    /** HashMap that stores default vibrations. Initialized with all default vibration patterns */
    private static final LinkedHashMap<String, VibrationPattern> vibrations = new LinkedHashMap<String, VibrationPattern>(){{
            put(DEFAULT_VIBRATION,      new VibrationPattern(new long[]{DELAY, 250, 250, 250}));
            put(NONE_VIBRATION,         new VibrationPattern(new long[]{}));
            put("Short",                new VibrationPattern(new long[]{DELAY, 300}));
            put("Medium",               new VibrationPattern(new long[]{DELAY, 500}));
            put("Long",                 new VibrationPattern(new long[]{DELAY, 1200}));
            put("Short Double Skip",    new VibrationPattern(new long[]{DELAY, 150, 150, 150}));
            put("Double Skip",          new VibrationPattern(new long[]{DELAY, 300, 300, 300}));
            put("Short Multi Skip",     new VibrationPattern(new long[]{DELAY, 200, 50, 200, 100, 200, 150, 200}));
            put("Multi Skip",           new VibrationPattern(new long[]{DELAY, 300, 75, 300, 150, 300, 175, 300}));
            put("Skippidy Skip",        new VibrationPattern(new long[]{DELAY, 300, 150, 200, 200, 500, 50, 100}));
            put("Short Short Long",     new VibrationPattern(new long[]{DELAY, 70, 70, 70, 55, 70, 55, 625}));
            put("Long Short Short",     new VibrationPattern(new long[]{DELAY, 220, 90, 60, 75, 70, 75, 60}));
            put("Staccato",             new VibrationPattern(new long[]{DELAY, 70, 70, 70, 70, 70, 70, 70, 70}));
            put("Double Staccato",      new VibrationPattern(new long[]{DELAY, 75, 85, 60, 70, 70, 50, 50, 430, 70, 90, 70, 70, 60, 50, 50}));
    }};

    /**
     * Simple vibrateion pattern object that can be serialized and written to disk.
     */
    private static class VibrationPattern implements Serializable {
        final long[] timestamps;
        VibrationPattern(long[] _timestamps) {
            timestamps = _timestamps;
        }
    }

    /**
     *  Load data file that contains custom vibrations from disk; create it if not exist
      */
    @SuppressWarnings("unchecked")
    private static void readDataFile(Context context) {
        lock.readLock().lock();
        try {
            File dataDir = new File (context.getExternalFilesDir(null) + DATADIR);
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    throw new Exception("Failed to create data dir: " + dataDir.getAbsolutePath());
                }
            }

            File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);

            if (dataFile.exists()) {
                // Read data file from disk and store it into hashmap
                FileInputStream inputStream = new FileInputStream(dataFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                customVibrations = (LinkedHashMap<String, VibrationPattern>) (objectInputStream.readObject());
                objectInputStream.close();
                inputStream.close();

                // append custom vibrations to the overall vibration list
                for (Map.Entry<String, VibrationPattern> entry : customVibrations.entrySet()) {
                    vibrations.put(entry.getKey(), entry.getValue());
                }
            } else {
                // Custom vibration file not found, intiialize empty hashmap
                customVibrations = new LinkedHashMap<>();
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.toString());
            Log.e(TAG, "Vibrations data file is corrupt. Trying to delete it...");
            File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);
            if (dataFile.delete()) Log.i(TAG, "Data file deleted succesfully!");
            customVibrations = new LinkedHashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets notificattion channels for each vibration in vibrations list. Each vibration will
     * get its own, named notification channel IF SDK >= 26. If SDK < 26, skip this function, and
     * vibrations will be played the old way.
     */
    public static void genNotificationChannels(Context context) {

        // don't use notification channels if SDK < 26
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mNotificationManager == null) {
            Log.e(TAG, "Failed to get notification manager...");
            return;
        }

        for (String s : vibrations.keySet()) {
            VibrationPattern pattern = getVibrations(context).get(s);
            if (pattern == null) continue;
            NotificationChannel channel = new NotificationChannel(s, s,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Vibration: " + s);
            channel.setVibrationPattern(pattern.timestamps);
            channel.setSound(null, null);
            channel.enableVibration(true);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Private method that saves the datafile of custom vibrations to disk.
     */
    private static void writeDataFile(Context context) {
        lock.writeLock().lock();
        File dataFile = new File(context.getExternalFilesDir(null), DATAFILE);
        try {
            FileOutputStream outputStream = new FileOutputStream(dataFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(customVibrations);
            objectOutputStream.close();
            outputStream.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Function to add a new custom vibration pattern to the custom vibration pattern list.
     * @param name Name of the custom vibration pattern that should be added
     * @param times The actual long[] times that define the pattern
     * @return True if the custom vibration pattern can be added. False, otherwise.
     */
    public static boolean addCustomVibration(Context ctx, String name, long[] times) {
        if (customVibrations == null) readDataFile(ctx);

        // check for duplicate name entries
        for (String s : vibrations.keySet()) {
            if (name.equals(s)) return false;
        }
        for (String s : customVibrations.keySet()) {
            if (name.equals(s)) return false;
        }

        // add vibration pattern
        VibrationPattern vib = new VibrationPattern(times);
        customVibrations.put(name, vib);
        vibrations.put(name, vib);
        genNotificationChannels(ctx);
        writeDataFile(ctx);
        return true;
    }

    /**
     * Removes a custom vibration from customVibration list, from vibrations list, and from any
     * contact who has the vibration assigned IFF the given vibration is a custom vibration.
     * vibration.
     * @param name Name of vibration to remove
     * @return True if removed succesfully. False, if the vibration pattern is not a custom vibration.
     */
    public static boolean removeCustomVibration(Context ctx, String  name) {
        if (customVibrations.remove(name) != null) {
            vibrations.remove(name);
            writeDataFile(ctx);
            ContactsManager.removeVibrationFromAllContacts(ctx, name);
            return true;
        }
        return false;
    }

    /**
     * Returns the list of all current vibrations
     * @return HashMap<Name of Vibration, VibrationPattern> of all vibrations
     */
    @NonNull
    public static HashMap<String, VibrationPattern> getVibrations(Context ctx) {
        if (customVibrations == null)
            readDataFile(ctx);
        return vibrations;
    }

    /**
     * Plays the vibration given a vibratePattern object. Uses notification channels if SDK >= 26,
     * uses deprecatd vibrate method otherwise.
     * @param vibrateName The name of the vibration Pattern to be played
     */
    public static void vibrateByName(Context context, String vibrateName) {
        if (!vibrateName.equals(VibrationsManager.NONE_VIBRATION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, vibrateName)
                        .setSmallIcon(R.drawable.ic_notifications_active_black_24dp)
                        .setTimeoutAfter(3000)
                        .setAutoCancel(true);
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.notify(1, builder.build());
            }
            else {
                VibrationPattern pattern = getVibrations(context).get(vibrateName);
                if (pattern != null)
                    vibrate(context, pattern.timestamps);
            }
        }
    }

    /**
     * Deprecated. Plays the vibration given a long pattern. Used for SDK < 26 where notification
     * channels aren't available yet. Uses Vibrator service.
     * @param pattern Pattern to play. Array of longs.
     */
    public static void vibrate(Context context, long[] pattern) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, VibrationEffect.DEFAULT_AMPLITUDE)
                );
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
        else{
            Log.e(TAG, "No vibrator found!");
        }
    }

    /**
     * Vibrate indefinitely until cancleVibrate() is called. Used for generating new vibration patterns.
     */
    public static void vibrateForever(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                        VibrationEffect.createOneShot(10000, VibrationEffect.DEFAULT_AMPLITUDE)
                );
            } else {
                vibrator.vibrate(10000);
            }
        }
        else{
            Log.e(TAG, "No vibrator found!");
        }
    }

    /**
     * Cancel any vibration that is currently playing.
     */
    public static void cancelVibrate(Context ctx) {
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.cancel();
        }
        else{
            Log.e(TAG, "No vibrator found!");
        }
    }
}
