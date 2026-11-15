# Android GPT-Live background call port (staging)

This patch is based on the official Android v1.5.0 baseline. It restores the Android/Core
Live-call client and changes its lifetime from a process-lifecycle-scoped call to a
user-started microphone foreground-service-scoped call.

## Android microphone rules

- Ask for RECORD_AUDIO only when a user starts a call.
- Start a foregroundServiceType="microphone" service after the permission is granted,
  while the Activity is still foreground, before beginning WebRTC audio capture.
- Show an ongoing, visible notification with a Hang up action.
- Keep the companion's SSE session alive while the call service owns the microphone.
- If Android refuses or destroys the microphone service, end the call and release media.
- Pressing Home, rotating, opening another screen or locking the screen must not
  implicitly end the call. A deliberate Hang up ends it.
- This is START_NOT_STICKY: after process death the app must not auto-resume the
  previous microphone session without a new user action.

## Verification gate

1. Core tests, App unit tests and Preview APK must pass in GitHub Actions.
2. Integrate and test corresponding server/harness Live endpoints. This staging
   client cannot hold an end-to-end GPT-Live conversation against v1.5.0's server.
3. Install the side-by-side Preview APK on an Android 14+ phone; test:
   - interruption and full-duplex voice with app foreground
   - Home and lock screen for more than 60 seconds
   - notification hang-up while screen locked
   - microphone denied / revoked, server disconnect, network loss
   - speaker/earpiece, Bluetooth and audio-focus loss
4. Confirm the microphone privacy indicator and notification remain visible
   while microphone audio capture is active.

Passing a Kotlin compilation alone does not validate background audio
behavior or replace the Android device tests.
