# Device Integrity Checks

This reference implementation does not include device integrity or root-detection logic.
Implementers are responsible for integrating appropriate checks based on their security requirements and threat model.

## Recommendations

- Use a commercial Runtime Application Self-Protection (RASP) solution for production deployments.
  These provide tamper detection, root/jailbreak detection, hooking framework detection, and debugger protection with ongoing maintenance against bypass techniques.

- **Google Play Integrity API** may be evaluated as an optional, platform-native alternative.
  It provides device attestation via server-side verification but has trade-offs around device coverage, offline availability, and quota limits.
  Consider gating it behind a feature flag for staged rollout.

## Additional Hardening

- **`WindowManager.LayoutParams.FLAG_SECURE`** — Adds `FLAG_SECURE` to all activities that display sensitive identity data. This prevents the system from capturing screenshots, including the image in the recent-apps overview, and blocks screen recording or casting of those screens. Deployments that extend this wallet to show passport images, document numbers, or other PII should set `window.setFlags(FLAG_SECURE, FLAG_SECURE)` in `onCreate()` before calling `super`.

- **Code obfuscation and shrinking** — Enable R8 full mode with aggressive obfuscation for release builds to raise the cost of reverse engineering.
