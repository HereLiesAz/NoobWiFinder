package com.hereliesaz.dumbwifinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.dumbwifinder.data.WifiNetworkInfo
import com.hereliesaz.dumbwifinder.databinding.ListItemWifiBinding

class WifiListAdapter(private var dataSet: List<WifiNetworkInfo>) :
    RecyclerView.Adapter<WifiListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ListItemWifiBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemWifiBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val wifiInfo = dataSet[position]
        viewHolder.binding.ssidText.text = wifiInfo.ssid
        viewHolder.binding.signalStrengthText.text = "${wifiInfo.signalStrength} dBm"
        viewHolder.binding.statusText.text = "Status: ${wifiInfo.status}"
    }

    override fun getItemCount() = dataSet.size

    fun updateData(newDataSet: List<WifiNetworkInfo>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}
