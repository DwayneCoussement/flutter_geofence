class Geolocation {
  final double latitude;
  final double longitude;
  final double radius; // in meters
  final String id;

  const Geolocation(
      {required this.latitude,
      required this.longitude,
      required this.radius,
      required this.id});
}

enum GeolocationEvent { entry, exit }
