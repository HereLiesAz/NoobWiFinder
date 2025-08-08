package com.hereliesaz.noobwifinder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PasswordListAdapter(private var dataSet: List<String>) :
    RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val passwordText: TextView = view.findViewById(R.id.password_text)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_password, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.passwordText.text = dataSet[position]
    }

    override fun getItemCount() = dataSet.size

    fun updateData(newDataSet: List<String>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}
