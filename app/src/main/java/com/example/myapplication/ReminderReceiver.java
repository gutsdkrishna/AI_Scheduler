package com.example.myapplication;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int PRE_NOTIFICATION_ID = 2;
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String task = intent.getStringExtra("task");
        String time = intent.getStringExtra("time");
        boolean isPreNotification = intent.getBooleanExtra("is_pre_notification", false);
        String recurrenceTypeStr = intent.getStringExtra("recurrence_type");
        int recurrenceId = intent.getIntExtra("recurrence_id", 0);

        TaskItem.RecurrenceType recurrenceType = TaskItem.RecurrenceType.valueOf(recurrenceTypeStr);

        // Handle action buttons first
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case "COMPLETE_TASK":
                    handleTaskCompletion(context, task, time, recurrenceId);
                    return;
                case "DELETE_TASK":
                    handleTaskDeletion(context, task, time, recurrenceId);
                    return;
                case "SNOOZE_TASK":
                    handleTaskSnooze(context, task, time, recurrenceId);
                    return;
            }
        }

        // Create notification channel for Android 8.0 and above
        createNotificationChannel(context);

        // Build and show notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(isPreNotification ? "Upcoming Reminder" : "Reminder")
            .setContentText(isPreNotification ? 
                task + " is scheduled in 1 minute" : 
                task)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        // Add action buttons
        Intent completeIntent = new Intent(context, ReminderReceiver.class);
        completeIntent.setAction("COMPLETE_TASK");
        completeIntent.putExtra("task", task);
        completeIntent.putExtra("time", time);
        completeIntent.putExtra("recurrence_id", recurrenceId);
        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
            context, 
            recurrenceId, 
            completeIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent deleteIntent = new Intent(context, ReminderReceiver.class);
        deleteIntent.setAction("DELETE_TASK");
        deleteIntent.putExtra("task", task);
        deleteIntent.putExtra("time", time);
        deleteIntent.putExtra("recurrence_id", recurrenceId);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
            context, 
            recurrenceId + 1, 
            deleteIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent snoozeIntent = new Intent(context, ReminderReceiver.class);
        snoozeIntent.setAction("SNOOZE_TASK");
        snoozeIntent.putExtra("task", task);
        snoozeIntent.putExtra("time", time);
        snoozeIntent.putExtra("recurrence_id", recurrenceId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context, 
            recurrenceId + 2, 
            snoozeIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_check, "Complete", completePendingIntent)
               .addAction(R.drawable.ic_delete, "Delete", deletePendingIntent)
               .addAction(R.drawable.ic_snooze, "Snooze 5 min", snoozePendingIntent);

        // Show the notification
        notificationManager.notify(
            isPreNotification ? PRE_NOTIFICATION_ID : NOTIFICATION_ID, 
            builder.build()
        );

        // Handle recurring reminders
        if (!isPreNotification && recurrenceType != TaskItem.RecurrenceType.NONE) {
            scheduleNextRecurrence(context, task, time, recurrenceType, recurrenceId);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminder notifications");
            
            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleNextRecurrence(Context context, String task, String time, 
                                      TaskItem.RecurrenceType recurrenceType, int recurrenceId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Parse the time
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Get current time
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();

        // Set the reminder time
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Calculate next occurrence based on recurrence type
        switch (recurrenceType) {
            case DAILY:
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case WEEKLY:
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                calendar.add(Calendar.MONTH, 1);
                break;
            default:
                return;
        }

        // If the next occurrence is in the past, keep adding intervals until we find a future date
        while (calendar.before(now)) {
            switch (recurrenceType) {
                case DAILY:
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    break;
                case WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case MONTHLY:
                    calendar.add(Calendar.MONTH, 1);
                    break;
            }
        }

        // Create intent for the next reminder
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("task", task);
        intent.putExtra("time", time);
        intent.putExtra("is_pre_notification", false);
        intent.putExtra("recurrence_type", recurrenceType.name());
        intent.putExtra("recurrence_id", recurrenceId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            recurrenceId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the next reminder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }

        // Schedule pre-notification (1 minute before)
        Calendar preCalendar = (Calendar) calendar.clone();
        preCalendar.add(Calendar.MINUTE, -1);

        Intent preIntent = new Intent(context, ReminderReceiver.class);
        preIntent.putExtra("task", task);
        preIntent.putExtra("time", time);
        preIntent.putExtra("is_pre_notification", true);
        preIntent.putExtra("recurrence_type", recurrenceType.name());
        preIntent.putExtra("recurrence_id", recurrenceId);

        PendingIntent prePendingIntent = PendingIntent.getBroadcast(
            context,
            recurrenceId + 1,
            preIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                preCalendar.getTimeInMillis(),
                prePendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                preCalendar.getTimeInMillis(),
                prePendingIntent
            );
        }
    }

    private void handleTaskCompletion(Context context, String task, String time, int recurrenceId) {
        // Cancel the current notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        // Show completion notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("Task Completed")
            .setContentText(task + " has been marked as completed")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        notificationManager.notify(recurrenceId + 2, builder.build());

        // Cancel any pending alarms for this task
        cancelTaskAlarms(context, recurrenceId);
    }

    private void handleTaskDeletion(Context context, String task, String time, int recurrenceId) {
        // Cancel the current notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        // Show deletion notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_delete)
            .setContentTitle("Task Deleted")
            .setContentText(task + " has been deleted")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        notificationManager.notify(recurrenceId + 3, builder.build());

        // Cancel any pending alarms for this task
        cancelTaskAlarms(context, recurrenceId);
    }

    private void cancelTaskAlarms(Context context, int recurrenceId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Cancel main reminder
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            recurrenceId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);

        // Cancel pre-notification
        Intent preIntent = new Intent(context, ReminderReceiver.class);
        preIntent.putExtra("is_pre_notification", true);
        PendingIntent prePendingIntent = PendingIntent.getBroadcast(
            context,
            recurrenceId + 1,
            preIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(prePendingIntent);
    }

    private void handleTaskSnooze(Context context, String task, String time, int recurrenceId) {
        // Cancel the current notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        // Schedule new reminder for 5 minutes later
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("task", task);
        intent.putExtra("time", time);
        intent.putExtra("is_pre_notification", false);
        intent.putExtra("recurrence_id", recurrenceId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            recurrenceId + 3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }

        // Show snooze notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_snooze)
            .setContentTitle("Task Snoozed")
            .setContentText(task + " has been snoozed for 5 minutes")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        notificationManager.notify(recurrenceId + 4, builder.build());
    }
} 