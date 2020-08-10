package jasper.wagner.sensortracker.services

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationResult
import android.location.LocationManager
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import jasper.wagner.sensortracker.utils.Bearing.calculateBetween
import jasper.wagner.sensortracker.utils.Utils.round


class LocationProvider : Service(),
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var settingsBroadcastReceiver: BroadcastReceiver
    private lateinit var startTrackingReceiver: BroadcastReceiver

    private val mBinder = ServiceBinder()

    private var currentTime: Long? = null
    private var previousTime: Long? = null
    private var firstLocation: Location? = null
    private var newLocation: Location? = null
    private var prevLocation: Location? = null

    //override fun onBind(intent: Intent) = LocalBinder()

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class ServiceBinder : Binder() {
        val service: LocationProvider
            get() = this@LocationProvider
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(TAG, "GoogleApi Client Connected")

    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(TAG, "GoogleApi Client Suspended")
    }

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        Log.d(TAG, "GoogleApi Client Failed")
    }

    override fun onCreate() {
        super.onCreate()

        buildGoogleApiClient()
        //showNotificationAndStartForegroundService()
        mLocationCallback = LocationCallback()
        mFusedLocationProviderClient = FusedLocationProviderClient(applicationContext)
        createLocationRequest()
        localBroadcastManager =LocalBroadcastManager.getInstance(applicationContext!!)
        initSettingsUpdatedReceiver()
        initStartTrackingReceiver()

    }


    /**
     * Method used for building GoogleApiClient and add connection callback
     */
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()

        mGoogleApiClient.connect()
    }

    /**
     * Method used for creating location request
     * After successfully connection of the GoogleClient ,
     * This method used for to request continues location
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest.apply {
            priority = PRIORITY_HIGH_ACCURACY
            interval = DEFAULT_LOCATION_REQUEST_INTERVAL
            smallestDisplacement = DEFAULT_LOCATION_REQUEST_DISPLACEMENT
        }
        requestLocationUpdate()
    }

    private fun updateSettings(newDisplacement: Float, newTimeInterval: Long) {
        mLocationRequest.interval = newTimeInterval
        mLocationRequest.smallestDisplacement = newDisplacement
        requestLocationUpdate()
    }

    /**
     * Method used for the request new location using Google FusedLocation Api
     */
    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mFusedLocationProviderClient.lastLocation?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                prevLocation = newLocation
                newLocation = task.result
            } else {
                return@addOnCompleteListener//get the last location of the device
            }
        }

        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    onLocationChanged(locationResult!!.lastLocation)
                }
            },
            Looper.myLooper()
        )
    }

    private fun onLocationChanged(lastLocation: Location?) {
        if (newLocation == null){
            newLocation = lastLocation
            //sendBroadcastIntent()
            Log.i("Location Debugging", "first Location sent")
        } else {
            prevLocation = newLocation
            newLocation = lastLocation
            if (prevLocation?.latitude != newLocation?.latitude && prevLocation?.longitude != newLocation?.longitude) {
                //previousTime = currentTime
                //currentTime = System.currentTimeMillis()
                sendBroadcastIntent()
                Log.i("Location Debugging", "Location sent")
            }
            else {
                Log.i("Location Debugging", "Location didn't change")
            }
        }
    }

private fun sendBroadcastIntent() {
    val intent = Intent(BR_NEW_LOCATION)
    //intent.putExtra(LocationProvider.KEY_DISTANCE, getDistanceInMeters)
    //intent.putExtra(LocationProvider.KEY_TIME, timeElapsed)
    intent.putExtra(KEY_HEADING, getHeading)
    intent.putExtra(KEY_HEADING_CALC, getHeadingCalc)
    intent.putExtra(KEY_SPEED, getSpeed)
    intent.putExtra(KEY_ACCURACY, getGPSAccuracy)
    intent.putExtra(KEY_ALTITUDE, getAltitude)
    intent.putExtra(KEY_LOCATION, newLocation)
    intent.putExtra(KEY_DISTANCE, distanceSequment)
    intent.putExtra(KEY_PROVIDER_SOURCE, providerSource)
    //intent.putExtra(LocationProvider.KEY_CURRENT_TIME, currentTime)
    localBroadcastManager.sendBroadcast(intent)
    Log.i("Location Debugging", "sendBroadcast invoked")
}

private fun removeLocationUpdate() {
    mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
}

fun clearGpsData() {
    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, "delete_aiding_data", null)
    val bundle = Bundle()
    locationManager.sendExtraCommand("gps", "force_xtra_injection", bundle)
    locationManager.sendExtraCommand("gps", "force_time_injection", bundle)
    val criteria = Criteria()
    criteria.accuracy = Criteria.ACCURACY_FINE
    criteria.isSpeedRequired = false
    criteria.isAltitudeRequired = false
    criteria.isBearingRequired = false
    criteria.isCostAllowed = false
    criteria.powerRequirement = Criteria.NO_REQUIREMENT

    val bestProvider = locationManager.getBestProvider(criteria, true)

    if (bestProvider != null) {
        //locationManager.requestLocationUpdates(DEFAULT_LOCATION_REQUEST_INTERVAL, DEFAULT_LOCATION_REQUEST_DISPLACEMENT, criteria,mFusedLocationProviderClient)
    }
}

