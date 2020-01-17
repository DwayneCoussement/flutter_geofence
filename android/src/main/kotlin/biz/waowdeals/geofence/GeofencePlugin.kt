package biz.waowdeals.geofence

import android.util.Log
import androidx.annotation.NonNull
import com.google.android.gms.location.Geofence
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
    companion object {
        private var geofenceManager: GeofenceManager? = null
        private var channel: MethodChannel? = null
    }

    override fun onDetachedFromActivity() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        activityPluginBinding.activity?.applicationContext.let {
            GeofencePlugin.geofenceManager = GeofenceManager(it, {
                handleGeofenceEvent(it)
            }, {
                channel?.invokeMethod("userLocationUpdated", hashMapOf("lat" to it.latitude, "lng" to it.longitude))
            })
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "geofence")
        channel.setMethodCallHandler(GeofencePlugin())
        GeofencePlugin.channel = channel
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
            } else if (call.method == "getUserLocation") {
                geofenceManager?.getUserLocation()
                result.success(null)
            } else {
                result.notImplemented()
            }

    private fun handleGeofenceEvent(region: GeoRegion) {
        if (region.events.contains(GeoEvent.entry)) {
            channel?.invokeMethod("entry", region)
        } else {
            channel?.invokeMethod("exit", region)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {

    }
}