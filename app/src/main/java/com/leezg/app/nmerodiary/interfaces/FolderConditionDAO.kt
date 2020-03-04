package com.leezg.app.nmerodiary.interfaces

import androidx.room.*
import com.leezg.app.nmerodiary.models.FolderCondition

@Dao
interface FolderConditionDAO {

    @Query("SELECT * FROM FolderCondition WHERE folderID = :folderID")
    fun retrieveConditionByID(folderID: Int): MutableList<FolderCondition>

    @Query("DELETE FROM FolderCondition WHERE folderID = :folderID AND folderConditionID = :index")
    fun deleteFolderConditionByID(folderID: Int, index: Int)

    @Query("DELETE FROM FolderCondition WHERE folderID = :folderID")
    fun deleteFolderCondition(folderID: Int)

    @Insert
    fun insertFolderCondition(condition: FolderCondition)

    @Update
    fun updateFolderCondition(condition: FolderCondition)

    @Delete
    fun deleteFolderCondition(condition: FolderCondition)
}