private val getGPSAccuracy: String
    get() {
        var acc = VALUE_MISSING
        newLocation?.let {
            if (it.hasAccuracy()) {
                acc = round(it.accuracy.toDouble(), 1).toString()
            }
        }
        return acc
    }

private val getSpeed: String
    get() {
        var speedString = VALUE_MISSING
        prevLocation?.let {
            val meters = prevLocation?.distanceTo(newLocation)
            if (previousTime != null) {
                val millis = currentTime!!.minus(previousTime!!)

                val speed = meters!! / (millis / 1000f)
                if (speed == Float.POSITIVE_INFINITY) {
                    Log.i("speed: ", "$speed")
                    Log.i("distance: ", "$millis")
                    Log.i("meters: ", "$meters")
                    return VALUE_MISSING
                }
                speedString = "${round(speed.toDouble(), 1)} m/s"
            }
        }
        return speedString
    }

val getHeading: String
    get() {
        var bearing = VALUE_MISSING
        prevLocation?.let {
            var bear = round((prevLocation!!.bearingTo(newLocation)).toDouble(), 0)
            if (bear < 0) {
                bear = 270 - (bear + 90)
            }
            bearing = "${bear.toInt()} °"
        }
        return bearing
    }

val getHeadingCalc: String
    get() {
        var bearing = VALUE_MISSING
        prevLocation?.let {
            var bear = calculateBetween(prevLocation!!, newLocation!!)
/*                if (bear < 0) {
                    bear = 270 - (bear + 90)
                }*/
            bearing = "${bear.toInt()} °"
        }
        return bearing
    }

val getAltitude: String
    get() {
        var altitude = VALUE_MISSING
        newLocation?.let {
            if (it.hasAltitude())
                altitude = "${round(it.altitude, 1)} m"
        }
        return altitude
    }

val providerSource: String
    get() {
        var provider = VALUE_MISSING
        newLocation?.let {

            provider = it.provider.toString()
        }
        return provider
    }

val distanceSequment: String
    get() {
        var distance = VALUE_MISSING
        newLocation?.let { new ->
            firstLocation?.let { first ->
                distance = "${round(new.distanceTo(first).toDouble(), 2)} m"
            }
        }
        return distance
    }


private fun initSettingsUpdatedReceiver() {
    settingsBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val newDisplacement =
                intent!!.getFloatExtra(
                    KEY_SET_REQUEST_DISPLACEMENT,
                    DEFAULT_LOCATION_REQUEST_DISPLACEMENT
                )
            val newTimeInterval =
                intent!!.getLongExtra(
                    KEY_SET_REQUEST_INTERVAL,
                    DEFAULT_LOCATION_REQUEST_INTERVAL
                )

            updateSettings(newDisplacement, newTimeInterval)
        }
    }
    val filter = IntentFilter(BR_NEW_SETTING)
    applicationContext.registerReceiver(settingsBroadcastReceiver, filter)
}

private fun initStartTrackingReceiver() {
    startTrackingReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            firstLocation = newLocation
        }
    }
    val filter = IntentFilter(BR_FIRST_LOCATION)
    applicationContext.registerReceiver(startTrackingReceiver, filter)
}

override fun onDestroy() {
    super.onDestroy()
    removeLocationUpdate()
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected) {
        mGoogleApiClient.disconnect()
    }
}

companion object {

    const val TAG = "LocationProvider"
    const val VALUES_BEFORE_UI_UPDATE = 8
    private const val DEFAULT_LOCATION_REQUEST_INTERVAL: Long = 100
    private const val DEFAULT_LOCATION_REQUEST_DISPLACEMENT = 0.0f
    //val CHANNEL_ID = BuildConfig.APPLICATION_ID.concat("_notification_id")
    //val CHANNEL_NAME = BuildConfig.APPLICATION_ID.concat("_notification_name")
    val NOTIFICATION_ID = 100

    const val BR_NEW_LOCATION = "BR_NEW_LOCATION"
    const val BR_FIRST_LOCATION = "BR_FIRST_LOCATION"
    const val KEY_DISTANCE = "KEY_DISTANCE"
    const val KEY_TIME = "KEY_TIME"
    const val KEY_CURRENT_TIME = "KEY_CURRENT_TIME"
    const val KEY_HEADING = "KEY_HEADING"
    const val KEY_HEADING_CALC = "KEY_HEADING_CALC"
    const val KEY_SPEED = "KEY_SPEED"
    const val KEY_PROVIDER_SOURCE = "KEY_PROVIDER_SOURCE"

    const val BR_NEW_SETTING = "BR_NEW_LOCATION"
    const val KEY_SET_REQUEST_DISPLACEMENT = "KEY_SET_REQUEST_DISPLACEMENT"
    const val KEY_SET_REQUEST_INTERVAL = "KEY_SET_REQUEST_INTERVAL"
    const val KEY_ACCURACY = "KEY_ACCURACY"
    const val KEY_ALTITUDE = "KEY_ALTITUDE"
    const val KEY_LOCATION = "KEY_LOCATION"

    const val VALUE_MISSING = "--"
}
}