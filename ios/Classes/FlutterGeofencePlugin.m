#import "FlutterGeofencePlugin.h"
#if __has_include(<flutter_geofence/flutter_geofence-Swift.h>)
#import <flutter_geofence/flutter_geofence-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_geofence-Swift.h"
#endif

@implementation FlutterGeofencePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterGeofencePlugin registerWithRegistrar:registrar];
}
@end
