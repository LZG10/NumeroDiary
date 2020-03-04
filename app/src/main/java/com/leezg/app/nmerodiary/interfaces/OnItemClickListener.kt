package com.leezg.app.nmerodiary.interfaces

import android.view.View

interface OnItemClickListener {

    fun onItemClick(position: Int, view: View, model: Any)
    fun onItemClick(
        position: Int,
        view: View,
        itemID: Int,
        model: Any
    )
}
