package com.intivoto.flutter_geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** FlutterGeofencePlugin */
class FlutterGeofencePlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private var geofenceManager: GeofenceManager? = null
    private var currentActivity: Activity? = null


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "geofence")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "addRegion") {
            val arguments = call.arguments as? HashMap<*, *>
            if (arguments != null) {
                val region = safeLet(arguments["id"] as? String,
                        arguments["radius"] as? Double,
                        arguments["lat"] as? Double,
                        arguments["lng"] as? Double,
                        arguments["event"] as? String)
                { id, radius, latitude, longitude, event ->
                    GeoRegion(
                            id,
                            radius.toFloat(),
                            latitude,
                            longitude,
                            events = when (event) {
                                "GeolocationEvent.entry" -> listOf(GeoEvent.entry)
                                "GeolocationEvent.exit" -> listOf(GeoEvent.exit)
                                else -> GeoEvent.values().toList()
                            })
                }
                if (region != null) {
                    geofenceManager?.startMonitoring(region)
                    result.success(null)
                } else {
                    result.error("Invalid arguments", "Has invalid arguments", "Has invalid arguments")
                }
            } else {
                result.error("Invalid arguments", "Has invalid arguments", "Has invalid arguments")
            }
        } else if (call.method == "removeRegion") {
            val arguments = call.arguments as? HashMap<*, *>
            if (arguments != null) {
                val region = safeLet(arguments["id"] as? String,
                        arguments["radius"] as? Double,
                        arguments["lat"] as? Double,
                        arguments["lng"] as? Double,
                        arguments["event"] as? String)
                { id, radius, latitude, longitude, event ->
                    GeoRegion(
                            id,
                            radius.toFloat(),
                            latitude,
                            longitude,
                            events = when (event) {
                                "GeolocationEvent.entry" -> listOf(GeoEvent.entry)
                                "GeolocationEvent.exit" -> listOf(GeoEvent.exit)
                                else -> GeoEvent.values().toList()
                            })
                }
                if (region != null) {
                    geofenceManager?.stopMonitoring(region)
                    result.success(null)
                } else {
                    result.error("Invalid arguments", "Has invalid arguments", "Has invalid arguments")
                }
            } else {
                result.error("Invalid arguments", "Has invalid arguments", "Has invalid arguments")
            }
        } else if (call.method == "removeRegions") {
            geofenceManager?.stopMonitoringAllRegions()
            result.success(null)
        } else if (call.method == "getUserLocation") {
            geofenceManager?.getUserLocation()
            result.success(null)
        } else if (call.method == "requestPermissions") {
            requestPermissions()
        } else if (call.method == "startListeningForLocationChanges") {
            geofenceManager?.startListeningForLocationChanges()
            result.success(null)
        } else if (call.method == "stopListeningForLocationChanges") {
            geofenceManager?.stopListeningForLocationChanges()
            result.success(null)
        } else {
            result.notImplemented()
        }
    }

    private fun requestPermissions() {
        safeLet(currentActivity, currentActivity?.applicationContext) { activity, context ->
            checkPermissions(context, activity)
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermissions(context: Context, activity: Activity) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    999)
        } else {
            // Permission has already been granted
            startGeofencing(context)
        }
    }

    private fun startGeofencing(context: Context) {
        context.let {
            geofenceManager = GeofenceManager(it, {
                handleGeofenceEvent(it)
            }, {
                channel.invokeMethod("userLocationUpdated", hashMapOf("lat" to it.latitude, "lng" to it.longitude))
            }, {
                channel.invokeMethod("backgroundLocationUpdated", hashMapOf("lat" to it.latitude, "lng" to it.longitude))
            })
        }
    }

    private fun handleGeofenceEvent(region: GeoRegion) {
        if (region.events.contains(GeoEvent.entry)) {
            channel.invokeMethod("entry", region.serialized())
        } else {
            channel.invokeMethod("exit", region.serialized())
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
        binding.addRequestPermissionsResultListener(this)

        requestPermissions()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        currentActivity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
        binding.addRequestPermissionsResultListener(this)

        requestPermissions()
    }

    override fun onDetachedFromActivity() {
        currentActivity = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean =
        when (requestCode) {
            999 -> {
                if (grantResults != null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentActivity?.let {
                        startGeofencing(it.applicationContext)
                    }
                    true
                } else {
                    false
                }
            } else -> false
        }
}