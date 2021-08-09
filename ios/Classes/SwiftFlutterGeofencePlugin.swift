import Flutter
import UIKit

public class SwiftFlutterGeofencePlugin: NSObject, FlutterPlugin {
	private var geofenceManager: GeofenceManager!
	private var channel: FlutterMethodChannel
	
	public static func register(with registrar: FlutterPluginRegistrar) {
		let channel = FlutterMethodChannel(name: "geofence", binaryMessenger: registrar.messenger())
		let instance = SwiftFlutterGeofencePlugin(channel: channel)
		registrar.addMethodCallDelegate(instance, channel: channel)
	}
	
	public init(channel: FlutterMethodChannel) {
		self.channel = channel
		super.init()
		self.geofenceManager = GeofenceManager(callback: { [weak self] (region) in
			self?.handleGeofenceEvent(region: region)
		}, locationUpdate: { [weak self] (coordinate) in
			self?.channel.invokeMethod("userLocationUpdated", arguments: ["lat": coordinate.latitude, "lng": coordinate.longitude])
		}, backgroundLocationUpdated: { [weak self] (coordinate) in
			self?.channel.invokeMethod("backgroundLocationUpdated", arguments: ["lat": coordinate.latitude, "lng": coordinate.longitude])
		})
	}
	
	public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
		if (call.method == "addRegion") {
			guard let arguments = call.arguments as? [AnyHashable: Any] else { return }
			guard let identifier = arguments["id"] as? String,
				  let latitude = arguments["lat"] as? Double,
				  let longitude = arguments["lng"] as? Double else {
				return
			}
			let radius = arguments["radius"] as? Double
			let event = arguments["event"] as? String
			addRegion(identifier: identifier, latitude: latitude, longitude: longitude, radius: radius, event: event ?? "")
			result(nil)
		} else if (call.method == "removeRegion") {
			guard let arguments = call.arguments as? [AnyHashable: Any] else { return }
			guard let identifier = arguments["id"] as? String,
				  let latitude = arguments["lat"] as? Double,
				  let longitude = arguments["lng"] as? Double else {
				return
			}
			let radius = arguments["radius"] as? Double
			let event = arguments["event"] as? String
			removeRegion(identifier: identifier, latitude: latitude, longitude: longitude, radius: radius, event: event ?? "")
			result(nil)
		} else if (call.method == "removeRegions") {
			geofenceManager.stopMonitoringAllRegions()
			result(nil)
		} else if (call.method == "getUserLocation") {
			geofenceManager.getUserLocation()
			result(nil)
		} else if (call.method == "startListeningForLocationChanges") {
			geofenceManager.startListeningForLocationChanges()
			result(nil)
		} else if (call.method == "stopListeningForLocationChanges") {
			geofenceManager.stopListeningForLocationChanges()
			result(nil)
		} else if (call.method == "requestPermissions") {
			geofenceManager.requestPermissions()
			result(nil)
		}
	}
	
	private func handleGeofenceEvent(region: GeoRegion) {
		if (region.events.contains(.entry)) {
			channel.invokeMethod("entry", arguments: region.toDictionary())
		} else {
			channel.invokeMethod("exit", arguments: region.toDictionary())
		}
	}
	
	private func addRegion(identifier: String, latitude: Double, longitude: Double, radius: Double?, event: String) {
		let events: [GeoEvent]
		switch event {
		case "GeolocationEvent.entry":
			events = [.entry]
		case "GeolocationEvent.exit":
			events = [.exit]
		default:
			events = [.entry, .exit]
		}
		let georegion = GeoRegion(id: identifier, radius: radius ?? 50.0, latitude: latitude, longitude: longitude, events: events)
		geofenceManager.startMonitoring(georegion: georegion)
	}
	
	private func removeRegion(identifier: String, latitude: Double, longitude: Double, radius: Double?, event: String) {
		let events: [GeoEvent]
		switch event {
		case "GeolocationEvent.entry":
			events = [.entry]
		case "GeolocationEvent.exit":
			events = [.exit]
		default:
			events = [.entry, .exit]
		}
		let georegion = GeoRegion(id: identifier, radius: radius ?? 50.0, latitude: latitude, longitude: longitude, events: events)
		geofenceManager.stopMonitoring(georegion: georegion)
	}
}
