package com.leezg.app.nmerodiary.interfaces

import androidx.room.*
import com.leezg.app.nmerodiary.models.Field

@Dao
interface FieldDAO {

    @Query("SELECT * FROM Field WHERE identifier IN (:identifiers) ORDER BY UPPER(:orderBy)")
    fun retrieveAllFields(orderBy: String, vararg identifiers: String): MutableList<Field>

    @Query("SELECT * FROM Field WHERE fieldID = :id")
    fun retrieveFieldByID(id: String): Field?

    @Query("SELECT DISTINCT fieldName FROM Field WHERE fieldName != '' AND identifier = :identifier ORDER BY fieldName")
    fun retrieveDistinctFieldName(identifier: String): MutableList<String>

    @Query("SELECT DISTINCT fieldUnit FROM Field WHERE fieldUnit != '' AND identifier = :identifier ORDER BY fieldUnit")
    fun retrieveDistinctFieldUnit(identifier: String): MutableList<String>

    @Query("DELETE FROM Field WHERE identifier = :identifier")
    fun deleteFieldByIdentifier(identifier: String)

    @Insert
    fun insertField(field: Field)

    @Update
    fun updateField(field: Field)

    @Delete
    fun deleteField(field: Field)
}