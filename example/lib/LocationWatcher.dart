import 'dart:async';

import 'package:flutter/services.dart';

class LocationWatcher {
  final _controller = StreamController<void>();
  static const _stream = const EventChannel('example.significantlocation');
  Stream<void> get stream => _controller.stream;

  LocationWatcher() {
    _stream.receiveBroadcastStream().listen((d) => _onLocation());
  }

  void _onLocation() {
    _controller.sink.add(null);
  }

  void dispose() {
    _controller.close();
  }
}