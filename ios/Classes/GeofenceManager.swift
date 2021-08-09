//
//  GeofenceManager.swift
//  Geolocation
//
//  Created by The Amazing Dwayne Coussement on 19/12/2019.
//  Copyright © 2019 intivoto. All rights reserved.
//

import Foundation
import CoreLocation

enum GeoEvent {
	case entry
	case exit
}

struct GeoRegion {
	let id: String
	let radius: Double
	let latitude: Double
	let longitude: Double
	let events: [GeoEvent]
}

extension GeoRegion {
	func toDictionary() -> [AnyHashable: Any] {
		return ["id": id, "radius": radius, "latitude": latitude, "longitude": longitude]
	}
}

class GeofenceManager: NSObject, CLLocationManagerDelegate {
	private let locationManager = CLLocationManager()
	private let callback: ((GeoRegion) -> Void)
	private let userLocationUpdated: ((CLLocationCoordinate2D) -> Void)
	private let backgroundLocationUpdated: ((CLLocationCoordinate2D) -> Void)
	private var regionsState: [CLRegion: CLRegionState] = [:]
	private lazy var backgroundLocationListener: BackgroundLocationListener = {
		return BackgroundLocationListener(backgroundLocationUpdated: backgroundLocationUpdated)
	}()
	
	init(callback: @escaping (GeoRegion) -> Void, locationUpdate: @escaping (CLLocationCoordinate2D) -> Void, backgroundLocationUpdated: @escaping (CLLocationCoordinate2D) -> Void) {
		self.callback = callback
		self.userLocationUpdated = locationUpdate
		self.backgroundLocationUpdated = backgroundLocationUpdated
		super.init()
		locationManager.delegate = self
	}
	
	func requestPermissions() {
		locationManager.requestAlwaysAuthorization()
	}
	
	func startMonitoring(georegion: GeoRegion) {
		if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
			// Register the region.
			let center = CLLocationCoordinate2D(latitude: georegion.latitude, longitude: georegion.longitude)
			let region = CLCircularRegion(center: center, radius: georegion.radius, identifier: georegion.id)
			region.notifyOnEntry = georegion.events.contains(.entry)
			region.notifyOnExit = georegion.events.contains(.exit)
			locationManager.startMonitoring(for: region)
			locationManager.requestState(for: region)
		}
	}
	
	func stopMonitoring(georegion: GeoRegion) {
		if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
			let center = CLLocationCoordinate2D(latitude: georegion.latitude, longitude: georegion.longitude)
			let region = CLCircularRegion(center: center, radius: georegion.radius, identifier: georegion.id)
			locationManager.stopMonitoring(for: region)
			locationManager.requestState(for: region)
		}
	}
	
	func stopMonitoringAllRegions() {
		locationManager.monitoredRegions.forEach {
			locationManager.stopMonitoring(for: $0)
		}
	}
	
	private func requestStateUpdates() {
		locationManager.monitoredRegions.forEach {
			locationManager.requestState(for: $0)
		}
	}
	
	func locationManager(_ manager: CLLocationManager, didDetermineState state: CLRegionState, for region: CLRegion) {
		defer { regionsState[region] = state }
		if let knownState = regionsState[region], state != .unknown, state != knownState {
			if let region = region as? CLCircularRegion {
				let identifier = region.identifier
				if state == .inside && region.notifyOnEntry {
					let georegion = GeoRegion(id: identifier, radius: region.radius, latitude: region.center.latitude, longitude: region.center.longitude, events: [.entry])
					callback(georegion)
				} else if state == .outside && region.notifyOnExit {
					let georegion = GeoRegion(id: identifier, radius: region.radius, latitude: region.center.latitude, longitude: region.center.longitude, events: [.exit])
					callback(georegion)
				}
			}
		}
	}
	
	func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
		requestStateUpdates()
	}
	
	func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
		requestStateUpdates()
	}
	
	func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
		guard let coordinate = locations.last?.coordinate else { return }
		userLocationUpdated(coordinate)
	}
	
	func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {

	}
	
	func getUserLocation() {
		locationManager.requestLocation()
	}
	
	func startListeningForLocationChanges() {
		if CLLocationManager.locationServicesEnabled() {
			switch CLLocationManager.authorizationStatus() {
			case .notDetermined, .restricted, .denied:
				break // TODO: give feedback
			case .authorizedAlways, .authorizedWhenInUse:
				backgroundLocationListener.startMonitoring()
			@unknown default:
				break
			}
		} else {
			// TODO: give feedback
		}
	}
	
	func stopListeningForLocationChanges() {
		backgroundLocationListener.stopMonitoring()
	}
	
	func triggerLocationUpdate() {
        locationManager.startUpdatingLocation()
	}
}

class BackgroundLocationListener: NSObject, CLLocationManagerDelegate {
	private lazy var locationManager = CLLocationManager()
	private let backgroundLocationUpdated: ((CLLocationCoordinate2D) -> Void)
	
	init(backgroundLocationUpdated: @escaping ((CLLocationCoordinate2D) -> Void)) {
		self.backgroundLocationUpdated = backgroundLocationUpdated
		super.init()
		locationManager.delegate = self
	}
	
	func startMonitoring() {
		locationManager.allowsBackgroundLocationUpdates = true
		locationManager.startMonitoringSignificantLocationChanges()
	}
	
	func stopMonitoring() {
		locationManager.stopMonitoringSignificantLocationChanges()
	}
	
	func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
		guard let coordinate = locations.last?.coordinate else { return }
		backgroundLocationUpdated(coordinate)
	}
	
	func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {

	}
}
