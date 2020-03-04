package com.leezg.app.nmerodiary.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.leezg.app.nmerodiary.others.FolderTypeConverter
import com.leezg.app.nmerodiary.others.GenericTypeConverter


@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true) var folderID: Int = 0, var folderName: String = "",
    var timestampModified: Long = 0
) {

    var timestampCreated: Long = 0
    var userID: String = ""
    var isPinned: Boolean = false
    var identifier: String = ""
    var folderType: Int = 0

    @TypeConverters(GenericTypeConverter::class)
    var recordIDList: ArrayList<String> = arrayListOf()
}