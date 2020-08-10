package jasper.wagner.sensortracker.utils


import android.content.Context
import android.location.Location
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import jasper.wagner.sensortracker.extensions.show
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class GpxFile(val context: Context) {


    /**
     * Writes locations to gpx file format
     *
     * @param file file for the gpx
     * @param n name for the file
     * @param points List of locations to be written to gpx format
     */
    fun createFile(file: File, author: String, time: String, points: ArrayList<Location>) {
        val header =
            "<gpx creator=\"GPS Tracker\" version=\"1.1\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"
        val metadata =
            " <metadata>\n" + "   <time>$time</time>" + "\n  </metadata>"
        val author = " <trk>\n  <author>$author</author>\n  <trkseg>\n"

        var segments = ""
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")


        val stockList = ArrayList<String>()


        for (i in 0 until (points.size - 1)) {

            if (i == 0) {
                stockList.add(
                    "   <trkpt lat=\"" + points[i].latitude + "\" lon=\"" + points[i].longitude + "\">\n" +
                            "    <accuracy>" + points[i].accuracy + "</accuracy>\n" +
                            "    <bearing>" + points[i].bearing + "</bearing>\n" +
                            "    <speed>" + points[i].speed + "</speed>\n" +
                            "    <time_elapsed>" + points[i].elapsedRealtimeNanos + "</time_elapsed>\n" +
                            "    <provider>" + points[i].provider + "</provider>\n" +
                            "    <distance>" + 0 + "</distance>\n" +
                            "    <elevation>" + points[i].altitude + "</elevation>\n" +
                            "    <time>" + df.format(Date()) + "Z</time>\n   " +
                            "</trkpt>\n"
                )
            } else {
                stockList.add(
                    "   <trkpt lat=\"" + points[i].latitude + "\" lon=\"" + points[i].longitude + "\">\n" +
                            "    <acc>" + points[i].accuracy + "</acc>\n" +
                            "    <bear>" + points[i].bearing + "</bear>\n" +
                            "    <speed>" + points[i].speed + "</speed>\n" +
                            "    <time_elapsed>" + points[i].elapsedRealtimeNanos + "</time_elapsed>\n" +
                            "    <provider>" + points[i].provider + "</provider>\n" +
                            "    <distance>" + points[i].distanceTo(points[i - 1]) + "</distance>\n" +
                            "    <ele>" + points[i].altitude + "</ele>\n" +
                            "    <time>" + df.format(Date()) + "Z</time>\n   </trkpt>\n"
                )
            }
        }

        segments += stockList
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