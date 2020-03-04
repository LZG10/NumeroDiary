package com.leezg.app.nmerodiary.interfaces

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.leezg.app.nmerodiary.models.Record
import com.leezg.app.nmerodiary.others.Constant

@Dao
interface RecordDAO {

    @Query("SELECT * FROM Record WHERE recordID = :id")
    fun retrieveRecordByID(id: String): Record?

    @Query("SELECT * FROM Record WHERE isTemplate = 0")
    fun retrieveAllRecords(): MutableList<Record>

    @Query("SELECT * FROM Record WHERE isTemplate = 1")
    fun retrieveAllTemplates(): MutableList<Record>

    @Query("SELECT * FROM Record WHERE searchString LIKE '%' || :searchText || '%' ORDER BY UPPER(:orderBy) ASC")
    fun searchAndSortRecordsByAscending(
        searchText: String,
        orderBy: String
    ): MutableList<Record>

    @RawQuery
    fun searchAndSortRecords(query: SupportSQLiteQuery): MutableList<Record>

    @Insert
    fun insertRecord(record: Record)

    @Update
    fun updateRecord(record: Record)

    @Delete
    fun deleteRecord(record: Record)
}