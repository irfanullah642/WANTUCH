package com.example.wantuch.data.local.database

import android.content.Context
import androidx.room.*
import com.example.wantuch.data.local.dao.WantuchDao
import com.example.wantuch.data.local.entities.StaffEntity
import com.example.wantuch.data.local.entities.StudentEntity
import com.example.wantuch.data.local.entities.ClassEntity
import com.example.wantuch.data.local.entities.SectionEntity
import com.example.wantuch.data.local.entities.InstitutionEntity
import com.example.wantuch.data.local.entities.DashboardEntity
import com.example.wantuch.data.local.entities.PortfolioEntity

@Database(
    entities = [
        StudentEntity::class, 
        StaffEntity::class, 
        ClassEntity::class, 
        SectionEntity::class,
        InstitutionEntity::class,
        DashboardEntity::class,
        PortfolioEntity::class
    ], 
    version = 2, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WantuchDatabase : RoomDatabase() {

    abstract fun wantuchDao(): WantuchDao

    companion object {
        @Volatile
        private var INSTANCE: WantuchDatabase? = null

        fun getDatabase(context: Context): WantuchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WantuchDatabase::class.java,
                    "wantuch_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
