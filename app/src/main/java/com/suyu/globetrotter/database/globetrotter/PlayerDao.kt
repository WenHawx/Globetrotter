package com.suyu.globetrotter.database.globetrotter

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// This class provides access to the data in the database
@Dao
interface PlayerDao {
    //Get list of player data
    @Query("SELECT * FROM player ORDER BY id ASC")
    fun getAllPlayers(): Flow<List<Player>>

    //get specific player's info
    @Query("SELECT * FROM player WHERE id = :id")
    fun getPlayerInfo(id: Int): Flow<Player>

    @Update
    suspend fun update(player: Player)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(player: Player)

    @Delete
    suspend fun delete(player: Player)
}