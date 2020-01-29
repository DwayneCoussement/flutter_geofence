#import "GeofencePlugin.h"
#if __has_include(<geofence/geofence-Swift.h>)
#import <geofence/geofence-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "geofence-Swift.h"
#endif

@implementation GeofencePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftGeofencePlugin registerWithRegistrar:registrar];
}
@end
