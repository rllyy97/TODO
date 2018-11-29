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
import android.animation.AnimatorInflater






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
        clearButton = findViewById(R.id.clearButton)
        clearButton.setOnClickListener {
            tasks.clear()
            updateView()
        }

    }

    fun updateView() {
        writeTasks()
        runOnUiThread { (tasksRecyclerView.adapter as RecyclerAdapter).notifyDataSetChanged() }
        counterText.text = tasks.count().toString()
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

    fun toast(string: String) = Toast.makeText(this, string, Toast.LENGTH_LONG).show()
    override fun onBackPressed() { super.onBackPressed(); rootView.requestFocus() }

}
