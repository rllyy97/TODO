package com.onthewifi.riley.todo

import android.animation.AnimatorInflater
import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.LinearLayout
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var rootView: NestedScrollView
    private lateinit var logo: ImageView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var counterText: TextView
    private lateinit var newTaskButton: Button

    var tasks: ArrayList<Task> = arrayListOf()
    private var tasksFilename = "tasks.file"

    private var CHANNEL_ID = "todo"
    private var MORNING_REQUEST_CODE = 12
    private var NIGHT_REQUEST_CODE = 13

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check file is initialized
        if (!checkPref("initialized")) {
            // Initialize file
            writeTasks()
            // Initialize alarm prefs
            writePrefInt(MORNING_REQUEST_CODE.toString()+"hour", 8)
            writePrefInt(MORNING_REQUEST_CODE.toString()+"min", 0)
            writePrefInt(NIGHT_REQUEST_CODE.toString()+"hour", 22)
            writePrefInt(NIGHT_REQUEST_CODE.toString()+"min", 0)
            writePref("initialized", true)
            writePref(MORNING_REQUEST_CODE.toString(), false)
            writePref(NIGHT_REQUEST_CODE.toString(), false)
        }

        // Load tasks from File
        readTasks()

        // Init Root
        rootView = findViewById(R.id.root)
        rootView.setOnClickListener { it.requestFocus() }
        logo = findViewById(R.id.logo)
        logo.setOnClickListener { settingsPopup() }

        // Set up recyclerView
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this as Context)
        tasksRecyclerView.adapter = RecyclerAdapter(this as Context, tasks)

        // Enable counter
        counterText = findViewById(R.id.counterText)
        counterText.text = tasks.count().toString()

        // Init new task button
        newTaskButton = findViewById(R.id.newTaskButton)
        newTaskButton.setOnClickListener {
            it.alpha = 0.0F
            val animator = AnimatorInflater.loadAnimator(applicationContext, R.animator.fade_in)
            animator.setTarget(it)
            animator.duration = 800
            animator.start()
            rootView.requestFocus()
            tasks.add(Task(false,""))
            updateView()
            tasksRecyclerView.post {
                val view = (tasksRecyclerView.layoutManager as LinearLayoutManager).findViewByPosition(tasksRecyclerView.adapter!!.itemCount-1)
                val newTaskText = view!!.findViewById<EditText>(R.id.bodyText)
                newTaskText!!.requestFocus()
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(newTaskText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        // Clear functionality
        counterText.setOnClickListener {
            tasks.clear()
            hideKeyboard()
            updateView()
        }

        createNotificationChannel()
        notifyInit()

    }

    // HANDLE DATA WRITING AND READING

    fun readTasks() {
        val objectInputStream = ObjectInputStream(applicationContext.openFileInput(tasksFilename))
        tasks = objectInputStream.readObject() as ArrayList<Task>
        objectInputStream.close()
    }

    fun writeTasks() {
        val objectOutputStream = ObjectOutputStream(applicationContext.openFileOutput(tasksFilename, Context.MODE_PRIVATE))
        objectOutputStream.writeObject(tasks)
        objectOutputStream.close()
    }

    // UPDATE VIEWS AND HANDLE VIEW EVENTS

    fun updateView() {
        writeTasks()
        runOnUiThread { (tasksRecyclerView.adapter as RecyclerAdapter).notifyDataSetChanged() }
        counterText.text = tasks.count().toString()
        if (tasks.count() == 0) thumbPopup()
        hideKeyboard()
    }

    fun thumbPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.all_done_popup, null)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.animationStyle = R.style.popup_window_animation_fade
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
        Handler().postDelayed({ popupWindow.dismiss() }, 1000)

    }

    private fun settingsPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.settings_popup, null)

        val width = LinearLayout.LayoutParams.MATCH_PARENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.animationStyle = R.style.popup_window_animation

        // Initialize time picker button with current time set
        val morningPicker = popupView.findViewById<Button>(R.id.morningPicker)
        val mHour = getPrefInt(MORNING_REQUEST_CODE.toString()+"hour")
        val mMin = getPrefInt(MORNING_REQUEST_CODE.toString()+"min")
        morningPicker.text = formatTime(mHour, mMin)
        morningPicker.setOnClickListener { showTimePickerDialog(MORNING_REQUEST_CODE); popupWindow.dismiss()  }

        // Initialize switch with current active state
        val morningSwitch = popupView.findViewById<Switch>(R.id.morningSwitch)
        morningSwitch.isChecked = checkPref(MORNING_REQUEST_CODE.toString())
        morningSwitch.setOnClickListener { reminderSwitch(MORNING_REQUEST_CODE)}

        // Initialize time picker button with current time set
        val nightPicker = popupView.findViewById<Button>(R.id.nightPicker)
        val nHour = getPrefInt(NIGHT_REQUEST_CODE.toString()+"hour")
        val nMin = getPrefInt(NIGHT_REQUEST_CODE.toString()+"min")
        nightPicker.text = formatTime(nHour, nMin)
        nightPicker.setOnClickListener { showTimePickerDialog(NIGHT_REQUEST_CODE); popupWindow.dismiss() }

        // Initialize switch with current active state
        val nightSwitch = popupView.findViewById<Switch>(R.id.nightSwitch)
        nightSwitch.isChecked = checkPref(NIGHT_REQUEST_CODE.toString())
        nightSwitch.setOnClickListener { reminderSwitch(NIGHT_REQUEST_CODE) }

        popupWindow.showAtLocation(rootView, Gravity.BOTTOM, 0, 0)
    }

    // NOTIFICATION

    fun notifyInit() {
        createNotificationChannel()
        // Morning Notification
        if(checkPref(MORNING_REQUEST_CODE.toString()))
            setReminder(MORNING_REQUEST_CODE, getPrefInt(MORNING_REQUEST_CODE.toString()+"hour"), getPrefInt(MORNING_REQUEST_CODE.toString()+"min"))
        // Night Notification
        if(checkPref(NIGHT_REQUEST_CODE.toString()))
            setReminder(NIGHT_REQUEST_CODE, getPrefInt(NIGHT_REQUEST_CODE.toString()+"hour"), getPrefInt(NIGHT_REQUEST_CODE.toString()+"min"))
    }

    fun createNotification(title: String, text: String): Int {
        val notificationId: Int = Random.nextInt()
        val pendingIntent= PendingIntent.getActivity(this, 0, intent, 0)
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.filled_checkdot)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, mBuilder.build())
        }
        return notificationId
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "default"
            val descriptionText = "default"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setReminder(code: Int, hour: Int, min: Int) {
        val cal = Calendar.getInstance()
        val setCal = Calendar.getInstance()
        setCal.set(Calendar.HOUR_OF_DAY, hour)
        setCal.set(Calendar.MINUTE, min)
        setCal.set(Calendar.SECOND, 0)

        if (setCal.before(cal)) setCal.add(Calendar.DATE,1)

        // Enable a receiver
        val receiver = ComponentName(this, AlarmReceiver::class.java)
        val pm = packageManager
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        val intent1 = Intent(this, AlarmReceiver::class.java)
        intent1.putExtra("reminderCode", code)
        val pendingIntent = PendingIntent.getBroadcast(this, code, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, setCal.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

    }

    private fun cancelReminder(code: Int) {
        // Disable receiver
        val receiver = ComponentName(this, AlarmReceiver::class.java)
        val pm = packageManager
        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)

        val intent1 = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, code, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderSwitch(code: Int) {
        if (checkPref(code.toString())) {
            cancelReminder(code)
            writePref(code.toString(), false)
        } else {
            setReminder(code, getPrefInt(code.toString()+"hour"), getPrefInt(code.toString()+"min"))
            writePref(code.toString(), true)
        }
    }

    private fun showTimePickerDialog(code: Int) {
        val hour = getPrefInt(code.toString()+"hour")
        val min = getPrefInt(code.toString()+"min")
        val timePickerDialog = TimePickerDialog(this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    writePrefInt(code.toString()+"hour", hourOfDay)
                    writePrefInt(code.toString()+"min", minute)
                    setReminder(code, hourOfDay, minute)
                }, hour, min, false)
        timePickerDialog.setTitle("Select Time")
        timePickerDialog.setOnDismissListener { settingsPopup() }
        timePickerDialog.show()
    }

    private fun formatTime(hour: Int, min: Int): String {
        val inputString = "$hour:$min"
        val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return outputFormat.format(inputFormat.parse(inputString))
    }

    // UTILITY

    fun toast(string: String) = Toast.makeText(this, string, Toast.LENGTH_LONG).show()
    fun log(string: String) = Log.e("ERROR: ", string)
    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (currentFocus != null) inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    override fun onBackPressed() { super.onBackPressed(); rootView.requestFocus() }

    // PREFERENCES

    private fun checkPref(label: String): Boolean {
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean(label, false)
    }

    private fun writePref(label: String, bool: Boolean) {
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean(label, bool)
        editor.apply()
    }

    private fun getPrefInt(label: String): Int {
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        return sharedPref.getInt(label, -1)
    }

    private fun writePrefInt(label: String, int: Int) {
        val sharedPref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putInt(label, int)
        editor.apply()
    }


}
