
#import "RNPaypalWrapper.h"
#import "RCTConvert.h"
#import "PayPalMobile.h"

@interface RNPaypalWrapper () <PayPalPaymentDelegate, RCTBridgeModule>

@property PayPalPayment *payment;
@property PayPalConfiguration *configuration;
@property (copy) RCTPromiseResolveBlock resolve;
@property (copy) RCTPromiseRejectBlock reject;

@end

@implementation RNPaypalWrapper

NSString * const USER_CANCELLED = @"USER_CANCELLED";

- (NSDictionary *)constantsToExport
{
    return @{
             @"PRODUCTION": PayPalEnvironmentProduction,
             @"SANDBOX": PayPalEnvironmentSandbox,
             @"NO_NETWORK": PayPalEnvironmentNoNetwork,
             USER_CANCELLED: USER_CANCELLED
             };
}

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(initialize:(NSString *) environment clientId:(NSString *)clientId)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [PayPalMobile initializeWithClientIdsForEnvironments:@{environment : clientId}];
        [PayPalMobile preconnectWithEnvironment:environment];
    });
}

RCT_EXPORT_METHOD(initializeWithOptions:(NSString *) environment clientId:(NSString *)clientId options:(NSDictionary *)options)
{
    dispatch_async(dispatch_get_main_queue(), ^{
        [PayPalMobile initializeWithClientIdsForEnvironments:@{environment : clientId}];
        [PayPalMobile preconnectWithEnvironment:environment];
    });

    if([options objectForKey:@"merchantName"] != nil 
        && [options objectForKey:@"merchantPrivacyPolicyUri"] != nil 
        && [options objectForKey:@"merchantUserAgreementUri"] != nil ) {

        NSString *merchantName = [RCTConvert NSString:options[@"merchantName"]];
        NSString *merchantPrivacyPolicyUri = [RCTConvert NSString:options[@"merchantPrivacyPolicyUri"]];
        NSString *merchantUserAgreementUri = [RCTConvert NSString:options[@"merchantUserAgreementUri"]];

        self.configuration = [[PayPalConfiguration alloc] init];
        self.configuration.merchantName = merchantName;
        self.configuration.merchantPrivacyPolicyURL = [NSURL URLWithString:merchantPrivacyPolicyUri];
        self.configuration.merchantUserAgreementURL = [NSURL URLWithString:merchantUserAgreementUri];
    }
}

RCT_EXPORT_METHOD(getClientMetadataId:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    self.resolve = resolve;
    self.reject = reject;

    NSString *metadataID = [PayPalMobile clientMetadataID];
    self.resolve(metadataID);
}

RCT_EXPORT_METHOD(obtainConsent:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    self.resolve = resolve;
    self.reject = reject;

    PayPalFuturePaymentViewController *vc = [[PayPalFuturePaymentViewController alloc] initWithConfiguration:self.configuration delegate:self];

    // Present the PayPalFuturePaymentViewController
    UIViewController *visibleVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    do {
        if ([visibleVC isKindOfClass:[UINavigationController class]]) {
            visibleVC = [(UINavigationController *)visibleVC visibleViewController];
        } else if (visibleVC.presentedViewController) {
            visibleVC = visibleVC.presentedViewController;
        }
    } while (visibleVC.presentedViewController);
    dispatch_async(dispatch_get_main_queue(), ^{
        [visibleVC presentViewController:vc animated:YES completion:nil];
    });
}

RCT_EXPORT_METHOD(pay:(NSDictionary *)options resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    self.resolve = resolve;
    self.reject = reject;
    
    NSString *price = [RCTConvert NSString:options[@"price"]];
    NSString *currency = [RCTConvert NSString:options[@"currency"]];
    NSString *description = [RCTConvert NSString:options[@"description"]];
    
    self.payment = [[PayPalPayment alloc] init];
    [self.payment setAmount:[[NSDecimalNumber alloc] initWithString:price]];
    [self.payment setCurrencyCode:currency];
    [self.payment setShortDescription:description];
    
    self.configuration = [[PayPalConfiguration alloc] init];
    [self.configuration setAcceptCreditCards:true];
    [self.configuration setPayPalShippingAddressOption:PayPalShippingAddressOptionPayPal];
    
    PayPalPaymentViewController *vc = [[PayPalPaymentViewController alloc] initWithPayment:self.payment
                                                                             configuration:self.configuration
                                                                                  delegate:self];
    
    UIViewController *visibleVC = [[[UIApplication sharedApplication] keyWindow] rootViewController];
    do {
        if ([visibleVC isKindOfClass:[UINavigationController class]]) {
            visibleVC = [(UINavigationController *)visibleVC visibleViewController];
        } else if (visibleVC.presentedViewController) {
            visibleVC = visibleVC.presentedViewController;
        }
    } while (visibleVC.presentedViewController);
    dispatch_async(dispatch_get_main_queue(), ^{
        [visibleVC presentViewController:vc animated:YES completion:nil];
    });

}


#pragma mark Paypal Delegate

- (void)payPalPaymentDidCancel:(PayPalPaymentViewController *)paymentViewController
{
    [paymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
        if (self.reject) {
            NSError *error = [NSError errorWithDomain:RCTErrorDomain code:1 userInfo:NULL];
            self.reject(USER_CANCELLED, USER_CANCELLED, error);
        }
    }];
}

- (void)payPalPaymentViewController:(PayPalPaymentViewController *)paymentViewController
                 didCompletePayment:(PayPalPayment *)completedPayment
{
    [paymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
        if (self.resolve) {
            self.resolve(completedPayment.confirmation);
        }
    }];
}

#pragma mark - PayPalFuturePaymentDelegate methods

- (void)payPalFuturePaymentDidCancel:(PayPalFuturePaymentViewController *)futurePaymentViewController {
  // User cancelled login. Dismiss the PayPalLoginViewController, breathe deeply.
    [futurePaymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
        if (self.reject) {
            NSError *error = [NSError errorWithDomain:RCTErrorDomain code:1 userInfo:NULL];
            self.reject(USER_CANCELLED, USER_CANCELLED, error);
        }
    }];
}

- (void)payPalFuturePaymentViewController:(PayPalFuturePaymentViewController *)futurePaymentViewController
                didAuthorizeFuturePayment:(NSDictionary *)futurePaymentAuthorization {
    // Be sure to dismiss the PayPalLoginViewController.
    [futurePaymentViewController.presentingViewController dismissViewControllerAnimated:YES completion:^{
        if (self.resolve) {
            self.resolve(futurePaymentAuthorization);
        }
    }];
}

@end
