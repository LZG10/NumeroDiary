package com.leezg.app.nmerodiary.models

import android.text.InputType
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.leezg.app.nmerodiary.others.GenericTypeConverter

@Entity
data class Field(
    var fieldName: String = "",
    var fieldValue: String = "",
    var fieldType: Int = 0
) {

    var isChecked: Boolean = false
    @PrimaryKey(autoGenerate = true)
    var fieldID: Int = 0
    //var recordID: Int = 0
    var fieldUnit: String = ""
    var identifier: String = ""

    @TypeConverters(GenericTypeConverter::class)
    var fieldImgList: ArrayList<String> = arrayListOf()

    companion object {

        fun getInputType(fieldType: Int): Int {
            return when (fieldType) {
                0 -> InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_MULTI_LINE
                1 -> InputType.TYPE_CLASS_NUMBER
                2 -> InputType.TYPE_NUMBER_FLAG_DECIMAL
                3 -> InputType.TYPE_DATETIME_VARIATION_TIME
                4 -> InputType.TYPE_DATETIME_VARIATION_DATE
                else -> InputType.TYPE_CLASS_DATETIME
            }
        }

        fun returnFieldType(inputType: Int): Int {
            return when (inputType) {
                InputType.TYPE_CLASS_TEXT + InputType.TYPE_TEXT_FLAG_MULTI_LINE -> 0
                InputType.TYPE_CLASS_NUMBER -> 1
                InputType.TYPE_NUMBER_FLAG_DECIMAL -> 2
                InputType.TYPE_DATETIME_VARIATION_TIME -> 3
                InputType.TYPE_DATETIME_VARIATION_DATE -> 4
                else -> 5
            }
        }
    }
}