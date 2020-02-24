package me.fungames.filesender.frontend.ui.history

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import me.fungames.filesender.R
import me.fungames.filesender.utils.openFile
import java.io.File

class FileHistoryListAdapter(private val historyActivity: FileHistoryActivity, private val resource: Int, objects: List<File>) :
    ArrayAdapter<File>(historyActivity, resource, objects) {

    class ViewHolder(val fileNameView : TextView, val fileSizeView : TextView, val deleteFileButton : Button)

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val file = getItem(position)!!

        val result : View
        val holder : ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val clientAdapter = inflater.inflate(resource, parent, false)
            holder = ViewHolder(clientAdapter.findViewById(R.id.fileHistoryNameView), clientAdapter.findViewById(
                R.id.fileHistorySizeView), clientAdapter.findViewById(
                R.id.deleteFile))
            clientAdapter.tag = holder
            result = clientAdapter
        } else {
            holder = convertView.tag as ViewHolder
            result = convertView
        }


        holder.fileNameView.text = file.name
        holder.fileSizeView.text = Formatter.formatFileSize(historyActivity, file.length())
        holder.deleteFileButton.setOnClickListener {
            historyActivity.onFileDelete(file)
        }
        result.setOnClickListener {
            historyActivity.openFile(file)
        }
        return result
    }
}