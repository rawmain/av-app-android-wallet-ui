# Building the Android Age Verification App.

This guide aims to assist developers build and test the Android Age Verification application.

## Table of contents
* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Building the app](#building-the-app)
* [How to work with self signed certificates](#how-to-work-with-self-signed-certificates)
## Overview
This guide aims to assist developers in building the Android Wallet application.

## Prerequisites

* **Java Development Kit (JDK):** JDK 8 or higher is required to compile Java or Kotlin code for Android
* **Android Studio:** The official IDE for Android development, which includes essential tools like the Android SDK, build tools, and an emulator
* **Android SDK Tools:** These provide libraries, debuggers, and other utilities needed for building Android apps
* **Gradle:** The build automation system used to compile, package, and manage dependencies for your app

### Cloning the Repository.

Clone the repository normally:

```bash
git clone https://github.com/eu-digital-identity-wallet/av-app-android-wallet-ui.git
cd av-app-android-wallet-ui
```

## Building the app

Open the project in Android Studio.

The application has two product flavors:
- "Dev", which communicates with the services deployed in an environment based on the latest main branch.
- "Demo", which communicates with the services deployed in an environment based on the latest main branch.

and two Build Types:
- "Debug", which has full logging enabled.
- "Release", which has no logging enabled.

which, ultimately, result in the following Build Variants:

- "devDebug", "devRelease", "demoDebug", "demoRelease".

To change the Build Variant, go to Build -> Select Build Variant and from the tool window you can click on the "Active Build Variant" of the module ":app" and select the one you prefer.
It will automatically apply it to the other modules as well.

To run the App on a device, firstly you must connect your device with the Android Studio, and then go to Run -> Run 'app'. To run the App on an emulator, simply go to Run -> Run 'app'.

### Running with remote services
If you wish to test the application with the Issuer and Verifier services provided by the Toolbox, you can utilize the online services that are publicly available. The configuration below is already predefined within the app for this purpose.

These are the contents of the ConfigWalletCoreImpl file (dev flavor), and you don't need to change anything:
```Kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://issuer.dev.ageverification.dev/")
      .withClientId(clientId = "wallet-dev")
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
          .withParUsage(OpenId4VciManager.Config.ParUsage.NEVER)
          .withUseDPoPIfSupported(false)
      .build()
)
```

### Running with local services
If you prefer not to use the online services and instead wish to run or operate them locally, you will need to install three software components. In the following, we will focus on the scenario where the services are started locally. 

For detailed instructions on how to set up each of these components, please refer to the documentation provided for each respective component.
* [Issuer](https://github.com/eu-digital-identity-wallet/av-srv-web-issuing-avw-py)
* [Web Verifier UI](https://github.com/eu-digital-identity-wallet/av-verifier-ui)
* [Web Verifier Endpoint](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt)

After this, and assuming you are now running everything locally,
you need to change the contents of the ConfigWalletCoreImpl file, from:
```Kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://issuer.dev.ageverification.dev/")
      .withClientId(clientId = "wallet-dev")
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
          .withParUsage(OpenId4VciManager.Config.ParUsage.NEVER)
          .withUseDPoPIfSupported(false)
      .build()
)
```
with this:
```Kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "local_IP_address_of_issuer")
      .withClientId(clientId = "wallet-dev")
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
          .withParUsage(OpenId4VciManager.Config.ParUsage.NEVER)
          .withUseDPoPIfSupported(false)
      .build()
)
```

for example:
```Kotlin
override val vciConfig: List<OpenId4VciManager.Config>
    get() = listOf(
       OpenId4VciManager.Config.Builder()
      .withIssuerUrl(issuerUrl = "https://10.0.2.2")
      .withClientId(clientId = "wallet-dev")
      .withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
          .withParUsage(OpenId4VciManager.Config.ParUsage.NEVER)
          .withUseDPoPIfSupported(false)
      .build()
)
```
## Why 10.0.2.2?

When using the Android emulator, 10.0.2.2 is a special alias that routes to localhost on your development machine.
So if you’re running the issuer locally on your host, the emulator can access it via https://10.0.2.2.

## How to work with self-signed certificates

This section describes configuring the application to interact with services utilizing self-signed certificates.

1. Open the build.gradle.kts file of the "core-logic" module.
2. In the 'dependencies' block, add the following two:
    ```Gradle
    implementation(libs.ktor.android)
    implementation(libs.ktor.logging)
    ```
3. Now, you need to create a new kotlin file *ProvideKtorHttpClient* and place it into the *src\main\java\eu\europa\ec\corelogic\config* package.
4. Copy and paste the following into your newly created *ProvideKtorHttpClient* kotlin file.
    ```Kotlin
    import android.annotation.SuppressLint
    import io.ktor.client.HttpClient
    import io.ktor.client.engine.android.Android
    import io.ktor.client.plugins.logging.Logging
    import java.security.SecureRandom
    import javax.net.ssl.HostnameVerifier
    import javax.net.ssl.SSLContext
    import javax.net.ssl.TrustManager
    import javax.net.ssl.X509TrustManager
    import javax.security.cert.CertificateException
    
    object ProvideKtorHttpClient {

        @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
        fun client(): HttpClient {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                        return arrayOf()
                    }
                }
            )

            return HttpClient(Android) {
                install(Logging)
                engine {
                    requestConfig
                    sslManager = { httpsURLConnection ->
                        httpsURLConnection.sslSocketFactory = SSLContext.getInstance("TLS").apply {
                            init(null, trustAllCerts, SecureRandom())
                        }.socketFactory
                        httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                    }
                }
            }
        }

    }
    ```
5. Also, add this custom HttpClient to the EudiWallet provider function *provideEudiWallet* located in *LogicCoreModule.kt*
    ```Kotlin
    @Single
    fun provideEudiWallet(
    context: Context,
    walletCoreConfig: WalletCoreConfig,
    walletCoreLogController: WalletCoreLogController
    ): EudiWallet = EudiWallet(context, walletCoreConfig.config) {
        withLogger(walletCoreLogController)
        // Custom HttpClient
        withKtorHttpClientFactory {
            ProvideKtorHttpClient.client()
        }
    }
    ```
6. Finally, you need to use the preregistered clientId scheme instead of X509.
   
   Change this:
   ```Kotlin
   withClientIdSchemes(
    listOf(ClientIdScheme.X509SanDns)
   )
    ```
   
   into something like this:
   ```Kotlin
   withClientIdSchemes(
    listOf(
        ClientIdScheme.Preregistered(
            preregisteredVerifiers =
                listOf(
                    PreregisteredVerifier(
                        clientId = "Verifier",
                        legalName = "Verifier",
                        verifierApi = "https://10.0.2.2"
                    )
                )
            )
        )
   )
   ```

   For all configuration options, please refer to [this document](configuration.md)
