package biz.waowdeals.geofence

import android.content.Context
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlin.math.log

/** GeofencePlugin */
public class GeofencePlugin(context: Context) : FlutterPlugin, MethodCallHandler {
    private var geofenceManager: GeofenceManager? = GeofenceManager(context, {
        //
    }, {
        print("sluizet door die location")
    })

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "geofence")
        channel.setMethodCallHandler(GeofencePlugin(context = flutterPluginBinding.applicationContext))
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "geofence")
            channel.setMethodCallHandler(GeofencePlugin(registrar.context()))
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) =
            if (call.method == "addRegion") {
                val arguments = call.arguments as? HashMap<*, *>
                if (arguments != null) {
                    val region = safeLet(arguments["id"] as? String,
                            arguments["radius"] as? Float,
                            arguments["lat"] as? Double,
                            arguments["lng"] as? Double,
                            arguments["event"] as? String)
                    { id, radius, latitude, longitude, event ->
                        GeoRegion(
                                id,
                                radius,
                                latitude,
                                longitude,
                                events = when (event) {
                                    "entry" -> listOf(GeoEvent.entry)
                                    "exit" -> listOf(GeoEvent.exit)
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

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        print("hallo")
    }
}


/*
	public init(channel: FlutterMethodChannel) {
		self.channel = channel
		super.init()
		self.geofenceManager = GeofenceManager(callback: { [weak self] (region) in
				self?.handleGeofenceEvent(region: region)
			}, locationUpdate: { [weak self] (coordinate) in
				self?.channel.invokeMethod("userLocationUpdated", arguments: ["lat": coordinate.latitude, "lng": coordinate.longitude])
		})
	}

	public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
		else if (call.method == "getUserLocation") {
			geofenceManager.getUserLocation()
		}
	}

	private func handleGeofenceEvent(region: GeoRegion) {
		if (region.events.contains(.entry)) {
			channel.invokeMethod("entry", arguments: region.toDictionary())
		} else {
			channel.invokeMethod("exit", arguments: region.toDictionary())
		}
	}
}
 */