package jasper.wagner.sensortracker.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import jasper.wagner.sensortracker.R
import jasper.wagner.sensortracker.extensions.show
import jasper.wagner.sensortracker.services.LocationProvider
import jasper.wagner.sensortracker.services.LocationProvider.Companion.BR_FIRST_LOCATION
import jasper.wagner.sensortracker.services.LocationProvider.Companion.VALUE_MISSING
import jasper.wagner.sensortracker.utils.Utils.getDate
import jasper.wagner.sensortracker.utils.Utils.getTime
import jasper.wagner.sensortracker.utils.Utils.round
import java.util.*
import kotlin.math.roundToInt


class MainFragment(private val baseContext: Context) : Fragment(), SensorEventListener {

    private lateinit var locationBroadcastReceiver: BroadcastReceiver
    private lateinit var viewModel: MainViewModel
    private var switchProvider: Switch? = null
    private var switchTracking: Switch? = null
    private var tvAccAccuracy: TextView? = null
    private var tvSpeed: TextView? = null
    private var tvHeadingX: TextView? = null
    private var tvHeadingY: TextView? = null
    private var tvHeadingZ: TextView? = null

    private var tvAccX: TextView? = null
    private var tvAccY: TextView? = null
    private var tvAccZ: TextView? = null

    private var fileName: String = ""
    private var fileNameNumber: Int = 0
    private var startTime: Long? = null
    private var trackingIsRunning = false
    private var gpsIsEnabled = false
    private lateinit var customToast: Toast

    // record the compass picture angle turned
    private var currentDegree = 0f

