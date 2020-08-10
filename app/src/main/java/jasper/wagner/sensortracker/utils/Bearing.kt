package jasper.wagner.sensortracker.utils

import android.location.Location
import java.lang.Math.*
import kotlin.math.abs

object Bearing {

    fun calculateBetween(start: Location, end: Location): Double {
        val lat1 = toRadians(start.latitude)
        val lat2 = toRadians(end.latitude)
        val lon1 = toRadians(start.longitude)
        val lon2 = toRadians(end.longitude)
        var deltaLon = lon2 - lon1
        val deltaPhi = log(
            tan(lat2 / 2 + PI / 4)
                    /
                    tan(lat1 / 2 + PI / 4)
        )
        if (abs(deltaPhi) > PI) {
            if (deltaLon > 0.0) {
                deltaLon = -(2.0 * PI - deltaLon)
            } else {
                deltaLon += 2 * PI
            }
        }
        return (toDegrees(
            atan2(deltaLon, deltaPhi)) + 360.0) %360
    }
}