package com.suyu.globetrotter

import android.app.Application
import com.suyu.globetrotter.database.globetrotter.GlobetrotterDatabase

class GlobetrotterApplication: Application() {
    val database : GlobetrotterDatabase by lazy { GlobetrotterDatabase.getDatabase(this)}
}