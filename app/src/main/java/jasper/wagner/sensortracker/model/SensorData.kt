package jasper.wagner.sensortracker.model


data class SensorData(
    val sensorName: String,
    val type: Int,
    val timeStamp: Long,
    val xValue: Float,
    val yValue: Float,
    val zValue: Float,
    val accuracy: Int,
    val resolution: Float
)
