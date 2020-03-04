package com.leezg.app.nmerodiary.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.leezg.app.nmerodiary.R
import com.leezg.app.nmerodiary.interfaces.OnItemClickListener
import com.leezg.app.nmerodiary.models.Change
import com.leezg.app.nmerodiary.models.Folder
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.models.createCombinedPayload
import com.leezg.app.nmerodiary.others.Constant
import kotlinx.android.synthetic.main.generic_list_row.view.*


class GenericRecyclerAdapter(
    list: MutableList<Any>,
    private val view_type: Int,
    private val extraParm: Any,
    private val _mActivity: Context
) : RecyclerView.Adapter<GenericRecyclerAdapter.GenericViewHolder>() {

    private var mListener: OnItemClickListener? = null
    private var mList: MutableList<Any> = mutableListOf()

    init {
        this.mList.addAll(list)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GenericViewHolder {
        // Inflate the row layout
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.generic_list_row, parent, false)

        return GenericViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mList.count()
    }

    // Set updated item list and refresh the Adapter
    fun setItems(data: Any) {
        val newList = data as MutableList<Any>
        val result =
            DiffUtil.calculateDiff(
                AdapterListDiffUtilCallback(this.mList, newList, view_type),
                true
            )
        result.dispatchUpdatesTo(this)
        this.mList.clear()
        this.mList.addAll(newList)
    }

    override fun onBindViewHolder(
        holder: GenericViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        // If there is nothing to update, then proceed
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            // Else proceed to display updated data on the View components
        } else {
            if (view_type == 0) {
                val combinedChange =
                    createCombinedPayload(payloads as List<Change<Record>>)
                val oldData = combinedChange.oldData
                val newData = combinedChange.newData

                holder.itemView.apply {
                    //*** Display record data on the View components (Start)
                    RecordTitle_TxtView.text = newData.recordTitle
                    Placeholder1_TxtView.text = newData.recordRemarks
                    Placeholder1_TxtView.visibility =
                        if (newData.recordRemarks.isBlank()) View.GONE else View.VISIBLE
                    Placeholder2_TxtView.text =
                        Constant.dateTimeFormat.format(newData.timestampModified)
                    Pin_ImgView.setImageResource(if (newData.isPinned) R.drawable.pinned else R.drawable.unpinned)
                    //*** (End)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        if (view_type in listOf(0, 2)) {
            // Get record on specified position
            val record = mList[position] as Record

            holder.itemView.apply {
                //*** Display record data on the View components (Start)
                RecordTitle_TxtView.text = record.recordTitle
                Placeholder1_TxtView.visibility =
                    if (record.recordRemarks.isBlank()) View.GONE else View.VISIBLE
                Placeholder1_TxtView.text = record.recordRemarks
                Placeholder2_TxtView.text = Constant.dateTimeFormat.format(record.timestampModified)
                //*** (End)

                //Glide.with(this).load(R.drawable.unpinned).into(Pin_ImgView)

                if (view_type == 2)
                    Pin_ImgView.visibility = View.GONE
                else {
                    // Set image on the ImageView based on pin status
                    Pin_ImgView.setImageResource(if (record.isPinned) R.drawable.pinned else R.drawable.unpinned)

                    // Set OnClickListener on the ImageView
                    Pin_ImgView.setOnClickListener {
                        mListener!!.onItemClick(
                            holder.adapterPosition,
                            it,
                            record
                        )
                    }
                }

                // Set OnClickListener on the row
                setOnClickListener {
                    mListener!!.onItemClick(
                        holder.adapterPosition,
                        it,
                        record
                    )
                }
            }
        } else if (view_type == 1) {
            // Get record on specified position
            val folder = mList[position] as Folder

            holder.itemView.apply {
                //*** Display record data on the View components (Start)
                RecordTitle_TxtView.text = folder.folderName
                Placeholder1_TxtView.visibility =
                    if (folder.recordIDList.size == 0) View.GONE else View.VISIBLE
                Placeholder1_TxtView.text =
                    _mActivity.getString(R.string.item_count, folder.recordIDList.size.toString())
                Placeholder2_TxtView.text = Constant.dateTimeFormat.format(folder.timestampModified)
                //*** (End)

                // Set image on the ImageView based on pin status
                Pin_ImgView.setImageResource(if (folder.isPinned) R.drawable.pinned else R.drawable.unpinned)

                // Set OnClickListener on the ImageView
                Pin_ImgView.setOnClickListener {
                    mListener!!.onItemClick(
                        holder.adapterPosition,
                        it,
                        folder
                    )
                }

                // Set OnClickListener on the row
                setOnClickListener {
                    mListener!!.onItemClick(
                        holder.adapterPosition,
                        it,
                        folder
                    )
                }
            }
        }
    }

    // Setter for OnItemClickListener
    fun setOnItemClickListener(itemClickListener: OnItemClickListener) {
        mListener = itemClickListener
    }

    inner class GenericViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class AdapterListDiffUtilCallback(
        private var oldItems: MutableList<Any>,
        private var newItems: MutableList<Any>,
        private var view_type: Int
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            if (view_type in listOf(0, 2)) (oldItems[oldItemPosition] as Record).recordID == (newItems[newItemPosition] as Record).recordID
            else
                (oldItems[oldItemPosition] as Folder).folderID == (newItems[newItemPosition] as Folder).folderID

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItems[oldItemPosition] === newItems[newItemPosition]

        @Nullable
        override fun getChangePayload(oldPosition: Int, newPosition: Int): Any? {
            val oldItem = oldItems[oldPosition]
            val newItem = newItems[newPosition]

            return Change(oldItem, newItem)
        }
    }
}