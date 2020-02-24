package me.fabianfg.filesender.frontend.ui.appsend

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import me.fabianfg.filesender.R

class AppListAdapter(private val appSendActivity: AppSendActivity, private val resource: Int, objects: List<ApplicationInfo>) :
    ArrayAdapter<ApplicationInfo>(appSendActivity, resource, objects) {

    private val packageManager = appSendActivity.packageManager

    class ViewHolder(val appIconView : ImageView, val appNameView : TextView, val appPackageView : TextView)

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val app = getItem(position)!!

        val result : View
        val holder : ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            val clientAdapter = inflater.inflate(resource, parent, false)
            holder = ViewHolder(clientAdapter.findViewById(R.id.appIconView), clientAdapter.findViewById(
                R.id.appNameView), clientAdapter.findViewById(
                R.id.appPackageView))
            clientAdapter.tag = holder
            result = clientAdapter
        } else {
            holder = convertView.tag as ViewHolder
            result = convertView
        }
        val label = app.loadLabel(packageManager)
        holder.appIconView.setImageDrawable(app.loadIcon(packageManager))
        holder.appNameView.text = label
        holder.appPackageView.text = app.packageName
        result.setOnClickListener {
            appSendActivity.setResult(Activity.RESULT_OK, Intent().putExtra(AppSendActivity.APK_PATH, app.publicSourceDir).putExtra(AppSendActivity.APK_NAME, "$label.apk"))
            appSendActivity.finish()
        }
        return result
    }
}