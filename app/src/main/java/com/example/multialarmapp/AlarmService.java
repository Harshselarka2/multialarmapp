package com.example.multialarmapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.util.HashMap;

public class AlarmService extends Service {
    private static AlarmManager alarmManager;
    private static final HashMap<Integer, PendingIntent> activeAlarms = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Stop all alarms when the app is removed from recent apps
        for (PendingIntent pendingIntent : activeAlarms.values()) {
            alarmManager.cancel(pendingIntent);
        }
        activeAlarms.clear();
        Toast.makeText(this, "All alarms stopped after app was closed", Toast.LENGTH_SHORT).show();

        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void addAlarm(int alarmId, PendingIntent pendingIntent) {
        activeAlarms.put(alarmId, pendingIntent);
    }

    public static void removeAlarm(int alarmId) {
        activeAlarms.remove(alarmId);
    }
}
