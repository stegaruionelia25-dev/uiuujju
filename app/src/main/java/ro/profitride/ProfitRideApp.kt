package ro.profitride

import android.app.Application
import ro.profitride.data.AutoStore
import ro.profitride.data.ProfileStore
import ro.profitride.data.RideStore

class ProfitRideApp : Application() {

    lateinit var profileStore: ProfileStore
        private set
    lateinit var rideStore: RideStore
        private set
    lateinit var autoStore: AutoStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        profileStore = ProfileStore(this)
        rideStore = RideStore(this)
        autoStore = AutoStore(this)
    }

    companion object {
        lateinit var instance: ProfitRideApp
            private set
    }
}
