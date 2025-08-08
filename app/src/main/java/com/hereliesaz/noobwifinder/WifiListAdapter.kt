package com.hereliesaz.noobwifinder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hereliesaz.noobwifinder.data.WifiNetworkInfo

class WifiListAdapter(private var dataSet: List<WifiNetworkInfo>) :
    RecyclerView.Adapter<WifiListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ssidText: TextView = view.findViewById(R.id.ssid_text)
        val bssidText: TextView = view.findViewById(R.id.bssid_text)
        val signalStrengthText: TextView = view.findViewById(R.id.signal_strength_text)
        val securityText: TextView = view.findViewById(R.id.security_text)
        val statusText: TextView = view.findViewById(R.id.status_text)
        val triedPasswordText: TextView = view.findViewById(R.id.tried_password)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item_wifi, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val wifiInfo = dataSet[position]
        viewHolder.ssidText.text = "SSID: ${wifiInfo.ssid}"
        viewHolder.bssidText.text = "BSSID: ${wifiInfo.bssid}"
        viewHolder.signalStrengthText.text = "Signal: ${wifiInfo.signalStrength} dBm"
        viewHolder.securityText.text = "Security: ${wifiInfo.securityType}"
        viewHolder.statusText.text = "Status: ${wifiInfo.status}"
        viewHolder.triedPasswordText.text = "Trying: ${wifiInfo.password ?: ""}"
    }

    override fun getItemCount() = dataSet.size

    fun updateData(newDataSet: List<WifiNetworkInfo>) {
        dataSet = newDataSet
        notifyDataSetChanged()
    }
}
