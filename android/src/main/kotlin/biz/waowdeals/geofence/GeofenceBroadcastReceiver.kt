package biz.waowdeals.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "GeoBroadcastReceiver"

        var callback: ((GeoRegion) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DC", "Called onreceive")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "something went wrong")
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val event = if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) GeoEvent.entry else GeoEvent.exit
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            for (geofence: Geofence in triggeringGeofences) {
                val region = GeoRegion(id=geofence.requestId,
                        latitude = geofencingEvent.triggeringLocation.latitude,
                        longitude = geofencingEvent.triggeringLocation.longitude,
                        radius = 50.0.toFloat(),
                        events = listOf(event)
                        )

                callback?.invoke(region)
                Log.i(TAG, region.toString())
            }
        } else {
            // Log the error.
            Log.e(TAG, "Not an event of interest")
        }
    }
}