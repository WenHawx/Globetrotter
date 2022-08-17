package com.suyu.globetrotter.database.globetrotter

import com.google.android.gms.maps.model.LatLng

class ChosenLocation(locName: String, locLat: Double, locLng: Double, locAddress: String) {
    private var name: String = locName
    private var oriLng: Double = locLat
    private var oriLat: Double = locLng
    private var address: String = locAddress

    fun getName(): String {
        return name
    }

    fun getLat(): Double {
        return oriLat
    }

    fun getLng(): Double {
        return oriLng
    }
    fun getAddress(): String {
        return address
    }

    fun setName(nameVal: String){
        name = nameVal
    }

    fun setLat(latVal: Double){
        oriLat = latVal
    }

    fun setLng(lngVal: Double){
        oriLng = lngVal
    }

    fun setAddress(addVal: String){
        address = addVal
    }

}