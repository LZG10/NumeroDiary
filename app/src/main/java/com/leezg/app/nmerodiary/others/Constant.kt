package com.leezg.app.nmerodiary.others

import java.text.SimpleDateFormat

object Constant {

    val dateFormat = SimpleDateFormat("E, dd MMM YYYY")
    val timeFormat = SimpleDateFormat("HH:mm")
    val dateTimeFormat = SimpleDateFormat("E, dd MMM  •  HH:mm")
    val numericMonthFormat = SimpleDateFormat("MM")
    val textMonthFormat = SimpleDateFormat("MMMM")
    val dayFormat = SimpleDateFormat("EE, HH:mm")
    val weekFormat = SimpleDateFormat("EEEE")
    val fullDateFormat = SimpleDateFormat("yyyyMMdd")
    val altFullDateFormat = SimpleDateFormat("yyyy-MM-dd")

    const val LOG_TAG = "AppDebug"
    const val DATABASE_NAME = "AppDatabase"
    const val fieldName = "fieldName"
    const val fieldUnit = "fieldUnit"
    const val fieldValue = "fieldValue"
    const val KEY1 = "Key1"
    const val KEY2 = "Key2"
    const val KEY3 = "Key3"
    const val SAVED_FIELD_UNIT = "SavedFieldUnit"
    const val SAVED_FIELD_NAME = "SavedFieldName"
    const val SAVED_CONDITION = "SavedCondition"
    const val MAX_INPUT_LENGTH = 50
    const val HOME_FRAGMENT = "HomeFragment"
    const val MODIFY_RECORD_FRAGMENT = "ModifyRecordFragment"
    const val SETTINGS_FRAGMENT = "SettingsFragment"
    const val RECORD_VIEW_FRAGMENT = "RecordViewFragment"
    const val FOLDER_VIEW_FRAGMENT = "FolderViewFragment"
    const val FOLDER_CONDITION_FRAGMENT = "FolderConditionFragment"
    val excludedFragmentList = listOf(
        MODIFY_RECORD_FRAGMENT,
        SETTINGS_FRAGMENT,
        FOLDER_VIEW_FRAGMENT,
        FOLDER_CONDITION_FRAGMENT
    )
    const val ASCENDING = 0
    const val DESCENDING = 1
    const val ascText = "Ascending"
    const val descText = "Descending"
    const val recordTitle = "recordTitle"
    const val recordRemarks = "recordRemarks"
    const val timestampModified = "timestampModified"
    const val timestampCreated = "timestampCreated"
    const val isPinned = "isPinned"
    const val isTemplate = "isTemplate"
    const val searchString = "searchString"
    const val identifier = "identifier"
    const val titleText = "Title"
    const val Record = "Record"
    const val Folder = "Folder"
    const val folderName = "folderName"
    const val CONDITION = 1
    const val NORMAL = 0
    const val DELETE = "delete"
    const val INSERT = "insert"
    const val UPDATE = "update"
    val standardFields =
        listOf(recordTitle, recordRemarks, isPinned, timestampModified, timestampCreated)
    val multiValCondOperator = listOf("Are", "Between")

    const val Is = "="
    const val Are = "IN"
    const val Between = "BETWEEN"
    const val NewerThan = ">"
    const val OlderThan = "<"
    const val LargerThan = ">"
    const val SmallerThan = "<"
    const val LargerOrEqualTo = ">="
    const val SmallerOrEqualTo = "<="
    const val Contains = "LIKE"
}