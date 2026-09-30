# PulseDeck HUD block contract

This document records the visual block pack approved by the user for the permanent Android main page.

The source references supplied in chat are six 1536×1152 HUD sheets. The implementation is intentionally code-driven for APK portability; the sheets are the visual contract, while the Android views recreate their geometry and behavior.

## Reference block roles

1. Transport block
   - shuffle
   - previous
   - dominant center play/pause
   - next
   - repeat
   - cyan/orange connected circular HUD language

2. Hero / energy block
   - large circular reactor ring
   - radial equalizer segments
   - cyan/orange energy ribbons
   - particles / star sparks

3. Audio display block
   - fine waveform/equalizer
   - orange-to-cyan progress line
   - circular F reactor

4. Utility controls
   - back
   - top overflow
   - favorite
   - track overflow

5. Quick actions
   - theme/palette
   - board/layers
   - visualizer/equalizer
   - export/share

6. Rail frames
   - five-slot transport housing
   - four-slot action housing

## Android implementation mapping

- `PulseDeckMainSkinView.kt`
  - permanent background
  - cyan/orange energy ribbons and particles
  - large reactive F reactor
  - radial audio bars
  - concentric HUD arcs

- `PulseDeckIconButton.kt`
  - all circular utility, transport and quick-action icons
  - one coherent cyan/orange button family

- `PulseDeckControlRail.kt`
  - five-slot transport panel geometry
  - four-slot quick-action panel geometry
  - socket rings / traces / center emphasis

- `PulseMiniView.kt`
  - fine reactive waveform strip

- `MainActivity.showNowPlaying()`
  - assembles the permanent main page from the blocks
  - metadata, seek/time, transport and quick actions remain real Android controls
  - auto-hide and double-tap visibility behavior remain active

## Main-page layer architecture

The permanent HUD is not Cyber Shark and is not a visualizer preset. It is the player skin.

Future content goes into explicit layers:

1. BACKGROUND_CONTENT
   - video
   - projectM
   - visualizer scene

2. MAIN_SKIN_BACKGROUND
   - permanent PulseDeck HUD background / reactor energy

3. HERO_CONTENT
   - GF
   - creature
   - logo
   - artwork

4. PLAYER_CHROME
   - metadata
   - waveform / progress
   - transport
   - quick actions

5. FOREGROUND_FX
   - lightning
   - particles
   - extra glow / overlays

The user will later be able to decide which existing content sits behind or above the permanent skin.
