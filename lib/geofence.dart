import 'dart:async';

import 'package:flutter/services.dart';
import 'package:geofence/Geolocation.dart';

export 'Geolocation.dart';

typedef void GeofenceCallback(Geolocation foo);

class Geofence {
  static const MethodChannel _channel = const MethodChannel('geofence');

  static GeofenceCallback _entryCallback;
  static GeofenceCallback _exitCallback;

  static Future<void> addGeolocation(Geolocation geolocation) {
    return _channel.invokeMethod("addRegion", {
      "lng": geolocation.longitude,
      "lat": geolocation.latitude,
      "id": geolocation.id,
      "radius": geolocation.radius
    });
  }

  static void initialize() {
    var completer = new Completer<void>();
    _channel.setMethodCallHandler((call) async {
      print("got a call ${call.method}, arguments: ${call.arguments}");
      if (call.method == "entry") {
        Geolocation location = Geolocation(latitude: call.arguments["latitude"] as double, longitude: call.arguments["longitude"] as double, radius: call.arguments["radius"] as double, id: call.arguments["id"] as String);
        _entryCallback(location);
      } else if (call.method == "exit") {
        Geolocation location = Geolocation(latitude: call.arguments["latitude"] as double, longitude: call.arguments["longitude"] as double, radius: call.arguments["radius"] as double, id: call.arguments["id"] as String);
        _exitCallback(location);
      }
      completer.complete();
    });
  }

  static void startListening(GeolocationEvent event, GeofenceCallback entry) {
    switch (event) {
      case GeolocationEvent.entry:
        _entryCallback = entry;
        break;
      case GeolocationEvent.exit:
        _exitCallback = entry;
        break;
    }
  }
}
