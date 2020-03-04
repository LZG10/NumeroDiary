package com.leezg.app.nmerodiary.others

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leezg.app.nmerodiary.models.FolderCondition


class FolderTypeConverter {

    private val gson = Gson()

    @TypeConverter
    fun stringToList(data: String?): ArrayList<FolderCondition> {
        if (data == null)
            return ArrayList()

        val listType = object : TypeToken<ArrayList<FolderCondition>>() {}.type
        return gson.fromJson<ArrayList<FolderCondition>>(data, listType)
    }

    @TypeConverter
    fun listToString(fieldList: ArrayList<FolderCondition>): String {
        return gson.toJson(fieldList)
    }
}