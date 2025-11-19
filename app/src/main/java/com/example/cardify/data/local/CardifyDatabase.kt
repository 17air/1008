package com.example.cardify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [GroupEntity::class, MembershipEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardifyDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: CardifyDatabase? = null

        fun getInstance(context: Context): CardifyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): CardifyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CardifyDatabase::class.java,
                "cardify.db"
            ).fallbackToDestructiveMigration().build()
        }
    }
}
