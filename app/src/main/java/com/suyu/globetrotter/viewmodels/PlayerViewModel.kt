package com.suyu.globetrotter.viewmodels

import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.database.globetrotter.PlayerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.abs

// View Model class for data handling between fragments
class PlayerViewModel(private val playerDao: PlayerDao) : ViewModel() {

    private var locName: String = ""
    private var locLatLng = LatLng(0.0,0.0)
    private var locAddress: String = ""

    fun getLocName(): String {
        return locName
    }
    fun getLocLatLng(): LatLng {
        return locLatLng
    }
    fun getLocAddress(): String {
        return locAddress
    }
    fun setLocName(newVal: String){
        locName = newVal
    }
    fun setLocLatLng(newVal: LatLng){
        locLatLng = newVal
    }
    fun setAddress(newVal: String){
        locAddress = newVal
    }

    val allPlayers: LiveData<List<Player>> = playerDao.getAllPlayers().asLiveData()

    // Add new Player functions
    private fun insertPlayer(player: Player) {
        viewModelScope.launch {
            playerDao.insert(player)
        }
    }
    private fun getNewPlayerEntry(playerName: String,
                                  oriLat: Double, oriLong: Double) : Player{
        return Player(
            playerName = playerName,
            currentScore = 0.0,
            oriLat = oriLat,
            oriLong = oriLong
        )
    }
    fun addNewPlayer(playerName: String, oriLat: Double, oriLong: Double){
        val newPlayer = getNewPlayerEntry(playerName, oriLat, oriLong)
        insertPlayer(newPlayer)
    }
    fun isEntryValid(playerName: String) : Boolean {
        if(playerName.isBlank()) {
            return false
        }
        return true
    }

    fun getInfo(id: Int): LiveData<Player> {
        return playerDao.getPlayerInfo(id).asLiveData()
    }

    private fun updatePlayer(player: Player) {
        viewModelScope.launch {
            playerDao.update(player)
        }
    }

    fun updateScore(player: Player, oriLat: Double, oriLong: Double) {
        var newScore = getPointValue(player, oriLat, oriLong)
        val newPlayer = player.copy(currentScore = player.currentScore + newScore)
        updatePlayer(newPlayer)
    }

    fun updateOriLatLng(player: Player, oriLat: Double, oriLong: Double){
        val newPlayer = player.copy(oriLat = oriLat, oriLong = oriLong)
        updatePlayer(newPlayer)
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            playerDao.delete(player)
        }
    }

    fun getPointValue(player: Player, oriLat: Double, oriLong: Double): Double{
        return (abs(oriLat - player.oriLat) + abs(oriLong - player.oriLong)) % 100
    }

}

// Instantiate  view models
class PlayerViewModelFactory(
    private val playerDao: PlayerDao
) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(playerDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}