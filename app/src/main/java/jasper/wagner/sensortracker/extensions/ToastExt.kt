package jasper.wagner.sensortracker.extensions

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import jasper.wagner.sensortracker.R

fun Toast.show(context: Context?, message:String, gravity:Int, duration:Int, isErrorToast: Boolean = false){
    val inflater: LayoutInflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    /*first parameter is the layout you made
    second parameter is the root view in that xml
     */
    val layout: View
    if (!isErrorToast) {
        layout = inflater.inflate(
            R.layout.custom_toast_layout,
            (context as Activity).findViewById<ViewGroup>(R.id.custom_toast_container)
        )
    }
    else{
        layout = inflater.inflate(
            R.layout.custom_error_toast_layout,
            (context as Activity).findViewById<ViewGroup>(R.id.custom_error_toast_container)
        )
    }
    layout.findViewById<TextView>(R.id.text).text = message
    setGravity(gravity, xOffset, yOffset)
    setDuration(duration)
    view = layout
    show()
}