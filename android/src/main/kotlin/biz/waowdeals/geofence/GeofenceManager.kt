package biz.waowdeals.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.*


enum class GeoEvent {
    entry,
    exit
}

data class GeoRegion(
        val id: String,
        val radius: Float,
        val latitude: Double,
        val longitude: Double,
        val events: List<GeoEvent>
)

fun GeoRegion.convertRegionToGeofence(): Geofence {
    val transitionType: Int = if (events.contains(GeoEvent.entry)) {
        GEOFENCE_TRANSITION_ENTER
    } else {
        GEOFENCE_TRANSITION_EXIT
    }

    return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                    latitude,
                    longitude,
                    radius
            )
            .setExpirationDuration(NEVER_EXPIRE)
            .setTransitionTypes(transitionType)
            .build()
}

class GeofenceManager(context: Context, val callback: (GeoRegion) -> Unit, val locationUpdate: (Location) -> Unit) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    fun startMonitoring(geoRegion: GeoRegion) {
        geofencingClient?.addGeofences(getGeofencingRequest(geoRegion.convertRegionToGeofence()), geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
            }
            addOnFailureListener {
                // Failed to add geofences
            }
        }
    }

    private fun getGeofencingRequest(geofence: Geofence): GeofencingRequest {
        val geofenceList = listOf(geofence)
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun refreshLocation() {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                locationUpdate(locationResult.lastLocation)
            }
        }

        fusedLocationClient.requestLocationUpdates(LocationRequest.create(), locationCallback, Looper.getMainLooper())
    }

    fun getUserLocation() {
        fusedLocationClient.apply {
            lastLocation.addOnCompleteListener {
                it.result?.let {
                    if (System.currentTimeMillis() - it.time > 60 * 1000) {
                        refreshLocation()
                    } else {
                        locationUpdate(it)
                    }
                }
            }
        }
    }
}