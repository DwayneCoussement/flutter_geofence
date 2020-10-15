import UIKit
import Flutter

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
	private var locationChannel: FlutterEventChannel?
	private let significantLocationHandler = LocationStreamHandler()

	override func application(
		_ application: UIApplication,
		didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
	) -> Bool {
		GeneratedPluginRegistrant.register(with: self)
		
		if launchOptions?[UIApplication.LaunchOptionsKey.location] != nil {
			self.locationChannel = FlutterEventChannel(name: "example.significantlocation", binaryMessenger: window.rootViewController as! FlutterBinaryMessenger)
			self.locationChannel?.setStreamHandler(significantLocationHandler)
			significantLocationHandler.handleSignificantLocation()
		}
		
		return super.application(application, didFinishLaunchingWithOptions: launchOptions)
	}
	
}

class LocationStreamHandler: NSObject, FlutterStreamHandler {
	var eventSink: FlutterEventSink?
	
	func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
		self.eventSink = events
		return nil
	}
	
	func onCancel(withArguments arguments: Any?) -> FlutterError? {
		self.eventSink = nil
		return nil
	}
	
	@discardableResult
	func handleSignificantLocation() -> Bool {
		guard let eventSink = eventSink else {
			return false
		}
		eventSink(nil)
		return true
	}
}
