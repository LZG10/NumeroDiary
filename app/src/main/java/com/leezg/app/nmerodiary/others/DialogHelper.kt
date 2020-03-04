package com.leezg.app.nmerodiary.others

import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.leezg.app.nmerodiary.interfaces.DialogButtonsCallback

class DialogHelper {

    companion object {

        fun showConfirmationDialog(
            _mActivity: FragmentActivity,
            dialogTitle: Int,
            dialogMessage: Int,
            positiveBtnText: Int,
            negativeBtnText: Int,
            callback: DialogButtonsCallback
        ) {

        }

        fun showInputDialog(
            _mActivity: FragmentActivity,
            dialogTitle: Int,
            prefill: String,
            positiveBtnText: Int,
            negativeBtnText: Int,
            callback: DialogButtonsCallback
        ) {
            MaterialDialog(_mActivity).show {
                input(
                    maxLength = Constant.MAX_INPUT_LENGTH,
                    prefill = prefill
                ) { dialog, input ->
                    callback.onPositiveBtnClick(dialog, input.toString().trim())
                }
                if (positiveBtnText > 0)
                    positiveButton(positiveBtnText)
                if (negativeBtnText > 0)
                    negativeButton(negativeBtnText) {
                        callback.onNegativeBtnClick(it)
                    }
                title(dialogTitle)
            }
        }

        fun showListDialog(
            _mActivity: FragmentActivity,
            dialogTitle: Int,
            itemList: MutableList<String>,
            positiveBtnText: Int,
            negativeBtnText: Int,
            waitForPositiveBtn: Boolean,
            callback: DialogButtonsCallback
        ) {
            MaterialDialog(_mActivity).show {
                title(dialogTitle)
                listItems(
                    items = itemList,
                    waitForPositiveButton = waitForPositiveBtn
                ) { dialog, index, text ->
                    callback.onItemSelected(dialog, index, text)
                }
                positiveButton(positiveBtnText) {
                    callback.onPositiveBtnClick(it, "")
                }
                negativeButton(negativeBtnText) {
                    callback.onNegativeBtnClick(it)
                }
            }
        }

        fun showMultiChoiceListDialog(
            _mActivity: FragmentActivity,
            dialogTitle: Int,
            itemList: MutableList<String>,
            positiveBtnText: Int,
            negativeBtnText: Int,
            waitForPositiveBtn: Boolean,
            callback: DialogButtonsCallback
        ) {
            MaterialDialog(_mActivity).show {
                title(dialogTitle)
                positiveButton(positiveBtnText)
                listItemsMultiChoice(
                    waitForPositiveButton = waitForPositiveBtn,
                    items = itemList
                ) { dialog, index, text ->
                    callback.onMultiItemSelected(dialog, index, text)
                }
                negativeButton(negativeBtnText) {
                    callback.onNegativeBtnClick(it)
                }
            }
        }
    }
}