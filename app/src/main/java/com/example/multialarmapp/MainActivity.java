package com.example.multialarmapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private EditText numberOfAlarms, timeMinutes, timeSeconds;
    private AlarmManager alarmManager;
    private final HashMap<Integer, Long> alarmTriggerTimes = new HashMap<>();
    private final HashMap<Integer, PendingIntent> activeAlarms = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private Timer timer;
    private ArrayList<String> alarmDisplayList;
    private final HashMap<Integer, Integer> alarmIndexMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numberOfAlarms = findViewById(R.id.number_of_alarm_you_want_to_set);
        timeMinutes = findViewById(R.id.time_in_minutes);
        timeSeconds = findViewById(R.id.time_in_seconds);
        Button btnSetAlarms = findViewById(R.id.set_alarms);
        Button btnStopAllAlarms = findViewById(R.id.stop_all_alarms);
        ListView alarmListView = findViewById(R.id.alarm_list_view);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        alarmDisplayList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alarmDisplayList);
        alarmListView.setAdapter(adapter);

        checkExactAlarmPermission();

        btnSetAlarms.setOnClickListener(v -> setMultipleAlarms());
        btnStopAllAlarms.setOnClickListener(v -> cancelAllAlarms());
        alarmListView.setOnItemClickListener((parent, view, position, id) -> stopSpecificAlarm(position));

        startCountdownTimer();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please allow exact alarms in settings", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setMultipleAlarms() {
        String numAlarmsStr = numberOfAlarms.getText().toString();
        String minutesStr = timeMinutes.getText().toString();
        String secondsStr = timeSeconds.getText().toString();

        if (numAlarmsStr.isEmpty() || minutesStr.isEmpty() || secondsStr.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int numAlarms = Integer.parseInt(numAlarmsStr);
        int minutes = Integer.parseInt(minutesStr);
        int seconds = Integer.parseInt(secondsStr);

        if (minutes > 59 || seconds > 59) {
            Toast.makeText(this, "Minutes and seconds must be between 0-59", Toast.LENGTH_SHORT).show();
            return;
        }

        int nextAlarmId = alarmIndexMap.size() + 1;
        for (int i = 0; i < numAlarms; i++) {
            int alarmId = nextAlarmId++;
            int delay = (i + 1) * (minutes * 60 + seconds) * 1000;
            long triggerTime = System.currentTimeMillis() + delay;
            setAlarm(triggerTime, alarmId);
        }
    }

    private void setAlarm(long triggerTime, int alarmId) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        activeAlarms.put(alarmId, pendingIntent);
        alarmTriggerTimes.put(alarmId, triggerTime);
        alarmIndexMap.put(alarmId, alarmDisplayList.size());

        alarmDisplayList.add("Alarm " + alarmId + " will ring in calculating...");
        adapter.notifyDataSetChanged();

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    private void stopSpecificAlarm(int position) {
        if (position >= 0 && position < alarmDisplayList.size()) {
            Integer alarmId = null;

            for (Map.Entry<Integer, Integer> entry : alarmIndexMap.entrySet()) {
                if (entry.getValue().equals(position)) {
                    alarmId = entry.getKey();
                    break;
                }
            }

            if (alarmId != null && activeAlarms.containsKey(alarmId)) {
                PendingIntent pendingIntent = activeAlarms.get(alarmId);
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    activeAlarms.remove(alarmId);
                    AlarmReceiver.stopAlarm();
                }
                alarmTriggerTimes.remove(alarmId);
                alarmDisplayList.set(position, "Alarm " + alarmId + " (Canceled)");
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Alarm " + alarmId + " canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cancelAllAlarms() {
        for (PendingIntent pendingIntent : activeAlarms.values()) {
            alarmManager.cancel(pendingIntent);
        }
        activeAlarms.clear();
        alarmTriggerTimes.clear();
        alarmIndexMap.clear();
        alarmDisplayList.clear();
        adapter.notifyDataSetChanged();
        AlarmReceiver.stopAlarm();
        Toast.makeText(this, "All alarms canceled", Toast.LENGTH_SHORT).show();
    }

    private void startCountdownTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateAlarmList());
            }
        }, 0, 1000);
    }

    private void updateAlarmList() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : alarmTriggerTimes.entrySet()) {
            int alarmId = entry.getKey();
            long remainingTime = entry.getValue() - currentTime;
            if (remainingTime > 0) {
                long minutes = (remainingTime / 1000) / 60;
                long seconds = (remainingTime / 1000) % 60;
                int position = alarmIndexMap.get(alarmId);
                alarmDisplayList.set(position, "Alarm " + alarmId + " will ring in " + minutes + " min " + seconds + " sec");
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllAlarms();
        if (timer != null) {
            timer.cancel();
        }
    }
}
