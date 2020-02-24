package me.fungames.filesender.frontend.ui.send

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.fileshare_select_client.*
import me.fungames.filesender.R
import me.fungames.filesender.server.ClientInfo
import kotlin.math.roundToInt

class ClientSelectDialog(private val sendActivity: SendActivity, private val clientListContent : List<ClientInfo>, val onSelected: (ClientInfo?) -> Unit) : Dialog(sendActivity) {

    private lateinit var clientListAdapter : ArrayAdapter<ClientInfo>

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val inflater = LayoutInflater.from(sendActivity)
        val view = inflater.inflate(R.layout.fileshare_select_client, null)
        val displayMetrics = DisplayMetrics()
        sendActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        view.minimumWidth = (displayMetrics.widthPixels * 0.9).roundToInt()
        setContentView(view)
        clientListAdapter = ArrayAdapter(sendActivity, R.layout.simple_string_list_item, clientListContent)
        selectClientList.adapter = clientListAdapter
        selectClientList.emptyView = noClientYetView
        clientListAdapter.notifyDataSetChanged()
        selectClientList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val client = clientListContent[position]
                cancel()
                onSelected(client)
            }
        setOnCancelListener { onSelected(null) }
    }
}