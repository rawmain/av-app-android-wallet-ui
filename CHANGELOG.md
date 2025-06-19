# Changelog

## v0.2.0-beta01 - 2025-06-18

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