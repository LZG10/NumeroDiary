package com.leezg.app.nmerodiary.others

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leezg.app.nmerodiary.models.Field
import com.leezg.app.nmerodiary.models.Record


class RecordTypeConverter {

    private val gson = Gson()

    @TypeConverter
    fun stringToList(data: String?): ArrayList<Field> {
        if (data == null)
            return ArrayList()

        val listType = object : TypeToken<ArrayList<Field>>() {}.type
        return gson.fromJson<ArrayList<Field>>(data, listType)
    }

    @TypeConverter
    fun listToString(fieldList: ArrayList<Field>): String {
        return gson.toJson(fieldList)
    }
}