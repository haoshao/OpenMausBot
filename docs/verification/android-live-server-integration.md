# Android v1.5.0: GPT-Live server and companion integration

This is an experimental integration branch based on the Android GPT-Live
staging commit 84082b892. The previously green Android CI branch stays intact.

## Implemented in source
- Live call creation through the harness: POST /api/live/session
- Current call status: GET /api/live/call
- Termination: POST /api/live/call/end
- Non-secret live settings: PATCH /api/live/settings
- GPT-Live OpenAI session creation and sideband controller; OpenAI credentials
  remain exclusively on the computer, never sent to the phone.
- Companion pairing allowlist limited to those four operations.
- Device-authenticated relaying and explicit unpair/revocation notice.
- Queued Live user messages retain their call provenance.

## Security and deployment
- Configure the GPT-Live key on the trusted host, not in the Android APK.
- The service refuses unsupported routes and does not permit the phone to set
  OpenAI Live API credentials. The pre-existing narrow TTS voice-key config
  write remains supported and does not accept the Live credential.
- The service is not deployed by building the Android APK; update both the
  computer-side harness/companion and the Android client together.

## Verification status
- The Android-side code compiles and previously passed dedicated GitHub CI.
- Companion route, live controller, and WebRTC session tests run locally.
- A dedicated CI workflow uses the repository's frozen package lock to verify
  TypeScript, lint and regression tests with matching dependency versions.
- No claim of real OpenAI audio or lock-screen verification until tested on a
  paired Android device against a host actually running this branch.
