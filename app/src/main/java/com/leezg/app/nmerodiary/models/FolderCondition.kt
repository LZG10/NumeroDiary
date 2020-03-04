package com.leezg.app.nmerodiary.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.leezg.app.nmerodiary.others.GenericTypeConverter


@Entity
data class FolderCondition(
    @PrimaryKey(autoGenerate = true) var folderConditionID: Int = 0
) {

    var folderID: Int = 0
    var identifier: String = ""
    var columnName: String = ""
    var conditionOperator: Int = 0
    var displayColumnName: String = ""

    @TypeConverters(GenericTypeConverter::class)
    var values: ArrayList<String> = arrayListOf()
}