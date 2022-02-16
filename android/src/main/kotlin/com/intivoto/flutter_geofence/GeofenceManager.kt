package com.intivoto.flutter_geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.renderscript.RenderScript
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.Geofence.*
import com.google.android.gms.location.LocationRequest.PRIORITY_LOW_POWER


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

fun GeoRegion.serialized(): Map<*, *> {
    return hashMapOf(
        "id" to id,
        "radius" to radius,
        "latitude" to latitude,
        "longitude" to longitude
    )
}

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

class GeofenceManager(context: Context,
                      callback: (GeoRegion) -> Unit,
                      val locationUpdate: (Location) -> Unit, val backgroundUpdate: (Location) -> Unit) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    init {
        GeofenceBroadcastReceiver.callback = callback
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }


    fun startMonitoring(geoRegion: GeoRegion) {
        geofencingClient.addGeofences(getGeofencingRequest(geoRegion.convertRegionToGeofence()), geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences added
                Log.d("DC", "added them")
            }
            addOnFailureListener {
                // Failed to add geofences
                Log.d("DC", "something not ok")
            }
        }
    }

    fun stopMonitoring(geoRegion: GeoRegion) {
        val regionsToRemove = listOf(geoRegion.id)
        geofencingClient.removeGeofences(regionsToRemove)
    }

    fun stopMonitoringAllRegions() {
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
            }
            addOnFailureListener {
                // Failed to remove geofences
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

    private val backgroundLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            backgroundUpdate(locationResult.lastLocation)
        }
    }

    fun startListeningForLocationChanges() {
        val request = LocationRequest().setInterval(900000L).setFastestInterval(900000L).setPriority(PRIORITY_LOW_POWER)
        fusedLocationClient.requestLocationUpdates(request, backgroundLocationCallback, Looper.getMainLooper())
    }

    fun stopListeningForLocationChanges() {
        fusedLocationClient.removeLocationUpdates(backgroundLocationCallback)
    }

}