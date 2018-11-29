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
import android.app.Activity
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.view.Gravity
import android.widget.PopupWindow
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.task_view.*


class MainActivity : AppCompatActivity() {

    private lateinit var rootView: NestedScrollView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var counterText: TextView
    private lateinit var newTaskButton: Button
    private lateinit var clearButton: ImageButton

    private var tasks: ArrayList<Task> = arrayListOf()
    private var tasksFilename = "tasks.file"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load tasks from File
        readTasks()

        // Init Root
        rootView = findViewById(R.id.root)
        rootView.setOnClickListener { it.requestFocus() }

        // Set up recyclerView
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this as Context)
        tasksRecyclerView.adapter = RecyclerAdapter(this as Context, tasks)

        // Swipe to delete
        val swipeHandler = object : SwipeToDeleteCallback(applicationContext) {
            override fun onSwiped(view: RecyclerView.ViewHolder, pos: Int) {
                (tasksRecyclerView.adapter as RecyclerAdapter).removeAt(view.adapterPosition)
                updateView()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView)

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
        clearButton = findViewById(R.id.clearButton)
        clearButton.setOnClickListener {
            tasks.clear()
            hideKeyboard()
            updateView()
        }

    }

    fun updateView() {
        writeTasks()
        runOnUiThread { (tasksRecyclerView.adapter as RecyclerAdapter).notifyDataSetChanged() }
        counterText.text = tasks.count().toString()
        if (tasks.count() == 0) thumbPopup()
    }

    fun writeTasks() {
        val objectOutputStream = ObjectOutputStream(applicationContext.openFileOutput(tasksFilename, Context.MODE_PRIVATE))
        objectOutputStream.writeObject(tasks)
        objectOutputStream.close()
    }

    fun readTasks() {
        val objectInputStream = ObjectInputStream(applicationContext.openFileInput(tasksFilename))
        tasks = objectInputStream.readObject() as ArrayList<Task>
        objectInputStream.close()
    }

    fun thumbPopup() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.all_done_popup, null)

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = true // lets taps outside the popup also dismiss it
        val popupWindow = PopupWindow(popupView, width, height, focusable)
        popupWindow.animationStyle = R.style.popup_window_animation
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
        Handler().postDelayed({ popupWindow.dismiss() }, 1000)

    }

    fun toast(string: String) = Toast.makeText(this, string, Toast.LENGTH_LONG).show()
    fun hideKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        if (currentFocus != null) inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    override fun onBackPressed() { super.onBackPressed(); rootView.requestFocus() }

}
