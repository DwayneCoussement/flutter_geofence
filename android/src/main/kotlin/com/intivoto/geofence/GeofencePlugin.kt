package com.intivoto.geofence

import android.Manifest
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
import io.flutter.plugin.common.PluginRegistry.Registrar

/** GeofencePlugin */
public class GeofencePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    override fun onDetachedFromActivity() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        activityPluginBinding.addRequestPermissionsResultListener { requestCode, permissions, grantResults ->
            if (requestCode == 999 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGeofencing(activityPluginBinding.activity.applicationContext)
                return@addRequestPermissionsResultListener true
            }

            return@addRequestPermissionsResultListener false
        }

        safeLet(activityPluginBinding.activity, activityPluginBinding.activity.applicationContext) { activity, context ->
            checkPermissions(context, activity)
        }

        print("on attached to activity called.")
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "geofence")
        channel.setMethodCallHandler(GeofencePlugin())
        GeofencePlugin.channel = channel
        print("on attached called")
    }

    companion object {
        private var geofenceManager: GeofenceManager? = null
        private var channel: MethodChannel? = null

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "geofence")
            channel.setMethodCallHandler(GeofencePlugin())
            GeofencePlugin.channel = channel

            registrar.addRequestPermissionsResultListener { requestCode, permissions, grantResults ->
                if (requestCode == 999 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.
                    startGeofencing(registrar.activity().applicationContext)
                    return@addRequestPermissionsResultListener true
                }

                return@addRequestPermissionsResultListener false
            }


            safeLet(registrar.activity(),registrar.activeContext().applicationContext) { activity, context ->
                checkPermissions(context, activity)
            }
        }

        fun startGeofencing(context: Context) {
            context.let {
                geofenceManager = GeofenceManager(it, {
                    handleGeofenceEvent(it)
                }, {
                    channel?.invokeMethod("userLocationUpdated", hashMapOf("lat" to it.latitude, "lng" to it.longitude))
                })
            }
        }

        private fun handleGeofenceEvent(region: GeoRegion) {
            if (region.events.contains(GeoEvent.entry)) {
                channel?.invokeMethod("entry", region.serialized())
            } else {
                channel?.invokeMethod("exit", region.serialized())
            }
        }

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
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) =
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
            } else if (call.method == "getUserLocation") {
                geofenceManager?.getUserLocation()
                result.success(null)
            } else {
                result.notImplemented()
            }


    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {

    }
}