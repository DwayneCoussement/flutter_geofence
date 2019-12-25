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
	
	init(callback: @escaping (GeoRegion) -> Void) {
		self.callback = callback
		super.init()
		locationManager.requestAlwaysAuthorization()
		locationManager.delegate = self
	}
	
	
	func startMonitoring(georegion: GeoRegion) {
		if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
			// Register the region.
			let center = CLLocationCoordinate2D(latitude: georegion.latitude, longitude: georegion.longitude)
			let region = CLCircularRegion(center: center, radius: georegion.radius, identifier: georegion.id)
			region.notifyOnEntry = georegion.events.contains(.entry)
			region.notifyOnExit = georegion.events.contains(.exit)
			locationManager.startMonitoring(for: region)
		}
	}
	
	func stopMonitoring(georegion: GeoRegion) {
		if CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) {
			let center = CLLocationCoordinate2D(latitude: georegion.latitude, longitude: georegion.longitude)
			let region = CLCircularRegion(center: center, radius: georegion.radius, identifier: georegion.id)
			locationManager.stopMonitoring(for: region)
		}
	}
		
	func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        print("Region Entry")
		if let region = region as? CLCircularRegion {
			let identifier = region.identifier
			let georegion = GeoRegion(id: identifier, radius: region.radius, latitude: region.center.latitude, longitude: region.center.longitude, events: [.entry])
			callback(georegion)
		}
	}

	func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
	    print("Region Exit")
		if let region = region as? CLCircularRegion {
			let identifier = region.identifier
			let georegion = GeoRegion(id: identifier, radius: region.radius, latitude: region.center.latitude, longitude: region.center.longitude, events: [.exit])
			callback(georegion)
		}
	}
}
