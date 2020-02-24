package me.fungames.filesender.frontend.ui.help

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import me.fungames.filesender.R

class HelpListAdapter(val context : Context) : BaseExpandableListAdapter() {

    class GroupViewHolder(val label : TextView)

    private val groups = listOf(context.getString(R.string.send), context.getString(R.string.receive))

    override fun getGroup(groupPosition: Int): Any {
        return groups[groupPosition]
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        var newConvertView = convertView

        val holder : GroupViewHolder
        if (newConvertView == null) {
            val inflater = LayoutInflater.from(context)
            val group = inflater.inflate(R.layout.help_list_group, parent, false)
            holder = GroupViewHolder(group.findViewById(R.id.lblListHeader))
            group.tag = holder
            newConvertView = group!!
        } else {
            holder = newConvertView.tag as GroupViewHolder
        }

        holder.label.text = getGroup(groupPosition) as String

        return newConvertView
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return 1
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        require(childPosition == 0)
        return when(groupPosition) {
            /*Send*/0 -> {
                context.getString(R.string.send)
            }
            /*Receive*/else -> {
                context.getString(R.string.receive)
            }
        }
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val inflater = LayoutInflater.from(context)
        return when(groupPosition) {
            0 -> inflater.inflate(R.layout.help_send, parent, false)
            else -> inflater.inflate(R.layout.help_receive, parent, false)
        }
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return groups.size
    }

}