# v0.6.7 — First FARIC music-video MP4 with audio

## Goal

Produce the first FARIC-generated video file that contains both:
- deterministic H.264 visuals;
- the matching music fragment.

## Audio export

The selected local source track is now:
1. opened with MediaExtractor;
2. decoded with MediaCodec;
3. clipped to the same 3-second proof range;
4. converted to PCM16 when needed;
5. encoded to AAC-LC at 160 kbps;
6. written into a temporary audio MP4.

The export then muxes:
- FARIC H.264 video;
- AAC audio

into one final MP4.

## Sync

Both tracks start at time zero inside the proof file.

The source range begins at the current playback position.

Visual frames still use:
```
OfflineAnalysisResult.signalAt(sourceTimestamp)
```

while audio comes from the same source range.

## Export Lab

The action now reads:
- "Експортувати 3 с MP4 зі звуком"

Success output is published to:
- Movies/FARIC

## Current proof profile

- 3 seconds;
- 15 fps;
- long edge <= 960 px;
- H.264/AVC video;
- AAC-LC audio;
- MP4 container.

This is still a correctness proof, not the final quality profile.

## Compatibility path

AAC transcode accepts decoded:
- PCM 16-bit;
- PCM float.

Audio and video are first encoded separately, then muxed into the final MP4. This keeps the proof pipeline easier to diagnose on-device.

## Next

- phone test MP3 and M4A inputs;
- verify A/V sync;
- verify no color corruption;
- measure export time;
- increase to 30 fps;
- move toward 1080p;
- add arbitrary clip range;
- then full-song export.
