package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "ReminderPrefs";
    private static final String TASKS_KEY = "saved_tasks";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule all saved reminders
            rescheduleReminders(context);
        }
    }

    private void rescheduleReminders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String tasksJson = prefs.getString(TASKS_KEY, "[]");

        try {
            JSONArray tasksArray = new JSONArray(tasksJson);
            List<TaskItem> tasks = new ArrayList<>();

            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskObj = tasksArray.getJSONObject(i);
                String task = taskObj.getString("task");
                String time = taskObj.getString("time");
                tasks.add(new TaskItem(task, time));
            }

            // Create an intent to reschedule reminders
            Intent rescheduleIntent = new Intent(context, MainActivity.class);
            rescheduleIntent.setAction("RESCHEDULE_REMINDERS");
            rescheduleIntent.putExtra("tasks", tasksJson);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(rescheduleIntent);
            } else {
                context.startService(rescheduleIntent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
} 