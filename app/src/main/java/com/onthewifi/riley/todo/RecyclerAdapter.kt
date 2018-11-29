package com.onthewifi.riley.todo

import android.content.Context
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher



class RecyclerAdapter(private val context: Context, private val tasks: ArrayList<Task>) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private var mainActivity: MainActivity
    init {
        setHasStableIds(true)
        mainActivity = context as MainActivity
    }

    class ViewHolder(taskView: LinearLayout): RecyclerView.ViewHolder(taskView) {
        // Holds view for each task
        var markButton: CheckBox = taskView.findViewById(R.id.markButton)
        var bodyText: TextView = taskView.findViewById(R.id.bodyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.task_view, parent, false) as LinearLayout)
    }

    override fun onBindViewHolder(view: ViewHolder, pos: Int) {
        view.markButton.isChecked = tasks[pos].mark
        view.markButton.setOnClickListener { tasks[view.adapterPosition].mark = view.markButton.isChecked; mainActivity.writeTasks() }
        view.bodyText.text = tasks[pos].body
        view.bodyText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) { tasks[view.adapterPosition].body = s.toString(); mainActivity.writeTasks() }
        })
    }

    fun removeAt(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
        mainActivity.writeTasks()
    }

    override fun getItemCount() = tasks.size
    override fun getItemId(position: Int) = position.toLong()

}