package com.leezg.app.nmerodiary.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.models.Record
import com.woxthebox.draglistview.DragItemAdapter

class RecordItemAdapter(
    private val list: ArrayList<Pair<Long, Record>>,
    private val grabHandleID: Int,
    private val dragOnLongPress: Boolean
) :
    DragItemAdapter<Pair<Long, Record>, RecordItemAdapter.ViewHolder>() {

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.record_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val record: Record = list[position].second
        holder.itemView.tag = list[position]
    }

    override fun getUniqueItemId(position: Int): Long {
        return list[position].first
    }

    inner class ViewHolder(itemView: View) :
        DragItemAdapter.ViewHolder(itemView, grabHandleID, dragOnLongPress) {

        override fun onItemClicked(view: View) {
            Toast.makeText(view.context, "Item clicked", Toast.LENGTH_SHORT).show()
        }

        override fun onItemLongClicked(view: View): Boolean {
            Toast.makeText(view.context, "Item long clicked", Toast.LENGTH_SHORT).show()
            return true
        }
    }
}