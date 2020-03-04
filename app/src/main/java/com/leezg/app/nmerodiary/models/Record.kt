package com.leezg.app.nmerodiary.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.leezg.app.nmerodiary.others.Constant
import com.leezg.app.nmerodiary.others.GenericTypeConverter
import com.leezg.app.nmerodiary.others.RecordTypeConverter


@Entity
data class Record(
    @PrimaryKey(autoGenerate = true) var recordID: Int = 0, var recordTitle: String = "",
    var timestampModified: Long = 0
) {

    var timestampCreated: Long = 0
    var userID: String = ""
    var isTemplate: Boolean = false
    var templateName: String = ""
    var templateID: String = ""
    var recordRemarks: String = ""
    var isPinned: Boolean = false
    var identifier: String = ""
    var searchString: String = ""

    @TypeConverters(GenericTypeConverter::class)
    var docIDList: ArrayList<String> = arrayListOf()

    @TypeConverters(GenericTypeConverter::class)
    var recordImgList: ArrayList<String> = arrayListOf()

    @TypeConverters(RecordTypeConverter::class)
    var fieldList: ArrayList<Field> = arrayListOf()

    companion object {
        fun returnColumnName(fieldIndex: Int): String {
            return when (fieldIndex) {
                0 -> Constant.recordTitle
                1 -> Constant.recordRemarks
                2 -> Constant.timestampModified
                3 -> Constant.timestampCreated
                4 -> Constant.isPinned
                5 -> Constant.fieldName
                6 -> Constant.fieldUnit
                else -> ""
            }
        }

        fun returnColumnIndex(fieldName: String): Int {
            return when (fieldName) {
                Constant.recordTitle -> 0
                Constant.recordRemarks -> 1
                Constant.timestampModified -> 2
                Constant.timestampCreated -> 3
                Constant.isPinned -> 4
                Constant.fieldName -> 5
                Constant.fieldUnit -> 6
                else -> 7
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val record: Record = other as Record

        if (recordTitle == record.recordTitle
            && templateName == record.templateName
            && recordRemarks == record.recordRemarks
            && fieldList == record.fieldList
        ) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recordTitle.hashCode()
        result = 31 * result + templateName.hashCode()
        result = 31 * result + recordRemarks.hashCode()
        result = 31 * result + fieldList.hashCode()
        return result
    }
}