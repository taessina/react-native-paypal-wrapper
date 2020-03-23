> This project is [now live on
> Subspace](https://app.dev.subspace.net/gitlab/subspace-open-development/react-native-paypal-wrapper)!
> Subspace is the open-development platform where projects are
> maintained collectively through voting.

# Note: No longer maintained, happy to accept PR or takeover

Or refer to this article https://medium.com/zestgeek/paypal-integration-in-react-native-9d447df4fce1

# react-native-paypal-wrapper

React Native PayPal wrapper for iOS and Android

## Getting started

`$ yarn add react-native-paypal-wrapper`

### Installation

`$ react-native link react-native-paypal-wrapper`

Extra steps for iOS 🙄 [see here](https://github.com/paypal/PayPal-ios-SDK#with-or-without-cocoapods)

## Usage

### Payment
```javascript
import PayPal from 'react-native-paypal-wrapper';

// 3 env available: NO_NETWORK, SANDBOX, PRODUCTION
PayPal.initialize(PayPal.NO_NETWORK, "<your-client-id>");
PayPal.pay({
  price: '40.70',
  currency: 'MYR',
  description: 'Your description goes here',
}).then(confirm => console.log(confirm))
  .catch(error => console.log(error));
```

### FuturePayment

```javascript
import PayPal from 'react-native-paypal-wrapper';

// Required for Future Payments
const options = {
  merchantName : "Merchant name",
  merchantPrivacyPolicyUri: "https://example.com/privacy",
  merchantUserAgreementUri: "https://example.com/useragreement",
}
// 3 env available: NO_NETWORK, SANDBOX, PRODUCTION
PayPal.initializeWithOptions(PayPal.NO_NETWORK, "<your-client-id>", options);

PayPal.obtainConsent().then(authorization => console.log(authorization))
  .catch(error => console.log(error));

// To decrease payment declines, you must specify a metadata ID header (PayPal-Client-Metadata-Id) 
// in the payment call. See docs: 
// https://developer.paypal.com/docs/integration/mobile/make-future-payment/#required-best-practices-for-future-payments

const metadataID = await PayPal.getClientMetadataId();

```
### Disclaimer

This project is created solely to suit our requirements, no maintenance/warranty are provided. Feel free to send in pull requests.

### Acknowledgement

This project is inspired by [MattFoley](https://github.com/MattFoley/react-native-paypal) (which does not support both Android and iOS simultaneously, and [shovelapps](https://github.com/shovelapps/react-native-paypal) a fork of the former repo (which we had some problems trying to integrate due to React Native version).
