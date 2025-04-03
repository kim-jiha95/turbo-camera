#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(TurboCameraViewManager, RCTViewManager)
RCT_EXTERN_METHOD(setBackgroundImage:(nonnull NSNumber *)reactTag imageId:(nonnull NSNumber *)imageId)
@end