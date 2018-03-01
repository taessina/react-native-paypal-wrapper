
package com.taessina.paypal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import com.facebook.internal.paypal.BundleJSONConverter;

import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalFuturePaymentActivity;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

public class RNPaypalWrapperModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
  private static final int PAYPAL_REQUEST = 65535;
  private static final int REQUEST_CODE_FUTURE_PAYMENT = 2;

  private final ReactApplicationContext reactContext;

  private static final String ERROR_USER_CANCELLED = "USER_CANCELLED";
  private static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";
  private static final String ERROR_INTERNAL_ERROR = "INTERNAL_ERROR";

  private Promise promise;
  private PayPalConfiguration config;
  private String environment;
  private String clientId;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (promise != null) {
        switch(requestCode) {
          case PAYPAL_REQUEST:
            if (resultCode == Activity.RESULT_OK) {
              PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
              if (confirm != null) {
                try {
                  BundleJSONConverter converter = new BundleJSONConverter();
                  Bundle bundle = converter.convertToBundle(confirm.toJSONObject());
                  WritableMap map = Arguments.fromBundle(bundle);
                  promise.resolve(map);
                } catch (Exception e) {
                  promise.reject(ERROR_INTERNAL_ERROR, "Internal error");
                }
              }
            } else if (resultCode == Activity.RESULT_CANCELED) {
              promise.reject(ERROR_USER_CANCELLED, "User cancelled");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
              promise.reject(ERROR_INVALID_CONFIG, "Invalid config");
            }
            break;
          case REQUEST_CODE_FUTURE_PAYMENT:
            if (resultCode == Activity.RESULT_OK) {
              PayPalAuthorization auth = data.getParcelableExtra(PayPalFuturePaymentActivity.EXTRA_RESULT_AUTHORIZATION);
              if (auth != null) {
                try {
                  BundleJSONConverter converter = new BundleJSONConverter();
                  Bundle bundle = converter.convertToBundle(auth.toJSONObject());
                  WritableMap map = Arguments.fromBundle(bundle);
                  promise.resolve(map);
                } catch (Exception e) {
                  promise.reject(ERROR_INTERNAL_ERROR, "Internal error");
                }
              }
            } else if (resultCode == Activity.RESULT_CANCELED) {
              promise.reject(ERROR_USER_CANCELLED, "User cancelled");
            } else if (resultCode == PayPalFuturePaymentActivity.RESULT_EXTRAS_INVALID) {
              promise.reject(ERROR_INVALID_CONFIG, "Invalid config");
            }
            break;
        }
      }
      promise = null;
    }
  };

  public RNPaypalWrapperModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    reactContext.addActivityEventListener(mActivityEventListener);
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "RNPaypalWrapper";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap();
    constants.put("NO_NETWORK", PayPalConfiguration.ENVIRONMENT_NO_NETWORK);
    constants.put("SANDBOX", PayPalConfiguration.ENVIRONMENT_SANDBOX);
    constants.put("PRODUCTION", PayPalConfiguration.ENVIRONMENT_PRODUCTION);
    constants.put(ERROR_USER_CANCELLED, ERROR_USER_CANCELLED);
    constants.put(ERROR_INVALID_CONFIG, ERROR_INVALID_CONFIG);
    return constants;
  }

  @ReactMethod
  public void initialize(String environment, String clientId) {
    this.environment = environment;
    this.clientId = clientId;

    config = new PayPalConfiguration().environment(environment).clientId(clientId);

    Intent intent = new Intent(reactContext, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    reactContext.startService(intent);
  }

  @ReactMethod
  public void initializeWithOptions(String environment, String clientId, ReadableMap params) {
    config = new PayPalConfiguration().environment(environment).clientId(clientId);

    if(params.hasKey("merchantName") && params.hasKey("merchantPrivacyPolicyUri") && params.hasKey("merchantUserAgreementUri")) {
      String merchantName = params.getString("merchantName");
      String merchantPrivacyPolicyUri = params.getString("merchantPrivacyPolicyUri");
      String merchantUserAgreementUri = params.getString("merchantUserAgreementUri");

      config = config.merchantName(merchantName)
        .merchantPrivacyPolicyUri(Uri.parse(merchantPrivacyPolicyUri))
        .merchantUserAgreementUri(Uri.parse(merchantUserAgreementUri));

    }

    Intent intent = new Intent(reactContext, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    reactContext.startService(intent);
  }

  @ReactMethod
  public void pay(ReadableMap params, Promise promise) {
    this.promise = promise;

    String price = params.getString("price");
    String currency = params.getString("currency");
    String description = params.getString("description");

    PayPalPayment payment =
      new PayPalPayment(
        new BigDecimal(price),
        currency,
        description,
        PayPalPayment.PAYMENT_INTENT_SALE
      );

    payment.enablePayPalShippingAddressesRetrieval(true);

    Intent intent = new Intent(reactContext, PaymentActivity.class);

    // send the same configuration for restart resiliency
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
    getCurrentActivity().startActivityForResult(intent, PAYPAL_REQUEST);
  }

  @ReactMethod
  public void obtainConsent(Promise promise) {
    this.promise = promise;
    Intent intentF = new Intent(reactContext, PayPalFuturePaymentActivity.class);
    intentF.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    getCurrentActivity().startActivityForResult(intentF, REQUEST_CODE_FUTURE_PAYMENT);
  }
  
  @ReactMethod
  public void getClientMetadataId(Promise promise) {
    this.promise = promise;
    String metadataId = PayPalConfiguration.getClientMetadataId(reactContext);
    promise.resolve(metadataId);
  }

  @Override
  public void onHostDestroy() {
    reactContext.stopService(new Intent(reactContext, PayPalService.class));
  }

  @Override
  public void onHostResume() {
    // Do nothing
  }

  @Override
  public void onHostPause() {
    // Do nothing
  }
}
