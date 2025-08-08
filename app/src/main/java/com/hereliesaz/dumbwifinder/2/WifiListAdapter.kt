package com.hereliesaz.dumbwifinder.`2`

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.dumbwifinder.data.WifiNetworkInfo

class WifiListAdapter(private var dataSet: List<WifiNetworkInfo>) :
    RecyclerView.Adapter<WifiListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ssidText: TextView = view.findViewById(R.id.ssid_text)
        val signalStrengthText: TextView = view.findViewById(R.id.signal_strength_text)
        val statusText: TextView = view.findViewById(R.id.status_text)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_wifi, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val wifiInfo = dataSet[position]
        viewHolder.ssidText.text = wifiInfo.ssid
        viewHolder.signalStrengthText.text = "${wifiInfo.signalStrength} dBm"
        viewHolder.statusText.text = "Status: ${wifiInfo.status}"
    }

    override fun getItemCount() = dataSet.size

    fun updateData(newDataSet: List<WifiNetworkInfo>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}
