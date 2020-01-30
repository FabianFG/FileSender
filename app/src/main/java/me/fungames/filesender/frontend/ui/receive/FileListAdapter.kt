package me.fungames.filesender.frontend.ui.receive

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import me.fungames.filesender.R

class FileListAdapter(val receiveActivity: ReceiveActivity, val resource: Int, objects: List<FileInfoContainer>) :
    ArrayAdapter<FileInfoContainer>(receiveActivity, resource, objects) {

    class ViewHolder(val fileName : TextView)

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = getItem(position)!!

        val result : View
        val holder : ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val clientAdapter = inflater.inflate(resource, parent, false)
            holder = ViewHolder(clientAdapter.findViewById(R.id.text1))

            clientAdapter.tag = holder
            result = clientAdapter
        } else {
            holder = convertView.tag as ViewHolder
            result = convertView
        }
        holder.fileName.text = "${file.descriptor.fileName} (${Formatter.formatFileSize(context, file.descriptor.fileSize)})"
        holder.fileName.setOnClickListener { receiveActivity.downloadFile(file) }
        return result
    }
}