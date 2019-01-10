package com.onthewifi.riley.todo

import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import java.io.ObjectInputStream
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    private var tasksFilename = "tasks.file"
    private var CHANNEL_ID = "todo"
    private val NOTIFICATION_ID = 7
    private var MORNING_REQUEST_CODE = 12
    private var NIGHT_REQUEST_CODE = 13
    override fun onReceive(context: Context, intent: Intent) {

        // Handles device restart
        if (intent.action != null)
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                MainActivity().notifyInit()
                return
            }

        var title = "Empty"
        var message = "Empty"

        if (intent.extras!!["reminderCode"] == MORNING_REQUEST_CODE) {
            title = "Good Morning!"
            message = "It's time to make your daily TODO"
        } else if (intent.extras!!["reminderCode"] == NIGHT_REQUEST_CODE) {
            val objectInputStream = ObjectInputStream(context.applicationContext.openFileInput(tasksFilename))
            val tasks = objectInputStream.readObject() as ArrayList<*>
            objectInputStream.close()
            val remaining = tasks.size
            if (tasks.isEmpty()) return
            title = "You have $remaining tasks remaining!"
            message = "Don't forget to finish them up"
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.filled_checkdot)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
        val notifyIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 2, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}