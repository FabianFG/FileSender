package me.fungames.filesender.utils

import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListView


fun ListView.setDynamicHeight() {
    val mListAdapter: ListAdapter = adapter
        ?: // when adapter is null
        return
    var height = 0
    val desiredWidth =
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED)
    for (i in 0 until mListAdapter.count) {
        val listItem: View = mListAdapter.getView(i, null, this)
        listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED)
        height += listItem.measuredHeight
    }
    val params: ViewGroup.LayoutParams = layoutParams
    params.height = height + dividerHeight * (mListAdapter.count - 1)
    layoutParams = params
    requestLayout()
}