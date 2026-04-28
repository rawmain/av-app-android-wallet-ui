# Device Integrity Checks

This reference implementation does not include device integrity or root-detection logic.
Implementers are responsible for integrating appropriate checks based on their security requirements and threat model.

## Recommendations

- Use a commercial Runtime Application Self-Protection (RASP) solution for production deployments.
  These provide tamper detection, root/jailbreak detection, hooking framework detection, and debugger protection with ongoing maintenance against bypass techniques.

- **Google Play Integrity API** may be evaluated as an optional, platform-native alternative.
  It provides device attestation via server-side verification but has trade-offs around device coverage, offline availability, and quota limits.
  Consider gating it behind a feature flag for staged rollout.
