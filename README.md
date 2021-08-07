# flutter_geofence

A Plugin for all your geofence interactions.

## Getting Started

### iOS

We have a minimum requirement of iOS 9.0.

In your `Info.plist` you'll need to add following keys:

- NSLocationWhenInUseUsageDescription
- NSLocationAlwaysAndWhenInUseUsageDescription

### Android

In your `AndroidManifest.xml` you should add the following lines:

```
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

If you want to build for android 10+ you need to add ACCESS_BACKGROUND_LOCATION too.

```
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

You'll need to register for a background service as well.

```
<receiver android:name="com.intivoto.flutter_geofence.GeofenceBroadcastReceiver"
    android:enabled="true" android:exported="true"/>
<service android:name="com.intivoto.flutter_geofence.GeofencePlugin"
    android:permission="android.permission.BIND_JOB_SERVICE" android:exported="true"/>
```

This should be sufficient if your app runs with the new Android embedding (v2, Flutter 1.12), also see: https://flutter.dev/docs/development/packages-and-plugins/plugin-api-migration.

## Usage

There is a small example app that prints to console to give an example of functionality of this plugin. At the current time following commands are supported.

### Initialize

Currently you need to initialize the plugin to let it setup it's system. This _might_ be altered in later versions.

```
Geofence.initialize();
```

### Requesting permission

In first versions of the plugin permissions were automatically handled, but as the user of the plugin might want more control over this (or implement it theirselves), this was moved to a separate call.

```
Geofence.requestPermissions();
```

### getCurrentLocation

This will give you a `Future` which will resolve with the current `Coordinate` of the user. If there is one that's recent enough (< 60 seconds old), it'll return this location.

Example usage:

```
Geofence.getCurrentLocation().then((coordinate) {
    print("Your latitude is ${coordinate.latitude} and longitude ${coordinate.longitude}");
});
```

### addGeolocation

This gives you the possibility to add regions to the native geofence manager. Note that even though by adding them they will get triggered, nothing will happen in your app just yet. You also need to start listening to them. You'll get a callback to tell you that your region has been scheduled though.

```
Geofence.addGeolocation(location, GeolocationEvent.entry).then((onValue) {
    scheduleNotification("Georegion added", "Your geofence has been added!");
}).catchError((error) {
    print("failed with $error");
});
```

### removeGeolocation

This gives you the possibility to remove regions you earlier added. Use it like:

```
Geofence.removeGeolocation(location, GeolocationEvent.entry).then((onValue) {
    scheduleNotification("Georegion removed", "Your geofence has been removed!");
}).catchError((error) {
    print("failed with $error");
});
```

### startListening

Start listening to geoevents as they happen.

NOTE:
Region events may not happen immediately after a region boundary is crossed. To prevent spurious notifications, iOS doesn’t deliver region notifications until certain threshold conditions are met. Specifically, the user’s location must cross the region boundary, move away from the boundary by a minimum distance, and remain at that minimum distance for at least 20 seconds before the notifications are reported.

The specific threshold distances are determined by the hardware and the location technologies that are currently available. For example, if Wi-Fi is disabled, region monitoring is significantly less accurate. However, for testing purposes, you can assume that the minimum distance is approximately 200 meters.

Source: https://developer.apple.com/forums/thread/94091

You can assume similar constraints on Android devices.

```
Geofence.startListening(GeolocationEvent.entry, (entry) {
    scheduleNotification("Entry of a georegion", "Welcome to: ${entry.id}");
});
```

### startListeningForLocationChanges

Starts listening for significant location changes on iOS, requests the location every 15 minutes on low power on Android.

```
Geofence.startListeningForLocationChanges();
```

### stopListeningForLocationChanges

Stops listening for significant location changes on iOS, stops requesting the location every 15 minutes on low power on Android.

```
Geofence.stopListeningForLocationChanges();
```

### backgroundLocationUpdated

This is a stream you can listen to, only triggered by the listening for significant location changes.

```
Geofence.backgroundLocationUpdated.stream.listen((event) {
    scheduleNotification("You moved significantly", "a significant location change just happened.");
});
```
