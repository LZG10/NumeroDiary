package com.leezg.app.nmerodiary.others

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leezg.app.nmerodiary.interfaces.FieldDAO
import com.leezg.app.nmerodiary.interfaces.FolderConditionDAO
import com.leezg.app.nmerodiary.interfaces.FolderDAO
import com.leezg.app.nmerodiary.interfaces.RecordDAO
import com.leezg.app.nmerodiary.models.Field
import com.leezg.app.nmerodiary.models.Folder
import com.leezg.app.nmerodiary.models.FolderCondition
import com.leezg.app.nmerodiary.models.Record


@Database(
    entities = [Record::class, Field::class, Folder::class, FolderCondition::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDAO(): RecordDAO
    abstract fun fieldDAO(): FieldDAO
    abstract fun folderDAO(): FolderDAO
    abstract fun folderConditionDAO(): FolderConditionDAO

    companion object {

        private var sInstance: AppDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): AppDatabase? {
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = Room.databaseBuilder<AppDatabase>(
                        context,
                        AppDatabase::class.java, Constant.DATABASE_NAME
                    )
                        .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8
                        )
                        .build()
                }
            }
            return sInstance
        }

        // Database schema update
        // 1. Change database version
        // 2. Add migration item as below to handle version upgrade
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Record_Backup` (`timestampCreated` INTEGER NOT NULL, `userID` TEXT NOT NULL, `position` INTEGER NOT NULL, `isTemplate` INTEGER NOT NULL, `templateName` TEXT NOT NULL, `templateID` TEXT NOT NULL, `recordRemarks` TEXT NOT NULL, `docID` TEXT NOT NULL, `fieldList` TEXT NOT NULL, `recordID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordTitle` TEXT NOT NULL, `timestampModified` INTEGER NOT NULL, `fieldID` TEXT NOT NULL)")
                database.execSQL(
                    "INSERT INTO `Record_Backup` SELECT `timestampCreated`, `userID`, `position`, `isTemplate`, `templateName`, `templateID`, `recordRemarks`, `docID`, `fieldList`, `recordID`, `recordTitle`, `timestampModified`, '' FROM `Record`"
                )
                database.execSQL("DROP TABLE `Record`")
                database.execSQL("ALTER TABLE `Record_Backup` RENAME TO `Record`")
                //database.execSQL("ALTER TABLE Record ADD COLUMN docID TEXT")
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Record_Backup` (`timestampCreated` INTEGER NOT NULL, `userID` TEXT NOT NULL, `isTemplate` INTEGER NOT NULL, `templateName` TEXT NOT NULL, `templateID` TEXT NOT NULL, `recordRemarks` TEXT NOT NULL, `docID` TEXT NOT NULL, `fieldList` TEXT NOT NULL, `recordID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordTitle` TEXT NOT NULL, `timestampModified` INTEGER NOT NULL)")
                database.execSQL(
                    "INSERT INTO `Record_Backup` SELECT `timestampCreated`, `userID`, `isTemplate`, `templateName`, `templateID`, `recordRemarks`, `docID`, `fieldList`, `recordID`, `recordTitle`, `timestampModified` FROM `Record`"
                )
                database.execSQL("DROP TABLE `Record`")
                database.execSQL("ALTER TABLE `Record_Backup` RENAME TO `Record`")
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Record_Backup` (`timestampCreated` INTEGER NOT NULL, `userID` TEXT NOT NULL, `isTemplate` INTEGER NOT NULL, `templateName` TEXT NOT NULL, `templateID` TEXT NOT NULL, `recordRemarks` TEXT NOT NULL, `docID` TEXT NOT NULL, `fieldList` TEXT NOT NULL, `recordID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordTitle` TEXT NOT NULL, `timestampModified` INTEGER NOT NULL, `recordImgList` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `identifier` TEXT NOT NULL)")
                database.execSQL(
                    "INSERT INTO `Record_Backup` SELECT `timestampCreated`, `userID`, `isTemplate`, `templateName`, `templateID`, `recordRemarks`, `docID`, `fieldList`, `recordID`, `recordTitle`, `timestampModified`, `recordImgList`, `isPinned`, '' FROM `Record`"
                )
                database.execSQL("DROP TABLE `Record`")
                database.execSQL("ALTER TABLE `Record_Backup` RENAME TO `Record`")

                database.execSQL("CREATE TABLE IF NOT EXISTS `Field_Backup` (`isChecked` INTEGER NOT NULL, `fieldID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordID` INTEGER NOT NULL, `fieldImgList` TEXT NOT NULL, `fieldName` TEXT NOT NULL, `fieldValue` TEXT NOT NULL, `fieldType` INTEGER NOT NULL, `identifier` TEXT NOT NULL)")
                database.execSQL("INSERT INTO `Field_Backup` SELECT `isChecked`, `fieldID`, `recordID`, `fieldImgList`, `fieldName`, `fieldValue`, `fieldType`, '' FROM `Field`")
                database.execSQL("DROP TABLE `Field`")
                database.execSQL("ALTER TABLE `Field_Backup` RENAME TO `Field`")
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Field_Backup` (`isChecked` INTEGER NOT NULL, `fieldID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fieldImgList` TEXT NOT NULL, `fieldName` TEXT NOT NULL, `fieldValue` TEXT NOT NULL, `fieldType` INTEGER NOT NULL, `identifier` TEXT NOT NULL, `fieldUnit` TEXT NOT NULL)")
                database.execSQL("INSERT INTO `Field_Backup` SELECT `isChecked`, `fieldID`, `fieldImgList`, `fieldName`, `fieldValue`, `fieldType`, `identifier`, '' FROM `Field`")
                database.execSQL("DROP TABLE `Field`")
                database.execSQL("ALTER TABLE `Field_Backup` RENAME TO `Field`")
            }
        }

        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `Folder` (`timestampCreated` INTEGER NOT NULL, `userID` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `identifier` TEXT NOT NULL, `recordIDList` TEXT NOT NULL, `folderID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folderName` TEXT NOT NULL, `timestampModified` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Folder ADD COLUMN folderType INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("CREATE TABLE IF NOT EXISTS `Record_Backup` (`timestampCreated` INTEGER NOT NULL, `userID` TEXT NOT NULL, `isTemplate` INTEGER NOT NULL, `templateName` TEXT NOT NULL, `templateID` TEXT NOT NULL, `recordRemarks` TEXT NOT NULL, `fieldList` TEXT NOT NULL, `recordID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordTitle` TEXT NOT NULL, `timestampModified` INTEGER NOT NULL, `recordImgList` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `identifier` TEXT NOT NULL, `docIDList` TEXT NOT NULL)")
                database.execSQL(
                    "INSERT INTO `Record_Backup` SELECT `timestampCreated`, `userID`, `isTemplate`, `templateName`, `templateID`, `recordRemarks`, `fieldList`, `recordID`, `recordTitle`, `timestampModified`, `recordImgList`, `isPinned`, `identifier`, '' FROM `Record`"
                )
                database.execSQL("DROP TABLE `Record`")
                database.execSQL("ALTER TABLE `Record_Backup` RENAME TO `Record`")
            }
        }
    }
}