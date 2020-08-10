package jasper.wagner.sensortracker.ui.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jasper.wagner.sensortracker.R
import jasper.wagner.sensortracker.services.LocationProvider
import jasper.wagner.sensortracker.utils.GpxFile
import java.io.File
import jasper.wagner.sensortracker.utils.Utils.getDate
import kotlin.math.roundToInt


class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    //private val context = getApplication<Application>().baseContext
    private val _accAccuracy = MutableLiveData<Int>()
    val accAccuracy: LiveData<Int>
        get() = _accAccuracy

    private val _speed = MutableLiveData<Float>(0F)
    val speed: LiveData<Float>
        get() = _speed

    private val _locationList = MutableLiveData<ArrayList<Location>>()
    val locationList: LiveData<ArrayList<Location>>
        get() = _locationList

    private val _heading = MutableLiveData<String>()
    val heading: LiveData<String>
        get() = _heading

    private val _headingCalc = MutableLiveData<String>()
    val headingCalc: LiveData<String>
        get() = _headingCalc

    private val _altitude = MutableLiveData<String>()
    val altitude: LiveData<String>
        get() = _altitude

    private val _elapsedTimeCurrentRun = MutableLiveData<String>()
    val elapsedTimeCurrentRun: LiveData<String>
        get() = _elapsedTimeCurrentRun

    private val _distanceCurrentRun = MutableLiveData<String>()
    val distanceCurrentRun: LiveData<String>
        get() = _distanceCurrentRun

    private val _providerSource = MutableLiveData<String>()
    val providerSource: LiveData<String>
        get() = _providerSource


    private val _accelerometerReading = MutableLiveData<FloatArray>()
    val accelerometerReading: LiveData<FloatArray>
        get() = _accelerometerReading

    private val _magnetorReading = MutableLiveData<FloatArray>()
    val magnetorReading: LiveData<FloatArray>
        get() = _magnetorReading

    private val _accX = MutableLiveData<Float>(0F)
    val accX: LiveData<Float>
        get() = _accX

    private val _accZ = MutableLiveData<Float>(0F)
    val accZ: LiveData<Float>
        get() = _accZ

    private val _deltaT = MutableLiveData<Float>(0F)
    val deltaT: LiveData<Float>
        get() = _deltaT

    private var timeStep: Long? = null



    fun enableGPS(context: Activity) {
        context.startService(Intent(context, LocationProvider::class.java))
    }

    fun disableGPS(context: Activity) {
        context.stopService(Intent(context, LocationProvider::class.java))
    }

    fun updateUI(
        speed: String?, heading: String?, headingCalc: String?, altitude: String?,
        accuracy: String?, providerSource: String?
    ) {

//        _gpsAccuracy.value = accuracy
//        _speed.value = speed
        _heading.value = heading
        _headingCalc.value = headingCalc
        _altitude.value = altitude
        _providerSource.value = providerSource
    }

    fun getLastFileNumber(directory: File): Int {
        val files = directory.listFiles()
        var lastFileNameNumber = 0
        if (files != null) {
            Log.i(TAG, "$files")
            for (file in files) {
                Log.i(TAG, "$file")
                if (file != null) {
                    val filename = file.name
                    if (filename.startsWith("${getDate()}")) {
                        var number = filename.substringAfter("${getDate()}_")
                            .substringBefore(".gpx")
                        //lastFileNameNumber = number.toInt()
                        Log.i(TAG, "$filename")
                        Log.i(TAG, "$number")
                        lastFileNameNumber = number.toInt()
                    }
                }
            }
        }
        return lastFileNameNumber
    }

    fun addToList(location: Location) {
        if (_locationList.value!!.isNullOrEmpty()) {
            _locationList.value = ArrayList()
        }
        _locationList.value!!.add(location)
    }

    fun startTracking() {
        _locationList.value = ArrayList()
    }

    fun saveTracking(context: Activity, filename: String, time: String) {

        Log.i(TAG, "trying to save file $filename.gpx ...")

        val file = File(context.filesDir, "$filename.gpx")
        if (!file.exists()) {
            val directory = context.filesDir
            directory.mkdir()
        }
        val author = context.getString(R.string.AUTHOR)

        val gpxFile = GpxFile(context)
        try {
            gpxFile.createFile(file, author, time, locationList.value!!)
            Log.i(TAG, "File ${file.name} successfully saved")
        } catch (e: Exception) {
            Log.e(TAG, "Not completed saving file: " + file.name + " " + e)
        }
    }

    fun update(distanceCurrentRun: String?, time_elapsed: String) {
        _distanceCurrentRun.value = distanceCurrentRun
        _elapsedTimeCurrentRun.value = time_elapsed
    }

    fun processAccelerometerData(accelerometerReading: FloatArray) {
        _accelerometerReading.value = accelerometerReading
        if(accelerometerReading[0] > 0.1F) {
        _accX.value = _accX.value!! + accelerometerReading[0]
        _accZ.value = _accZ.value!! + accelerometerReading[2]
        Log.d("SpeedChange", "acc: ${accelerometerReading[0]}")

            addTimeStep()
        Log.d("SpeedChange", "delta t: ${deltaT.value!!}")

        }

    }

    private fun addTimeStep() {
        if (timeStep == null) {
            timeStep = System.currentTimeMillis()
        } else {
            val timeDiff = System.currentTimeMillis() - timeStep!!
            _deltaT.value = _deltaT.value!! + (timeDiff / 1000F)
            timeStep = System.currentTimeMillis()
        }
    }

    fun resetAcc() {
        _accX.postValue( 0F)
        _accZ.postValue( 0F)
    }
    
    fun processMagnetData(magnetorReading: FloatArray) {
        _magnetorReading.value = magnetorReading
        val degree = magnetorReading[0].roundToInt()

    }

    fun updateAcc(accuracy: Int) {
        _accAccuracy.value = accuracy
    }

    fun calculateSpeedChange() {
        _speed.postValue(_accX.value!! * _deltaT.value!!)
        resetAcc()
    }

    fun resetDeltaT() {
        _deltaT.postValue(0F)
    }


    companion object {
        private val TAG = MainViewModel::class.java.name
    }
}
