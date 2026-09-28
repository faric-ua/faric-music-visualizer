# Current Handoff

Project: FARIC Music Visualizer

Verified:
- public GitHub repository exists;
- Termux checkout/aliases/widget PASS on phone;
- baseline Validate workflow PASS;
- Android scaffold CI PASS: run 36446252861;
- verified source for that Android PASS: 6232cebfc7a9ea8a00b3c2934775e4a73a08bc6b.

Current implementation:
- Android package: `com.saney.musicvisualizer`;
- AGP 9.2 / Gradle 9.4.1 / Java 17 baseline;
- Media3 playback;
- local audio picker;
- Android audio-session Visualizer analysis;
- spectrum bands + adaptive bass/onset prototype;
- first Canvas scene: dark orb + radial reactive rays + glow;
- project-specific stable signer pipeline prepared.

Evidence boundary:
- playback/audio-reactive sync/Canvas scene are NOT phone-PASS yet;
- stable signer secrets must be created on the phone and uploaded to GitHub;
- signing material must never enter Git or chat.

Next:
1. update phone checkout;
2. create the dedicated development signer using menu item 5;
3. make encrypted signer backup using item 7;
4. upload the five GitHub secrets using item 6;
5. wait for exact signed Android build PASS;
6. download exact APK with item 8;
7. install and perform first playback/beat/rotation phone QA.


Latest UX adjustment:
- menu item 8 downloads the exact current-commit APK into a versioned folder under Android Download and then opens that folder;
- target folder: `/storage/emulated/0/Download/FARIC-Music-Visualizer-vX.Y.Z-build/`.
