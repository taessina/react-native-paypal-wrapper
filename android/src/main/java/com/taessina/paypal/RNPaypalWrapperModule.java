
package com.taessina.paypal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import com.paypal.android.sdk.payments.PayPalAuthorization;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.paypal.android.sdk.payments.ShippingAddress;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

public class RNPaypalWrapperModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
  private static final int PAYPAL_REQUEST = 467081;

  private final ReactApplicationContext reactContext;

  private static final String ERROR_USER_CANCELLED = "USER_CANCELLED";
  private static final String ERROR_INVALID_CONFIG = "INVALID_CONFIG";

  private Promise promise;
  private Context context;
  private Activity activity;
  private PayPalConfiguration config;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (requestCode == PAYPAL_REQUEST) {
        if (promise != null) {
          if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
              promise.resolve("test");
            }
          } else if (resultCode == Activity.RESULT_CANCELED) {
            promise.reject(ERROR_USER_CANCELLED, "User cancelled");
          } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            promise.reject(ERROR_INVALID_CONFIG, "Invalid config");
          }
        }
      }
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
    this.activity = getCurrentActivity();
    this.context = this.activity.getBaseContext();

    config = new PayPalConfiguration().environment(environment).clientId(clientId);

    Intent intent = new Intent(this.activity, PayPalService.class);
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    this.activity.startService(intent);
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

    Intent intent = new Intent(this.context, PaymentActivity.class);

    // send the same configuration for restart resiliency
    intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
    intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
    this.activity.startActivityForResult(intent, PAYPAL_REQUEST);
  }

  @Override
  public void onHostDestroy() {
    activity.stopService(new Intent(activity, PayPalService.class));
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
