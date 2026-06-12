#import <AppKit/AppKit.h>
#import <ApplicationServices/ApplicationServices.h>
#import <dlfcn.h>
#import <stdio.h>
#import <stdlib.h>
#import <string.h>

typedef int CGSConnectionID;
typedef CGSConnectionID (*CGSMainConnectionIDFunction)(void);
typedef CFArrayRef (*CGSCopyManagedDisplaySpacesFunction)(CGSConnectionID);
typedef CGError (*CGSMoveWindowsToManagedSpaceFunction)(
    CGSConnectionID,
    CFArrayRef,
    uint64_t
);
typedef CGError (*CGSManagedDisplaySetCurrentSpaceFunction)(
    CGSConnectionID,
    CFStringRef,
    uint64_t
);

static void *loadSymbol(void *handle, const char *name) {
    void *symbol = dlsym(handle, name);
    if (symbol == NULL) {
        fprintf(stderr, "Missing macOS symbol: %s\n", name);
        exit(2);
    }
    return symbol;
}

static NSDictionary *targetAtIndex(NSArray *displays, NSInteger targetIndex) {
    NSInteger currentIndex = 0;
    for (NSDictionary *display in displays) {
        for (NSDictionary *space in display[@"Spaces"]) {
            if (currentIndex == targetIndex) {
                return @{
                    @"display": display[@"Display Identifier"],
                    @"space": space
                };
            }
            currentIndex++;
        }
    }
    return nil;
}

static CGWindowID focusedWindowId(void) {
    NSRunningApplication *application =
        NSWorkspace.sharedWorkspace.frontmostApplication;
    if (application == nil) {
        return 0;
    }

    CFArrayRef windowListRef = CGWindowListCopyWindowInfo(
        kCGWindowListOptionOnScreenOnly | kCGWindowListExcludeDesktopElements,
        kCGNullWindowID
    );
    NSArray *windows = CFBridgingRelease(windowListRef);
    for (NSDictionary *window in windows) {
        NSNumber *ownerPid = window[(id)kCGWindowOwnerPID];
        NSNumber *layer = window[(id)kCGWindowLayer];
        if (
            ownerPid.intValue == application.processIdentifier &&
            layer.intValue == 0
        ) {
            return [window[(id)kCGWindowNumber] unsignedIntValue];
        }
    }
    return 0;
}

int main(int argc, const char *argv[]) {
    @autoreleasepool {
        if (
            argc != 3 ||
            (strcmp(argv[1], "switch") != 0 && strcmp(argv[1], "move") != 0)
        ) {
            fprintf(
                stderr,
                "Usage: AnneVirtualDesktop <switch|move> <zero-based-index>\n"
            );
            return 2;
        }

        NSInteger desktopIndex = [
            [NSString stringWithUTF8String:argv[2]] integerValue
        ];
        void *skyLight = dlopen(
            "/System/Library/PrivateFrameworks/SkyLight.framework/SkyLight",
            RTLD_LAZY
        );
        if (skyLight == NULL) {
            fprintf(stderr, "Unable to load required macOS frameworks.\n");
            return 2;
        }

        CGSMainConnectionIDFunction mainConnection = loadSymbol(
            skyLight,
            "CGSMainConnectionID"
        );
        CGSCopyManagedDisplaySpacesFunction copySpaces = loadSymbol(
            skyLight,
            "CGSCopyManagedDisplaySpaces"
        );
        CGSMoveWindowsToManagedSpaceFunction moveWindows = loadSymbol(
            skyLight,
            "CGSMoveWindowsToManagedSpace"
        );
        CGSManagedDisplaySetCurrentSpaceFunction switchSpace = loadSymbol(
            skyLight,
            "CGSManagedDisplaySetCurrentSpace"
        );
        CGSConnectionID connection = mainConnection();
        CFArrayRef spacesRef = copySpaces(connection);
        NSArray *displays = CFBridgingRelease(spacesRef);
        NSDictionary *target = targetAtIndex(displays, desktopIndex);
        NSDictionary *space = target[@"space"];
        NSNumber *spaceId = space[@"id64"];
        if (spaceId == nil) {
            fprintf(stderr, "Desktop index is outside the available range.\n");
            return 3;
        }

        if (strcmp(argv[1], "switch") == 0) {
            NSString *display = target[@"display"];
            CGError error = switchSpace(
                connection,
                (__bridge CFStringRef)display,
                spaceId.unsignedLongLongValue
            );
            return error == kCGErrorSuccess ? 0 : 5;
        }

        CGWindowID windowId = focusedWindowId();
        if (windowId == 0) {
            fprintf(stderr, "Unable to find the focused window.\n");
            return 4;
        }

        NSArray *windows = @[@(windowId)];
        CGError error = moveWindows(
            connection,
            (__bridge CFArrayRef)windows,
            spaceId.unsignedLongLongValue
        );
        return error == kCGErrorSuccess ? 0 : 5;
    }
}
