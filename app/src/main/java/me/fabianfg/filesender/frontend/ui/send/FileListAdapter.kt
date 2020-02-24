package me.fabianfg.filesender.frontend.ui.send

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import me.fabianfg.filesender.R

class FileListAdapter(private val sendActivity: SendActivity, private val resource: Int, objects: List<FileInfoContainer>) :
    ArrayAdapter<FileInfoContainer>(sendActivity, resource, objects) {

    class ViewHolder(val fileName : TextView, val removeFile : Button)

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = getItem(position)!!

        val result : View
        val holder : ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val clientAdapter = inflater.inflate(resource, parent, false)
            holder = ViewHolder(clientAdapter.findViewById(R.id.fileName), clientAdapter.findViewById(R.id.removeFile))

            clientAdapter.tag = holder
            result = clientAdapter
        } else {
            holder = convertView.tag as ViewHolder
            result = convertView
        }
        holder.fileName.text = "${file.descriptor.fileName} (${Formatter.formatFileSize(context, file.descriptor.fileSize)})"
        holder.fileName.setOnClickListener { sendActivity.sendFile(file) }
        holder.removeFile.setOnClickListener { sendActivity.removeFile(file) }
        return result
    }
}