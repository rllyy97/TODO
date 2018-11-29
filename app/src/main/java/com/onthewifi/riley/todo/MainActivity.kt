package com.onthewifi.riley.todo

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class MainActivity : AppCompatActivity() {

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
            tasks.add(Task(false,""))
            updateView()
            var view = (tasksRecyclerView.layoutManager as LinearLayoutManager).findViewByPosition(tasks.size-1)
//            var newTaskText = view!!.bodyText
//            newTaskText!!.requestFocus()
//            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(newTaskText, InputMethodManager.SHOW_IMPLICIT)
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
}
