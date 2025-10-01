# Changelog

## v1.0.0 - 2025.10-1

### Added

- Initial support for Digital Credentials API (DCAPI) for presentation of org-iso-mdoc with protocol W3C 18013-7 Annex C
- Brute force protection for PIN entry
- Prevent insecure PINs (e.g., 1234, 1111)

### Changed

- Updated EUDI Wallet Core dependency to stable 0.19.0

### Removed

- RQES related code / support


## v0.1.0-beta05 - 2025-07-07

### Added

- Settings screen with app version display and option to delete all documents
- Show app version in splash screen
- Added ToS and data privacy sample link opening action

### Changed

- Customizable batch size and credential policy from WalletCoreConfig
- Show all text lines in landing page
- Updated build and release pipelines for demo APK and AAB distribution
- Update version with tag before build
- Bumped EUDI Wallet Core dependency to 0.18.0-SNAPSHOT

### Removed

- All translations (cleanup)
- Transaction Log related code, cleaned up proximity and dashboard feature modules, icons, strings,
  and image resources

### Fixed

- N/A

## v0.1.0-beta04 - 2025-06-26

### Changed

- Fixes same device presentation

### Removed

- Android Studio module templates

## v0.1.0-beta03 - 2025-06-24

### Added

- Added `av://` URI scheme handling to support deep linking

### Changed

- Updated app version name to `0.1.0-beta03`
- Updated Age Verification document identifier to versioned `eu.europa.ec.av.1`
- Updated issuance and presentation flows videos
- Updated EUDI Wallet Core dependency to stable version 0.17.0

## v0.1.0-beta02 - 2025-06-18

### Added

- Added AVSP URI scheme handling to support deep linking
- Added RedirectUri configuration to supported client ID schemes
- Added support for EUDI issuer
- Added automatic cleanup for depleted one-time-use documents
- Added credential count tracking to age verification documents

### Changed

- Updated EUDI Wallet Core dependency to version 0.17.0
- Updated document creation settings to request 30 credentials and use OneTimeUse policy
- Modified document metadata API usage (DocumentMetaData → IssuerMetadata)
- Updated BiometricCrypto implementation

## v0.1.0-beta01 - 2025-04-10

### Added

- Added AV issuer/verifier certificate
- Added AV issuer doc format to offer interactor
- Added IDE file templates for feature modules
- Added new onboarding flow pages:
  - Welcome screen with pager
  - Consent screen
  - PIN setup and confirmation
  - Enrollment options
  - Landing page

### Changed

- Updated app version name to 0.1.0-beta01
- Updated app icons and other UI assets
- Updated QR scan flow configuration to use NO_DOCUMENT for issuance
- Updated issuer URLs and document identifiers for age verification
- Updated CI configuration to use self-hosted GHA runners
- Updated build upload pipeline to use DEMO flavor
- Updated PIN validation flow to redirect to landing page after successful validation
- Updated splash screen design

### Removed

- Removed EUDI wallet certificates
- Removed EUDI certificate resources from configuration
- Removed unrelated sections from README

### Fixed

- Fixed issue allowing typing too many characters in PIN field
