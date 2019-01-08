package com.onthewifi.riley.todo

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.R.animator
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.task_view.*
import java.util.*
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var rootView: NestedScrollView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var counterText: TextView
    private lateinit var newTaskButton: Button
//    private lateinit var clearButton: ImageButton

    private var tasks: ArrayList<Task> = arrayListOf()
    private var tasksFilename = "tasks.file"

    private var CHANNEL_ID = "todo"
    private var DAILY_REMINDER_REQUEST_CODE = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check file is initialized
        if (!checkPref("initialized")) {
            writeTasks()
            writePref("initialized", true)
        }

        // Load tasks from File
        readTasks()

        // Init Root
        rootView = findViewById(R.id.root)
        rootView.setOnClickListener { it.requestFocus() }

        // Set up recyclerView
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this as Context)
        tasksRecyclerView.adapter = RecyclerAdapter(this as Context, tasks)

//        // Swipe to delete
//        val swipeHandler = object : SwipeToDeleteCallback(applicationContext) {
//            override fun onSwiped(view: RecyclerView.ViewHolder, pos: Int) {
//                (tasksRecyclerView.adapter as RecyclerAdapter).removeAt(view.layoutPosition)
//                updateView()
//            }
//        }
//        val itemTouchHelper = ItemTouchHelper(swipeHandler)
//        itemTouchHelper.attachToRecyclerView(tasksRecyclerView)

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
//        newTaskButton.alpha = 0.0F
//        tasksRecyclerView.postOnAnimation {
//            newTaskButton.alpha = 1.0F
//            newTaskButton.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.item_animation_fall_down))
//        }

        // Clear functionality
//        clearButton = findViewById(R.id.clearButton)
        counterText.setOnClickListener {
            tasks.clear()
            hideKeyboard()
            updateView()
        }

        createNotificationChannel()
        notifInit()

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
        val popupView = inflater.inflate(R.layout.all_done_popup, rootView)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.animationStyle = R.style.popup_window_animation
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
        Handler().postDelayed({ popupWindow.dismiss() }, 1000)

    }

    // NOTIFICATION

    fun notifInit() {
        createNotificationChannel()
        val cal= Calendar.getInstance()
        setReminder(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE) + 1)
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
            // notificationId is a unique int for each notification that you must define
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

    private fun setReminder(hour: Int, min: Int) {
        val cal = Calendar.getInstance()
        val setCal = Calendar.getInstance()
        setCal.set(Calendar.HOUR_OF_DAY, 8)
        setCal.set(Calendar.MINUTE, min)
        setCal.set(Calendar.SECOND, 0)
        //cancelReminder(context, this)

        if (setCal.before(cal)) setCal.add(Calendar.DATE,1)

        // Enable a receiver
        val receiver = ComponentName(this, AlarmReceiver::class.java)
        val pm = packageManager

        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        val intent1 = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, DAILY_REMINDER_REQUEST_CODE, intent1, PendingIntent.FLAG_UPDATE_CURRENT)
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, setCal.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

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


}
