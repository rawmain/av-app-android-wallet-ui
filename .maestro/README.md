# Maestro E2E Tests for AV Wallet

This directory contains Maestro end-to-end tests for the Age Verification Android Wallet application.

## Prerequisites

1. **Install Maestro** (if not already installed):
   ```bash
   brew tap mobile-dev-inc/tap
   brew install mobile-dev-inc/tap/maestro
   ```

2. **Build and Install the App**:
   ```bash
   ./gradlew assembleDemoDebug
   ./gradlew installDemoDebug
   ```

3. **Connected Device or Emulator**:
   - Ensure you have an Android device or emulator running
   - Verify with: `adb devices`

4. **Test Environment**:
   - Verifier service: https://verifier.ageverification.dev/

## Test Files

### 1. Trusted Issuer Scenario `e2e_flow_trusted_issuer.yaml`
Complete end-to-end test with **trusted issuer** combining both flows:
- Full onboarding and issuance (trusted issuer)
- Immediate verification test
- Browser attestation result verification (3/3 checks passed: "Fully trusted")
- Complete user journey from start to finish

**Run:**
```bash
maestro test .maestro/e2e_flow_trusted_issuer.yaml
```

### 2. Untrusted Issuer Scenario `e2e_flow_untrusted_issuer.yaml`
Complete end-to-end test with **untrusted issuer** combining both flows:
- Full onboarding and issuance (untrusted issuer)
- Immediate verification test
- Browser attestation result verification (1/3 checks passed: "Issuer is not trusted")
- Tests failure scenarios for untrusted credentials

**Run:**
```bash
maestro test .maestro/e2e_flow_untrusted_issuer.yaml
```

## Test Configuration

### App ID
All tests target: `com.scytales.av` (demo flavor)

### PIN Used
Tests use PIN: `159753` (hard-coded)

### Timeouts
- Page loads: 10 seconds
- Credential issuance: 30 seconds
- Verification flow: 15 seconds

## Key Implementation Details

### Chrome First-Run Dialogs
Tests handle Chrome's first-run experience:
- "Use without an account" prompt (if Chrome is newly installed)
- "Chrome notifications make things easier" dialog

These are handled conditionally using `runFlow` with `when: visible` checks.

### Issuer Flow
The issuer flow requires:
1. Country selection ("Trusted Issuer" / "Non-Trusted Issuer") + Submit
2. Credential attributes ("Age Over 18" toggle) + Confirm
3. Authorization page (scroll + "Authorize")
4. App returns with success screen

**Trusted vs. Untrusted Issuer Selection:**
- **Trusted:** Default selection, just tap "Submit"
- **Untrusted:** Tap "Non-Trusted Issuer" before tapping "Submit"

### Verification Results
The verification results differ based on issuer trust status:

**Trusted Issuer (3/3 checks passed):**
- "Fully trusted"
- "Issuer is trusted"
- "Issuer has not expired"
- "Age over 18 confirmed"
- All checks show "Verified"

**Untrusted Issuer (1/3 checks passed):**
- Tests verify individual check results
- "Issuer is trusted" → "Not verified"
- "Issuer has not expired" → "Not verified"
- "Age over 18 confirmed" → "Verified"
- Only 1 out of 3 checks passes (the age verification)

### Browser Context Persistence
After authorization, the browser may remain open on the verifier page. Tests handle both cases:
- Browser already on verifier → click "For mobile login click here" directly
- Browser elsewhere → navigate to verifier first

## Troubleshooting

### Test Fails at Browser Steps
- Ensure you have stable internet connection
- Verify issuer/verifier services are accessible
- Check if browser app is installed on the device

### Tap Actions Don't Work
- UI text might have changed - update selectors in YAML
- Try using `index` instead of `text` for element selection
- Use `maestro studio` to inspect the UI hierarchy

### Timing Issues
- Increase `timeout` values in `extendedWaitUntil` commands
- Add more `waitForAnimationToEnd` steps
- Use `sleep` command for fixed delays (e.g., `- sleep: 2000` for 2 seconds)

### Deep-Link Issues
- Ensure the app is set as the default handler for the deep-link schemes
- Check AndroidManifest.xml for proper intent-filter configuration
- Clear app defaults: Settings → Apps → AV Wallet → Open by default → Clear defaults

## Continuous Integration

To run tests in CI/CD:
```bash
# Start emulator
emulator -avd <your-avd-name> -no-window -no-audio &

# Wait for emulator to boot
adb wait-for-device

# Install app
./gradlew installDemoDebug

# Run tests
maestro test .maestro/e2e_flow_trusted_issuer.yaml --format junit --output test-results.xml
```

## Additional Resources

- [Maestro Documentation](https://maestro.mobile.dev/)
- [Maestro GitHub Repository](https://github.com/mobile-dev-inc/maestro)
- [AV Wallet Technical Specification](https://github.com/eu-digital-identity-wallet/av-doc-technical-specification)

## Notes

- Tests assume a clean app state (`clearState: true`)
- Tests use regex patterns for flexible text matching (e.g., `.*[Ss]uccess.*`)
- Browser-based steps require the default browser to be configured
- Tests are written for the demo flavor (`com.scytales.av`)

## Customization

To modify for different environments:

1. **Change App ID**: Update `appId` at the top of each YAML file
2. **Change PIN**: Replace all occurrences of `159753`
3. **Change URLs**: Update verifier URL in verification flows
4. **Adjust Selectors**: Use Maestro Studio to find correct element selectors
