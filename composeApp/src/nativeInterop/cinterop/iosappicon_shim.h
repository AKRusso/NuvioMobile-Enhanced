#import <UIKit/UIKit.h>

static inline int NuvioAppIconSupportsAlternateIcons(void) {
    return UIApplication.sharedApplication.supportsAlternateIcons ? 1 : 0;
}

static inline void NuvioAppIconSetAlternateIconName(const char *iconName) {
    NSString *name = iconName == NULL ? nil : [NSString stringWithUTF8String:iconName];
    [UIApplication.sharedApplication setAlternateIconName:name completionHandler:nil];
}
