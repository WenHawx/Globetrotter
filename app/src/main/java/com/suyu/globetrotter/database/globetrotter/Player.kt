package com.suyu.globetrotter.database.globetrotter

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// This file defines the various data classes that are used in the database
// Each class is a table within the database
@Entity
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @NonNull @ColumnInfo(name = "player_name") val playerName: String,
    @NonNull @ColumnInfo(name= "current_score") val currentScore: Double,
    @NonNull @ColumnInfo(name="origin_lat") val oriLat: Double,
    @NonNull @ColumnInfo(name="origin_long") val oriLong: Double
)
