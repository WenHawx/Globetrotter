package com.suyu.globetrotter.database.globetrotter

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// This is a class that allows app to interact with the database
@Database(entities = [Player::class], version = 1)
abstract class GlobetrotterDatabase: RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    companion object {
        @Volatile
        private var INSTANCE: GlobetrotterDatabase? = null

        /* This function either returns the existing instance of the database(if it already exist)
           or creates the database for the first time if needed*/
        fun getDatabase(context: Context): GlobetrotterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    GlobetrotterDatabase::class.java,
                    "globetrotter_database")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                return instance
            }
        }
    }
}