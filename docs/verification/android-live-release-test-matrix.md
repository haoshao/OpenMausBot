# GPT-Live Android v1.5.0: pre-PR regression and field test matrix

Status: pre-PR, pre-merge. Do not create or merge a PR until the
release gates below pass and the user explicitly authorizes opening one.

## Evidence so far

- Android tester reports real full-duplex voice, interruption, Home and
  screen lock beyond 60 seconds passed (C). Record device/build and logcat.
- Android staging CI was reported successful, APK tested on a real phone.
- Server integration branch passed fake OpenAI Live e2e tests locally.
- Four foreground microphone service Robolectric tests passed locally:
  no-call gate, dedicated channel, notification Hang up, start/stop.
- Test coverage remains incomplete: no PR or merge authorization.

## Manual on-device P0 tests

| ID | Procedure | Pass condition |
| --- | --- | --- |
| P0-01 | Interrupt AI during full-duplex speech | AI yields and accepts the next spoken request |
| P0-02 | Home and lock longer than 60 seconds | Live speech and visible mic notification; user reports PASS |
| P0-03 | Screen locked for 15 minutes with intermittent speech | Audio and microphone notification stay live |
| P0-04 | Wi-Fi to mobile data and back in active call | Recover or show clear dropped/retry state; no silent recording |
| P0-05 | Airplane mode 15 seconds and restore | Clean timeout/drop, audio released, retry succeeds |
| P0-06 | Tap notification Hang up while locked | Mic and WebRTC stop; server call ends; notification clears |
| P0-07 | Incoming cellular/VoIP call | Audio focus and microphone released correctly |
| P0-08 | Revoke microphone permission during active call | Audio stops; understandable error, no background recording |
| P0-09 | Swipe app away; separately force-stop | No hidden recording, force-stop does not auto-resume |
| P0-10 | Unpair phone during call | Existing call ends and revoked phone cannot call again |
| P0-11 | Second device attempts concurrent call | Correct busy response and access isolation |
| P0-12 | Reopen after hang-up and start a fresh call | No duplicate sessions or notifications |

## P1 longevity and interoperability

| ID | Procedure | Pass condition |
| --- | --- | --- |
| P1-01 | Bluetooth headset connect/disconnect mid-call | Route changes; microphone stays usable |
| P1-02 | Wired headset and speaker/earpiece switch | Correct input/output selection |
| P1-03 | Rotate, navigate to another app screen, return | Call persists |
| P1-04 | Battery optimization enabled; screen off 30 minutes | Document actual OEM behavior |
| P1-05 | Rapid start/hang-up/start and late permission callbacks | No stale or duplicate call |
| P1-06 | Many consecutive calls over 60 minutes | No leaked service, WebRTC thread or excessive heat |
| P1-07 | Restart host or drop sideband WebSocket | UI ends safely, microphone stops, retry succeeds |
| P1-08 | Reboot device after a call | No microphone session auto-start |
| P1-09 | Notifications disabled | OS-compliant foreground indication remains |
| P1-10 | Delegate Agent task and voice approval during call | Correct caller attribution and approval handling |

## Automated gates

- Android core tests, Android App unit tests and Preview APK build.
- Robolectric: microphone service notification, Hang up PendingIntent,
  no-call safety, explicit start/stop.
- Host: route allowlist, pairing revocation, sideband failure, spoken queue
  provenance and fake OpenAI Live E2E.
- CI must use JDK 17, SDK 37.0 and the locked Node dependencies.

## Evidence record

For each test: ID, model/Android version, Android app SHA, host SHA, connection
and audio route, duration, exact steps, observed status, logs/screenshots.
Do not share API credentials, pairing tokens or private transcripts.

## Release gate

All P0 tests plus the combined Android and host CI must pass. Investigate
failures before advancing. No PR until the user explicitly approves.
