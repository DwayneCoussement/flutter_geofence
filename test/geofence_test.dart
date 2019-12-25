import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geofence/geofence.dart';

void main() {
  const MethodChannel channel = MethodChannel('geofence');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });
}
