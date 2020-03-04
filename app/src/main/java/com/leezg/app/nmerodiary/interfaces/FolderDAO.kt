package com.leezg.app.nmerodiary.interfaces

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.leezg.app.nmerodiary.models.Folder

@Dao
interface FolderDAO {

    @RawQuery
    fun retrieveAllFolders(query: SupportSQLiteQuery): MutableList<Folder>

    @Query("SELECT * FROM Folder WHERE folderID = :folderID")
    fun retrieveFolderByID(folderID: Int): Folder

    @Query("SELECT folderID FROM Folder WHERE identifier = :identifier")
    fun retrieveFolderIDByIdentifier(identifier: String): Int

    @Insert
    fun insertFolder(folder: Folder)

    @Update
    fun updateFolder(folder: Folder)

    @Delete
    fun deleteFolder(folder: Folder)
}