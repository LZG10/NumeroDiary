package com.leezg.app.nmerodiary.others

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class GenericTypeConverter {

    private val gson = Gson()

    @TypeConverter
    fun stringToList(data: String?): ArrayList<String> {
        if (data == null)
            return ArrayList()

        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson<ArrayList<String>>(data, listType)
    }

    @TypeConverter
    fun listToString(fieldList: ArrayList<String>): String {
        return gson.toJson(fieldList)
    }
}