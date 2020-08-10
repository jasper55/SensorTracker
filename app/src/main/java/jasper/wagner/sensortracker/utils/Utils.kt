package jasper.wagner.sensortracker.utils

import java.util.*


object Utils {

    fun round(value: Double, places: Int): Double {
        var value = value
        require(places >= 0)

        val factor = Math.pow(10.0, places.toDouble()).toLong()
        value *= factor
        val tmp = Math.round(value)
        return tmp.toDouble() / factor
    }

    fun round(value: Float, places: Int): Double {
        var value = value
        require(places >= 0)

        val factor = Math.pow(10.0, places.toDouble()).toLong()
        value *= factor
        val tmp = Math.round(value)
        return tmp.toDouble() / factor
    }

    fun getDate(): String {
        Calendar.getInstance(Locale.GERMAN).apply {
            val day = get(Calendar.DAY_OF_MONTH)
            val month = get(Calendar.MONTH) + 1
            val year = get(Calendar.YEAR)
            return "$day-$month-$year"
        }
    }

    fun getTime(): String {
        Calendar.getInstance(Locale.GERMAN).apply {
            val day = get(Calendar.DAY_OF_MONTH)
            val month = get(Calendar.MONTH) + 1
            val year = get(Calendar.YEAR)
            val hour = get(Calendar.HOUR)
            val minute = get(Calendar.MINUTE)
            return "$day-$month-$year $hour:$minute"
        }
    }
    val VMG_FRAGMENT_TAG = 1
    val CCOMPASS_FRAGMENT_TAG = 2
    val SPEED_BEARING_FRAGMENT_TAG = 0

}