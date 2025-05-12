# Release Guide

This document describes the neccessary steps to publish the Android Age Verification app to the Google Play Store and make it available for production use, follow these steps:

**0. Customization (Optional)**
* Add additional features.
* Update the data privacy declaration and terms and conditions in the app folder.
* Update the branding scheme of the app. 

**1. Prepare Your App for Release**
* Build a release version of your app (APK or AAB format).
* Remove debug code, disable logging, and set debuggable to false.
* Thoroughly test the release version on different devices.
* Ensure all app resources are updated and any backend services are production-ready .
* Also please consider the android studio documentation[^1].

**2. Set Up a Google Play Developer Account**
* Sign up at the Google Play Console.
* Pay the one-time registration fee.
* Agree to the Google Play Developer Distribution Agreement.
* Complete the required account information.

**3. Create a New App in the Play Console**
* Log in to the Google Play Console.
* Click "Create app," select your default language, and enter your app’s name as it should appear in the store.

**4. Complete App Details**
* Fill in the store listing: app description, screenshots, icon, feature graphic, and contact details.
* Specify content rating, privacy policy, and target audience.
* Address privacy and security requirements

**5. Upload Your App**
* Go to "Release management" > "App releases."
* Choose your release type (e.g. open, or production).
* Click "Create release."
* Upload your APK or AAB file.
* Fill in release notes and other required information.

**6. Set Pricing and Distribution**
* Choose whether your app is free or paid.
* Select the countries where your app will be available.

**7. Submit for Review**
* Review all information for accuracy.
* Submit your app for Google’s review process.
* The review can take from a few hours up to several days.

**8. Go Live**
* Once approved, your app will be published and available on the Google Play Store for users to download and use in production.

By following these steps, your Android app will be ready for productive use via the Google Play Store.

[^1]: https://developer.android.com/studio/publish
