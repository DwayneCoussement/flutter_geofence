class Geolocation {
  final double latitude;
  final double longitude;
  final double radius; // in meters
  final String id;

  const Geolocation({this.latitude, this.longitude, this.radius, this.id});
}

enum GeolocationEvent { entry, exit }
