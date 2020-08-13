package jasper.wagner.sensortracker.utils

import android.content.Context
import android.hardware.SensorEvent
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import jasper.wagner.sensortracker.extensions.show
import jasper.wagner.sensortracker.model.SensorData
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class GpxFile(private val context: Context) {

    /**
     * Writes locations to gpx file format
     *
     * @param file file for the gpx
     * @param n name for the file
     * @param points List of SensorEvents to be written to gpx format
     */
    fun createFile(file: File, author: String, time: String, points: ArrayList<SensorData>) {
        val header =
            "<gpx creator=\"Sensor Tracker\" version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"
        val metadata =
            " <metadata>\n" + "   <time>$time</time>" + "\n  </metadata>"
        val author = " <trk>\n  <author>$author</author>\n  <trkseg>\n"

        var segments = ""
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        val dataList = ArrayList<String>()

        for (i in 0 until (points.size - 1)) {

            dataList.add(
                "   <trkpt>\n" +
                        "    <sensor_name>" + points[i].sensorName + "</sensor_name>\n" +
                        "    <type>" + points[i].type + "</type>\n" +
                        "    <timestamp>" + points[i].timeStamp + "</timestamp>\n" +
                        "    <x_value>" + points[i].xValue + "</x_value>\n" +
                        "    <y_value>" + points[i].yValue + "</y_value>\n" +
                        "    <z_value>" + points[i].zValue + "</z_value>\n" +
                        "    <accuracy>" + points[i].accuracy + "</accuracy>\n" +
                        "    <resolution>" + points[i].resolution + "</resolution>\n" +
                        "    <time>" + df.format(Date()) + "Z</time>\n   " +
                        "</trkpt>\n"
            )
        }

        segments += dataList
        segments = segments.replace(",", "")
        segments = segments.replace("[", "")
        segments = segments.replace("]", "")

        val footer = "  </trkseg>\n </trk>\n</gpx>"

        try {
            val writer = FileWriter(file)

            writer.append(header)
            writer.append(metadata)
            writer.append(author)
            writer.append(segments)
            writer.append(footer)
            val fileEncoding = writer.encoding
            writer.flush()
            writer.close()

            Log.i(TAG, "Saved " + points.size + " points.")
            Log.i(TAG, "Saved $fileEncoding .")

        } catch (e: IOException) {
            Log.e(TAG, "Error Writing Path", e)
            val customToast = Toast(context)
            customToast.show(
                context,
                "Error while saving data: $e",
                Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK,
                Toast.LENGTH_SHORT
            )
        }
    }


    companion object {
        private val TAG = GpxFile::class.java.name
    }


}