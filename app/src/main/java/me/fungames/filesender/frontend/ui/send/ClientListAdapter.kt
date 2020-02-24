package me.fungames.filesender.frontend.ui.send

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import me.fungames.filesender.R
import me.fungames.filesender.server.ClientInfo

class ClientListAdapter(private val sendActivity: SendActivity, private val resource: Int, objects: List<ClientInfo>) :
    ArrayAdapter<ClientInfo>(sendActivity, resource, objects) {

    class ViewHolder(val clientName : TextView, val kickClient : Button)


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val client = getItem(position)!!

        val result : View
        val holder : ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val clientAdapter = inflater.inflate(resource, parent, false)
            holder = ViewHolder(clientAdapter.findViewById(R.id.clientName), clientAdapter.findViewById(R.id.kickClient))

            clientAdapter.tag = holder
            result = clientAdapter
        } else {
            holder = convertView.tag as ViewHolder
            result = convertView
        }
        holder.clientName.text = client.clientName
        holder.kickClient.setOnClickListener { sendActivity.fileServer.kickClient(client.clientId) }
        return result
    }
}