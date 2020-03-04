package com.leezg.app.nmerodiary.interfaces

import com.afollestad.materialdialogs.MaterialDialog

abstract class DialogButtonsCallback {

    abstract fun onPositiveBtnClick(dialog: MaterialDialog, input: String)
    abstract fun onNegativeBtnClick(dialog: MaterialDialog)
    abstract fun onItemSelected(dialog: MaterialDialog, index: Int, text: CharSequence)
    abstract fun onMultiItemSelected(
        dialog: MaterialDialog,
        index: IntArray,
        text: List<CharSequence>
    )
}