    // device sensor manager
    private lateinit var mSensorManager: SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.main_fragment, container, false)
        initializeView(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        fileNameNumber = viewModel.getLastFileNumber(context!!.filesDir)
        initBroadcastReceiver()
        initializeSensor()

        LocalBroadcastManager.getInstance(activity!!.applicationContext)
            .registerReceiver(locationBroadcastReceiver, IntentFilter(LocationProvider.BR_NEW_LOCATION))
        observeLiveDataChanges()

        startTimer()
        customToast = Toast(context)
    }

    private fun startTimer() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                viewModel.calculateSpeedChange()
                viewModel.resetDeltaT()
            }
        }, 0, 5000) //put here time 1000 milliseconds=1 second

    }

    private fun observeLiveDataChanges() {

        viewModel.accAccuracy.observe(this.viewLifecycleOwner, Observer {
            tvAccAccuracy!!.text = it.toString()
        })

        viewModel.speed.observe(this.viewLifecycleOwner, Observer {
            tvSpeed!!.text = it.toString()
        })

        viewModel.accelerometerReading.observe(this.viewLifecycleOwner, Observer {
            tvAccX!!.text = round(it[0], 1).toString()
            tvAccY!!.text = round(it[1], 1).toString()
            tvAccZ!!.text = round(it[2], 1).toString()
        })

        viewModel.magnetorReading.observe(this.viewLifecycleOwner, Observer {
            tvHeadingX!!.text = "${round(it[0], 1)} °"
            tvHeadingY!!.text = "${round(it[1], 1)} °"
            tvHeadingZ!!.text = "${round(it[2], 1)} °"
        })

    }

    override fun onResume() {
        super.onResume()
//        LocalBroadcastManager.getInstance(activity!!.applicationContext)
//            .registerReceiver(
//                locationBroadcastReceiver,
//                IntentFilter(LocationProvider.BR_NEW_LOCATION)
//            )
        observeLiveDataChanges()
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onDestroy() {
        super.onDestroy()
        //context!!.unregisterReceiver(locationBroadcastReceiver)
        LocalBroadcastManager.getInstance(activity!!.applicationContext)
            .unregisterReceiver(locationBroadcastReceiver)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    private fun initializeView(view: View) {
        tvAccAccuracy = view.findViewById(R.id.tvAccAccuracy)
        tvSpeed = view.findViewById(R.id.tvSpeed)
        tvHeadingX = view.findViewById(R.id.tvHeadingX)
        tvHeadingY = view.findViewById(R.id.tvHeadingY)
        tvHeadingZ = view.findViewById(R.id.tvHeadingZ)
        tvAccX = view.findViewById(R.id.tvAccX)
        tvAccY = view.findViewById(R.id.tvAccY)
        tvAccZ = view.findViewById(R.id.tvAccZ)

        switchTracking = view.findViewById(R.id.switchTracking)
        switchProvider = view.findViewById(R.id.switchProvider)
        switchProvider!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                viewModel.enableGPS(activity!!)
                gpsIsEnabled = true
                customToast.show(
                    context,
                    "GPS Provider enabled",
                    Gravity.BOTTOM,
                    Toast.LENGTH_SHORT
                )
            } else {
                viewModel.disableGPS(activity!!)
                gpsIsEnabled = false
                try {
                    context!!.unregisterReceiver(locationBroadcastReceiver)
                } catch (e: Exception) {
                }
                customToast.show(
                    context,
                    "GPS Provider disabled",
                    Gravity.BOTTOM,
                    Toast.LENGTH_SHORT
                )
            }
        }

        switchTracking!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked && gpsIsEnabled) {
                trackingIsRunning = true
                fileNameNumber += 1
                sendStartTrackingIntent()
                setStartTime()
                viewModel.startTracking()
                fileName = "${getDate()}_$fileNameNumber"
                customToast.show(context, "Tracking started", Gravity.BOTTOM, Toast.LENGTH_SHORT)
            } else if (!isChecked && gpsIsEnabled) {
                trackingIsRunning = false
                showSaveDialog()
                viewModel.update(VALUE_MISSING, VALUE_MISSING)
            } else if (isChecked && !gpsIsEnabled) {
                customToast.show(
                    context,
                    "gps not enabled",
                    Gravity.CENTER_VERTICAL,
                    Toast.LENGTH_LONG,
                    isErrorToast = true
                )
            } else if (!isChecked && !gpsIsEnabled) {
                customToast.show(
                    context,
                    "gps not enabled",
                    Gravity.CENTER_VERTICAL,
                    Toast.LENGTH_LONG,
                    isErrorToast = true
                )
            }
        }
    }

    private fun initializeSensor() {
        // initialize your android device sensor capabilities
        // 1

        mSensorManager = baseContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // 2
        mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.also { accelerometer ->
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }

        // 3
        mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(context!!, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
        builder.setTitle("Save Tracking")
        builder.setMessage("Are you sure, you want to save the tracked route?")

        builder.setPositiveButton("YES") { _, _ ->
            viewModel.saveTracking(activity!!, fileName, getTime())
            customToast.show(context, "Tracking saved", Gravity.BOTTOM, Toast.LENGTH_SHORT)
        }

        builder.setNegativeButton("NO") { dialog, which ->
            fileNameNumber -= 1
            customToast.show(context, "Tracking discarded", Gravity.BOTTOM, Toast.LENGTH_SHORT)
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun setStartTime() {
        startTime = System.currentTimeMillis()
    }

    private val getTimeElapsed: String
        get() {
            var time = VALUE_MISSING
            val currentTime = System.currentTimeMillis()
            startTime?.let {
                val diff = round(((currentTime - it) / 1000.0), 1)
                time = "$diff s"
            }
            return time
        }


    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)

            viewModel.processAccelerometerData(accelerometerReading, event.timestamp)

//            viewModel.processAccelerometerData(event.values)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            viewModel.processMagnetData(magnetometerReading)
        }

        updateOrientationAngles()

        val degree = event.values[0].roundToInt()

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_ACCELEROMETER) {
            viewModel.updateAcc(accuracy)
        }
    }


    private fun sendStartTrackingIntent() {
        val intent = Intent(BR_FIRST_LOCATION)
        LocalBroadcastManager.getInstance(this.context!!).sendBroadcast(intent)
    }

    private fun initBroadcastReceiver() {
        locationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                intent?.let {
                    val heading = it.getStringExtra(LocationProvider.KEY_HEADING)
                    val headingCalc = it.getStringExtra(LocationProvider.KEY_HEADING_CALC)
                    val speed = it.getStringExtra(LocationProvider.KEY_SPEED)
                    val accuracy = it.getStringExtra(LocationProvider.KEY_ACCURACY)
                    val providerSource = it.getStringExtra(LocationProvider.KEY_PROVIDER_SOURCE)
                    val distanceCurrentRun = it.getStringExtra(LocationProvider.KEY_DISTANCE)
                    val altitude = intent.getStringExtra(LocationProvider.KEY_ALTITUDE)
                    val location =
                        intent.getParcelableExtra<Location>(LocationProvider.KEY_LOCATION)

                    viewModel.updateUI(
                        speed,
                        heading,
                        headingCalc,
                        altitude,
                        accuracy,
                        providerSource
                    )
                    if (trackingIsRunning) {
                        viewModel.update(distanceCurrentRun, getTimeElapsed)
                        viewModel.addToList(location)
                    }
                    //locationList.add(location)
                }
            }
        }

//        val filter = IntentFilter(LocationProvider.BR_NEW_LOCATION)
//        context!!.registerReceiver(locationBroadcastReceiver, filter)
    }

    private fun updateOrientationAngles() {
        // 1
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        // 2
        val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // 3
        val degrees = (Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0
        // 4
        val angle = round(degrees, 0)

    }

    companion object {
        fun getInstance(baseContext: Context): MainFragment {
            return MainFragment(baseContext)
        }
    }
}
